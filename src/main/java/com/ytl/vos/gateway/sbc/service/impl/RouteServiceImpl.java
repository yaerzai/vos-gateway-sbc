package com.ytl.vos.gateway.sbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.service.SnowFlakeService;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.redis.monitor.concurrent.ConcurrentEnum;
import com.ytl.common.redis.monitor.concurrent.ConcurrentService;
import com.ytl.common.redis.service.StringRedisCacheService;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailResDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.gateway.sbc.constant.SysConstant;
import com.ytl.vos.gateway.sbc.dto.RouteReqDTO;
import com.ytl.vos.gateway.sbc.dto.RouteRespDTO;
import com.ytl.vos.gateway.sbc.enums.NumberPoolTypeEnum;
import com.ytl.vos.gateway.sbc.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.sbc.runner.CallRequestQueue;
import com.ytl.vos.gateway.sbc.service.*;
import com.ytl.vos.gateway.sbc.service.bo.OperatorsInfoBO;
import com.ytl.vos.persistence.dataservice.*;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;
import com.ytl.vos.persistence.dataservice.bo.ChannelNumberInfoDataBO;
import com.ytl.vos.persistence.enums.ChannelNumAllocationEnum;
import com.ytl.vos.persistence.enums.LineTypeEnum;
import com.ytl.vos.persistence.enums.StatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 路由服务
 */
@Service
@Slf4j
public class RouteServiceImpl implements RouteService {
    @Resource
    private DataService dataService;
    @Resource
    private UserService userService;
    @Resource
    private NumberService numberService;
    @Resource
    private ChannelService channelService;
    @Resource
    private CallRequestQueue callRequestQueue;
    @Resource
    private StringRedisCacheService stringRedisCacheService;
    @Resource
    private SnowFlakeService snowFlakeService;
    @Resource
    private NumberCacheService numberCacheService;
    @Resource
    private PriNumberCacheService priNumberCacheService;
    @Resource
    private CustPriNumPoolCacheService custPriNumPoolCacheService;
    @Resource
    private ChnPubNumPoolCacheService chnPubNumPoolCacheService;
    @Resource
    private ProvincePubNumPoolCacheService provincePubNumPoolCacheService;
    @Resource
    private CityPubNumPoolCacheService cityPubNumPoolCacheService;
    @Resource
    private MonitorService monitorService;
    @Resource
    private ConcurrentService concurrentService;

    @Override
    public RouteRespDTO route(RouteReqDTO reqDTO) {
        String callIpStr = dataService.getSysParam(SysParamEnum.VOS_CALL_ALLOW_IP_LIST);
        List<String> ipList = Arrays.asList(callIpStr.split(","));
        if (StringUtils.isNotBlank(callIpStr) && ipList.contains(reqDTO.getFromip())) {
            // 呼入处理
            return getRouteRespDTO(reqDTO);
        }
        long startTimeA = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        MDC.put("type", "route");
        log.info("reqDTO: {}", JSONUtil.toJsonStr(reqDTO));

        //获取信息
        String userNo = reqDTO.getCallerid().substring(0, 6);
        String callerNumber;
        String calledNumber = reqDTO.getCalleeid();
        log.info("客户账号: {}", userNo);

        monitorService.incrCustReq(userNo);

        CallRequestDataBO callReqBO = null;
        ChannelInfoDetailResDTO channelInfo = null;
        boolean isCustRecord;
        try {
            callReqBO = buildCallRequest(reqDTO);
            callerNumber = callReqBO.getCallingNumber();

            //省份和城市
            provinceCity(callReqBO);
            log.info("[SBC网关] 省份和城市 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();
            //客户检查
            CustomerUserInfoQueryRespDTO userInfo = userService.checkUser(callReqBO, reqDTO.getFromip());
            log.info("[SBC网关] 客户检查 耗时：{} ms", System.currentTimeMillis() - startTime);
            //去主叫前缀
            if (StrUtil.isNotEmpty(userInfo.getCmccExtendCode()) && callerNumber.startsWith(userInfo.getCmccExtendCode())) {
                callerNumber = callerNumber.substring(userInfo.getCmccExtendCode().length());
                callReqBO.setCallingNumber(callerNumber);
                log.info("[SBC网关] 账号去主叫前缀, 主叫: {}, 前缀: {}", callerNumber, userInfo.getCmccExtendCode());
            }
            //号码黑名单检查
            numberService.checkNumber(userInfo, callerNumber, calledNumber);
            log.info("[SBC网关] 黑名单检查 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();

            //检查主叫号码是否为客户私有号码
            ChannelNumberInfoDataBO numberInfo = numberService.checkCustPrivateNumber(userInfo, callReqBO);
            log.info("[SBC网关] 检查主叫号码是否为客户私有号码 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();

            if (numberInfo != null) {
                //私有号码
                channelInfo = dataService.getChannelInfo(callReqBO.getChannelNo());
                checkChannel(calledNumber, channelInfo, userInfo);
                if (LineTypeEnum.LINE_PURE.eq(channelInfo.getSendType())) {
                    throw new BusinessException(VosErrCodeEnum.Channel_ConfigErr);
                }
                // A路号码
                callerNumber = callReqBO.getNumberA();
            } else {
                // 非主叫独享查找号码
                numberInfo = findNumber(userInfo, callReqBO);
                channelInfo = dataService.getChannelInfo(callReqBO.getChannelNo());
                if (LineTypeEnum.LINE_PURE.eq(channelInfo.getSendType())) {
                    callerNumber = callReqBO.getCallingNumber();
                } else {
                    callerNumber = callReqBO.getNumberA();
                }
            }
            //发送类型
            if (numberInfo != null) {
                callReqBO.setSendType(numberInfo.getNumberType());
            }

            //添加主叫前缀
            if (StrUtil.isNotEmpty(channelInfo.getCallerPreLimit())) {
                callerNumber = channelInfo.getCallerPreLimit() + callerNumber;
            }
            String oldCalledNumber = calledNumber;
            //添加被叫前缀
            if (calledNumber.length() == 11 && calledNumber.startsWith("1")) {
                calledNumber = "0" + calledNumber;
            }
            if (StrUtil.isNotEmpty(channelInfo.getCalleePreLimit())) {
                calledNumber = channelInfo.getCalleePreLimit() + calledNumber;
            }
            log.info("[SBC网关] 查找号码 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();

            //用户限额增加
            userService.userLimitIncrement(userInfo, oldCalledNumber);
            //通道限额增加
            channelService.channelLimitIncrement(channelInfo, oldCalledNumber);
            log.info("[SBC网关] 用户通道限额增加 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();

            isCustRecord = userService.isRecord(userInfo);
            callReqBO.setNeedRecord((byte) (isCustRecord ? 1 : 0));
            log.info("是否需要录音: {}", isCustRecord ? "是" : "否");

            //非语音线路  号码并发数+1
            if (numberInfo != null && !LineTypeEnum.LINE_PURE.eq(channelInfo.getSendType())) {
                if (ChannelNumAllocationEnum.COMMON.getCodeId() == numberInfo.getAllocationStatus()) {
                    numberCacheService.incrNumberConcurrency(numberInfo.getNumberNo());
                } else if (ChannelNumAllocationEnum.PRIVATE.getCodeId() == numberInfo.getAllocationStatus()) {
                    priNumberCacheService.incrNumberConcurrency(numberInfo.getNumberNo(), userNo);
                }

                //小号使用频率增加
                String channelNo = channelInfo.getChannelNo();
                numberService.numberFrequencyLimitAdd(channelNo, numberInfo.getNumberNo());

                String lineIp = numberInfo.getSipIp();
                Integer cps = channelInfo.getConnectNum();
                Integer delayTime = channelInfo.getReceiveWindow();
                if (StrUtil.isNotEmpty(lineIp) && cps != null && delayTime != null && cps > 0 && delayTime > 0) {
                    String currTime = DateUtil.date().toString("HHmmss");
                    long sipIpFlow = stringRedisCacheService.incrementAndGet(StrUtil.format("ChannelLineCPS:{}:{}:{}", channelNo, lineIp, currTime), Integer.MAX_VALUE, 1, 3);
                    if (sipIpFlow > cps) {
                        log.warn("[SBC网关] 通道{}, 线路{}, 超最大{}cps, 延迟: {} ms", channelNo, lineIp, cps, delayTime);
                        ThreadUtil.sleep(delayTime);
                    }
                }

            }
            log.info("[SBC网关] 录音 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();
            //缓存chnnalNo
            CallRequestDataBO finalCallReqBO = callReqBO;
            SysConstant.processExecutors.execute(() -> addChannelRedis(finalCallReqBO, isCustRecord));
            log.info("[SBC网关] 缓存chnnalNo 耗时：{} ms", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();
            //并发监控
            concurrentService.incr(ConcurrentEnum.USER_CONCURRENT, userNo);
            int channelConc = concurrentService.incr(ConcurrentEnum.CHANNEL_CONCURRENT, channelInfo.getChannelNo());
            concurrentService.incr(ConcurrentEnum.PLAT_CONCURRENT);
            if (isCustRecord) {
                concurrentService.incrRtp(ConcurrentEnum.USER_CONCURRENT, userNo);
                concurrentService.incrRtp(ConcurrentEnum.CHANNEL_CONCURRENT, channelInfo.getChannelNo());
                concurrentService.incrRtp(ConcurrentEnum.PLAT_CONCURRENT);
            } else {
                concurrentService.incrKam(ConcurrentEnum.USER_CONCURRENT, userNo);
                concurrentService.incrKam(ConcurrentEnum.CHANNEL_CONCURRENT, channelInfo.getChannelNo());
                concurrentService.incrKam(ConcurrentEnum.PLAT_CONCURRENT);
            }
            if (channelInfo.getLimitNum() != null && channelConc > channelInfo.getLimitNum()) {
                String channelNo = channelInfo.getChannelNo();
                String delayChannelNoStr = dataService.getSysParam(SysParamEnum.VOS_NUMBER_CONCURRENT_DELAY_CHANNEL_NO);
                List<String> delayChannelNoList = Arrays.asList(delayChannelNoStr.split(","));
                String delayFlag = dataService.getSysParam(SysParamEnum.VOS_CHANNEL_CONCURRENT_DELAY_FLAG);
                if ("1".equals(delayFlag) && delayChannelNoList.contains(channelNo)) {
                    int delayTime = dataService.getSysParamInt(SysParamEnum.VOS_CHANNEL_CONCURRENT_DELAY_TIME);
                    if (delayTime > 0) {
                        log.warn("[SBC网关] 通道并发数超限,延迟: {} ms", delayTime);
                        ThreadUtil.sleep(delayTime);
                    }
                }
            }

            //设置路由状态
            callReqBO.setRouteStatus((byte) 1);

            monitorService.incrCustRoute(userNo);
        } catch (BusinessException e) {
            //异常流水
            addErrInfo(callReqBO, e);
            throw e;
        } catch (Exception e) {
            //异常流水
            BusinessException error = new BusinessException(VosErrCodeEnum.System_Error, e.getMessage(), e);
            addErrInfo(callReqBO, error);
            throw error;
        } finally {
            startTime = System.currentTimeMillis();
            addCallRequest(callReqBO);
            log.info("[SBC网关] addCallRequest 耗时：{} ms", System.currentTimeMillis() - startTime);
            long endTime = System.currentTimeMillis();
            if (endTime - startTimeA > 1000 && endTime - startTimeA < 2000) {
                log.warn("[SBC网关] 路由接口 耗时：{} ms", endTime - startTimeA);
            } else if (endTime - startTimeA >= 2000) {
                log.error("[SBC网关] 路由接口 耗时：{} ms", endTime - startTimeA);
            }
        }

        RouteRespDTO respDTO = new RouteRespDTO();
        respDTO.setResult(true);

        //判断客户是否需要保留录音

        respDTO.setRecord(isCustRecord ? 1 : 0);

        //路由ip和端口
        String routeHost = channelInfo.getGetawayIp() + ":" + channelInfo.getGetawayPort();

        respDTO.setRouteHost(channelInfo.getGetawayIp());
        respDTO.setRoutePort(String.valueOf(channelInfo.getGetawayPort()));
        //主叫号码
        respDTO.setRouteRid(callerNumber);
        //被叫号码
        respDTO.setRouteCid(calledNumber);
        respDTO.setReason("请求成功");
        return respDTO;
    }

    private RouteRespDTO getRouteRespDTO(RouteReqDTO reqDTO) {
        String routeIP = dataService.getSysParam(SysParamEnum.VOS_CALL_ALLOW_FORWARD_IP);
        String routePort = dataService.getSysParam(SysParamEnum.VOS_CALL_ALLOW_FORWARD_PORT);
        RouteRespDTO respDTO = new RouteRespDTO();
        respDTO.setResult(true);

        //判断客户是否需要保留录音
        respDTO.setRecord(0);
        respDTO.setRouteHost(routeIP);
        respDTO.setRoutePort(routePort);
        //主叫号码
        respDTO.setRouteRid(reqDTO.getCallerid().replace("+86", ""));
        //TODO 根据被叫号码查询归属客户账
        respDTO.setRouteCid(reqDTO.getCalleeid().replaceFirst("86755", "0755"));
        respDTO.setReason("请求成功");
        log.warn("呼入转发: {}  resp: {}", JSONUtil.toJsonStr(reqDTO), JSONUtil.toJsonStr(respDTO));
        return respDTO;
    }

    private ChannelNumberInfoDataBO findNumber(CustomerUserInfoQueryRespDTO userInfo, CallRequestDataBO callReqBO) {
        String calledNumber = callReqBO.getCalledNumber();
        String userNo = callReqBO.getUserNo();
        List<ChannelNumberInfoDataBO> numberList = null;
        String channelGroupNo = userInfo.getChannelGroupNo();
        String sendType = userInfo.getSendType();
        Byte numberFitlerType = userInfo.getLongSmsFlag();
        // 是否支持私有号码池
        if (NumberPoolTypeEnum.Share.getCodeId() == userInfo.getEnforcePreSign()) {
            log.info("号码池共享模式, 账号: {}, 通道组: {}", userNo, channelGroupNo);
            //账号配置了通道组，先进行通道路由
            ChannelInfoDetailResDTO channelInfo = channelService.checkRoute(userInfo, callReqBO);
            checkChannel(calledNumber, channelInfo, userInfo);
            String channelNo = channelInfo.getChannelNo();
            callReqBO.setChannelNo(channelNo);
            log.info("路由到通道: {}", channelNo);

            if (LineTypeEnum.LINE_PURE.eq(channelInfo.getSendType())) {
                log.info("通道为语音线路，原号码返回, 通道: {}", channelNo);
                //通道是语音线路，不再找号码
                return null;
            }
            //语音号码，通过通道公共号码池找号码
            numberList = chnPubNumPoolCacheService.getChnPubNumList(channelNo);
            if (CollUtil.isEmpty(numberList)) {
                log.warn("该通道下无公共号码: {}", channelNo);
                throw new BusinessException(VosErrCodeEnum.Channel_No_PublicNumber);
            }
            //过滤非启用状态
            numberList = numberService.filterNumberByNotEnable(numberList);
            if (CollUtil.isEmpty(numberList)) {
                log.warn("该通道公共号码全部禁用: {}", channelNo);
                throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
            }
            //通道发送类型过滤
            numberList = numberService.filterNumberBySendType(numberList, sendType);
            if (CollUtil.isEmpty(numberList)) {
                log.warn("该通道下无该类型号码, 通道: {}, 发送类型: {}", channelNo, sendType);
                throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
            }
            //通过省份城市和并发数过滤
            numberList = numberService.filterNumberByArea(numberList, callReqBO, numberFitlerType);
        } else {
            log.info("号码池独享模式, 账号: {}", userNo);
            //账号未配置通道组，从客户私有号码池中获取筛选
            numberList = custPriNumPoolCacheService.getCustPriNumList(userNo);
            //过滤非启用状态
            numberList = numberService.filterNumberByNotEnable(numberList);
            //通道发送类型过滤
            numberList = numberService.filterNumberBySendType(numberList, sendType);

            log.info("客户号码池号码, 账号: {}, 数量: {}", userNo, numberList != null ? numberList.size() : 0);

            if (CollUtil.isEmpty(numberList)) {
                log.warn("号码池独享模式, 无可用号码: {}", callReqBO.getCallId());
                throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
//                String calledCity = callReqBO.getCalledCity();
//                //获取所属城市号码池列表
//                numberList = cityPubNumPoolCacheService.getCityPubNumList(calledCity);
//                numberList = filterMore(numberList);
//                //通道发送类型过滤
//                numberList = numberService.filterNumberBySendType(numberList, sendType);
//
//                log.info("城市号码池号码, 城市: {}, 数量： {}", calledCity, numberList != null ? numberList.size() : 0);
//
//                //严格筛选，只筛选
//                if (NumberFilterTypeEnum.ExactMatch.eq(numberFitlerType) && CollUtil.isEmpty(numberList)) {
//                    log.warn("严格筛选后无可用号码");
//                    throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
//                }
//
//                if (CollUtil.isEmpty(numberList)) {
//                    //如果列表为空，再从省从公共号池中获取
//                    String calledProvince = callReqBO.getCalledProvince();
//                    //获取所属省份号码池列表
//                    numberList = provincePubNumPoolCacheService.getProvincePubNumList(calledProvince);
//                    numberList = filterMore(numberList);
//                    //通道发送类型过滤
//                    numberList = numberService.filterNumberBySendType(numberList, sendType);
//
//                    log.info("省份号码池号码, 省份: {}, 数量： {}", calledProvince, numberList != null ? numberList.size() : 0);
//
//                    if (CollUtil.isEmpty(numberList)) {
//
//                        //如果列表为空，再从全国过滤
//                        String national = "000000";
//                        //获取所属省份号码池列表
//                        numberList = provincePubNumPoolCacheService.getProvincePubNumList(national);
//                        numberList = filterMore(numberList);
//                        //通道发送类型过滤
//                        numberList = numberService.filterNumberBySendType(numberList, sendType);
//
//                        log.info("全国号码池号码, 数量： {}", numberList != null ? numberList.size() : 0);
//
//                        if (CollUtil.isEmpty(numberList)) {
//                            log.warn("该省份下无公共号码: {}", calledProvince);
//                            throw new BusinessException(VosErrCodeEnum.Channel_No_PublicNumber);
//                        }
//                    }
//                }
            } else {
                //通过通道状态过滤
                numberList = numberService.filterNumberByChannnelStatus(numberList);
                log.info("通道状态过滤后号码, 数量： {}", numberList != null ? numberList.size() : 0);
                if (CollUtil.isEmpty(numberList)) {
                    log.warn("通道状态过滤后无可用号码");
                    throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
                }
                //通过省份城市和并发数筛选
                numberList = numberService.filterNumberByArea(numberList, callReqBO, numberFitlerType);
            }
        }
        //过滤出最小费率
        numberList = numberService.filterNumberByRate(numberList);

        //通过行业过滤，通知类 呼叫次数最少的，其他 呼叫次数>0
        numberList = numberService.filterNumberByIndustry(numberList, userNo);

        ChannelNumberInfoDataBO numberInfo = numberList.get(0);
        if (numberList.size() > 1) {
            numberInfo = RandomUtil.randomEle(numberList);
        }
        callReqBO.setNumberA(numberInfo.getNumberNo());
        callReqBO.setChannelNo(numberInfo.getChannelNo());
        callReqBO.setCallingProvince(numberInfo.getProvince());
        callReqBO.setCallingCity(numberInfo.getCity());
        return numberInfo;
    }

    private List<ChannelNumberInfoDataBO> filterMore(String channelNo, List<ChannelNumberInfoDataBO> numberList) {
        //过滤非启用状态
        numberList = numberService.filterNumberByNotEnable(numberList);
        //通过并发数进行筛选
        numberList = numberService.filterNumberByConcurrency(numberList);
        //过滤小号使用频率
        numberList = numberService.filterNumberByFrequency(channelNo, numberList);
        //通过通道状态过滤
        numberList = numberService.filterNumberByChannnelStatus(numberList);
        return numberList;
    }

    private void checkChannel(String calledNumber, ChannelInfoDetailResDTO channelInfo, CustomerUserInfoQueryRespDTO userInfo) {
        if (channelInfo == null) {
            log.warn("无效通道, 账号: {}, 号码: {}", userInfo.getUserNo(), calledNumber);
            throw new BusinessException(VosErrCodeEnum.Channel_Invalid);
        }
        if (!StatusEnum.ENABLE.eq(channelInfo.getStatus())) {
            log.warn("通道已停用, 账号: {}, 号码: {}", userInfo.getUserNo(), calledNumber);
            throw new BusinessException(VosErrCodeEnum.Channel_Disable);
        }
    }

    private void provinceCity(CallRequestDataBO callReqBO) {
        String callerNumber = callReqBO.getCallingNumber();
        String calledNumber = callReqBO.getCalledNumber();
//        //主叫所属省份
//        String callingProvince = "";
//        //主叫所属省份
//        String callingCity = "";
        //被叫所属省份
        String calledProvince = "";
        //被叫所属省份
        String calledCity = "";

//        OperatorsInfoBO callingOperatorsInfoBO;
//        if (callerNumber.length() == 11 && callerNumber.startsWith("1")) {
//            //11位1开头时，识别为手机号
//            callingOperatorsInfoBO = numberService.getMobileInfo(callerNumber);
//        } else {
//            //其它识别为座机号
//            callingOperatorsInfoBO = numberService.getPhoneInfo(callerNumber);
//        }
//        if (callingOperatorsInfoBO != null) {
//            callingProvince = callingOperatorsInfoBO.getProvince();
//            callingCity = callingOperatorsInfoBO.getCity();
//        }

//        log.info("[CallService] 主叫省份: {}, 主叫城市: {}", callingProvince, callingCity);

        OperatorsInfoBO calledOperatorsInfoBO;
        if (calledNumber.length() == 11 && calledNumber.startsWith("1")) {
            calledOperatorsInfoBO = numberService.getMobileInfo(calledNumber);
        } else {
            calledOperatorsInfoBO = numberService.getPhoneInfo(calledNumber);
        }
        if (calledOperatorsInfoBO != null) {
            calledProvince = calledOperatorsInfoBO.getProvince();
            calledCity = calledOperatorsInfoBO.getCity();
        }
        log.info("[CallService] 被叫省份: {}, 被叫城市: {}", calledProvince, calledCity);

//        callReqBO.setCallingProvince(callingProvince);
//        callReqBO.setCallingCity(callingCity);
        callReqBO.setCalledProvince(calledProvince);
        callReqBO.setCalledCity(calledCity);
    }

    public void addErrInfo(CallRequestDataBO callReqBO, BusinessException errInfo) {
        if (callReqBO == null) {
            return;
        }
        callReqBO.setRouteStatus((byte) 0);
        callReqBO.setErrCode(errInfo.getCode());
        String message = errInfo.getMessage();
        if (message != null && message.length() > 64) {
            message = message.substring(0, 64);
        }
        callReqBO.setErrMsg(message);
    }

    private CallRequestDataBO buildCallRequest(RouteReqDTO reqDTO) {
        String platCallId = String.valueOf(snowFlakeService.nextId());
        String userNo = reqDTO.getCallerid().substring(0, 6);
        String callerNumber = reqDTO.getCallerid().substring(6);
        String calledNumber = reqDTO.getCalleeid();
        String callid = reqDTO.getCallid();
        if (StrUtil.isEmpty(callerNumber)) {
            callerNumber = userNo;
        }

        return CallRequestDataBO.builder()
                .userNo(userNo)
                .needRecord((byte) 0) //默认不录音
                .platCallId(platCallId)
                .callId(callid)
                .callingNumber(callerNumber)
                .calledNumber(calledNumber)
                .requestTime(DateUtil.date().toString("yyyy-MM-dd HH:mm:ss"))
                .tableTime(DateUtil.date().toString("yyyyMMdd"))
                .build();
    }

    private void addCallRequest(CallRequestDataBO callReqBO) {
        if (callReqBO == null) {
            log.error("话单请求流水为空");
            return;
        }
        log.info("插入话单请求流水: {}", JSONUtil.toJsonStr(callReqBO));
        SysConstant.processExecutors.execute(() -> callRequestQueue.add(callReqBO));
//        addPlatCallIdRedis(userNo, callid, platCallId);
    }

    private void addChannelRedis(CallRequestDataBO callReqBO, boolean isCustRecord) {
        String userNo = callReqBO.getUserNo();
        String callId = callReqBO.getCallId();
        String groupNo = callReqBO.getGroupNo();
        String channelNo = callReqBO.getChannelNo();
        String numberA = callReqBO.getNumberA();
        //最多保存2天
        int expTime = DateUtils.getCurrent2TodayEndSecondTime() + 24 * 60 * 60;
        String date = DateUtil.date().toString("yyyyMMdd");
        String redisKey = StrUtil.format("CallReqChannelNo:{}:{}", userNo, date);
        String cacheStr = StrUtil.format("{}|{}|{}", groupNo, channelNo, (isCustRecord ? 1 : 0));
        if (StrUtil.isNotEmpty(numberA)) {
            cacheStr += "|" + numberA;
        }
        stringRedisCacheService.setHash(redisKey, callId, cacheStr, expTime);
    }

//    private void addPlatCallIdRedis(String userNo, String callid, String platCallId) {
//        //最多保存2天
//        int expTime = DateUtils.getCurrent2TodayEndSecondTime() + 24*60*60;
//        String date = DateUtil.date().toString("yyyyMMdd");
//        String redisKey = StrUtil.format("CallReqPlatCallId:{}:{}", userNo, date);
//        stringRedisCacheService.setHash(redisKey, callid, platCallId, expTime);
//    }
}
