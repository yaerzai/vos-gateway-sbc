package com.ytl.vos.gateway.sbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.db.model.PageData;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailResDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerBaseInfoQueryRespDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.customer.api.service.base.CustomerWhiteBusinessService;
import com.ytl.vos.customer.enums.CustomerComplaintBlackLevelEnum;
import com.ytl.vos.customer.enums.CustomerCustUnsubeCheckFlagEnum;
import com.ytl.vos.customer.enums.CustomerIndUnsubeCheckFlagEnum;
import com.ytl.vos.gateway.sbc.constant.SysConstant;
import com.ytl.vos.gateway.sbc.enums.NumberPoolTypeEnum;
import com.ytl.vos.gateway.sbc.enums.UnsubLevelEnum;
import com.ytl.vos.gateway.sbc.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.sbc.jms.producer.BlackMobileProducer;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.NumberService;
import com.ytl.vos.gateway.sbc.service.ThirdBlackService;
import com.ytl.vos.gateway.sbc.service.bo.OperatorsInfoBO;
import com.ytl.vos.jms.code.dto.black.BlackMobileDTO;
import com.ytl.vos.persistence.dataservice.*;
import com.ytl.vos.persistence.dataservice.bo.*;
import com.ytl.vos.persistence.dataservice.bo.business.SysMobileSegmentBusinessBO;
import com.ytl.vos.persistence.dataservice.bo.business.SysMobileTransferBusinessBO;
import com.ytl.vos.persistence.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 号码服务
 */
@Service
@Slf4j
public class NumberServiceImpl implements NumberService {

    @Resource
    private CustomerWhiteBusinessService customerWhiteBusinessService;
    @Resource
    private UnsubscribeBlackMobileCacheDataService unsubscribeBlackMobileCacheDataService;
    @Resource
    private ComplaintBlackMobileCacheDataService complaintBlackMobileCacheDataService;

    @Resource
    private SysMobileTransferDataService mobileTransferDataService;
    @Resource
    private SysMobileSegmentDataService mobileSegmentDataService;
    @Resource
    private SysAreaDataService sysAreaDataService;
    @Resource
    private DataService dataService;
    @Resource
    private NumberCacheService numberCacheService;
    @Resource
    private CustPriNumPoolCacheService custPriNumPoolCacheService;
    @Resource
    private PlatCallBlackMobileDataService platCallBlackMobileDataService;
    @Resource
    private CustTestLimitMobileDataService custTestLimitMobileDataService;
    @Resource
    private Map<String, ThirdBlackService> thirdBlackServiceMap;
    @Resource
    private RedisCacheService redisCacheService;
    @Resource
    private BlackMobileProducer blackMobileProducer;
    @Resource
    private PriNumberCacheService priNumberCacheService;

    private Map<String, SysAreaDataBO> areaCodeMap;

    /**
     * 号码黑名单检查
     *
     * @param userInfo     账号信息
     * @param callerNumber 被叫号码
     */
    @Override
    public void checkNumber(CustomerUserInfoQueryRespDTO userInfo, String callerNumber, String calledNumber) {
        String customerNo = userInfo.getCustomerNo();
        String userNo = userInfo.getUserNo();
        CustomerBaseInfoQueryRespDTO customerInfo = dataService.getCustomerInfo(customerNo);
        TestWhiteFlagEnum userWhiteFlag = TestWhiteFlagEnum.parse(userInfo.getCalledWhiteFlag());
        boolean needCheckCustomer = true;
        testLimit: if (userWhiteFlag != TestWhiteFlagEnum.Disable) {
            String item = calledNumber;
            if (userWhiteFlag != TestWhiteFlagEnum.Eleven && calledNumber.length() > userWhiteFlag.getDigit()) {
                item = calledNumber.substring(0, userWhiteFlag.getDigit());
            }
            if (checkMobileExists(customerNo, userNo, item)) {
                needCheckCustomer = false;
                break testLimit;
            }
            if (customerInfo != null) {
                TestWhiteFlagEnum customerWhiteFlag = TestWhiteFlagEnum.parse(customerInfo.getErrorPushFlag());
                if (customerWhiteFlag != TestWhiteFlagEnum.Disable) {
                    break testLimit;
                }
            }
            log.warn("非客户账号被叫白名单号码: {}", calledNumber);
            throw new BusinessException(VosErrCodeEnum.Number_NotCustTestLimitMobile, "非客户被叫白名单");
        }

        if (needCheckCustomer && customerInfo != null) { //账号不检查的时候，再检查客户
            TestWhiteFlagEnum customerWhiteFlag = TestWhiteFlagEnum.parse(customerInfo.getErrorPushFlag());
            testLimit: if (customerWhiteFlag != TestWhiteFlagEnum.Disable) {
                String item = calledNumber;
                if (customerWhiteFlag != TestWhiteFlagEnum.Eleven && calledNumber.length() > customerWhiteFlag.getDigit()) {
                    item = calledNumber.substring(0, customerWhiteFlag.getDigit());
                }
                if (checkMobileExists(customerNo, item)) {
                    break testLimit;
                }
                log.warn("非客户被叫白名单号码: {}", calledNumber);
                throw new BusinessException(VosErrCodeEnum.Number_NotCustTestLimitMobile, "非客户被叫白名单");
            }
        }


        Byte callBlackCheckFlag = userInfo.getCallBlackCheckFlag();
        if (callBlackCheckFlag != null && callBlackCheckFlag == 1) {
            //检查主叫黑名单
            if (platCallBlackMobileDataService.isBlackMobile(PlatCallBlackMobileLevelEnum.UserNo, userNo, callerNumber)) {
                log.warn("主叫账号黑名单号码: {}", callerNumber);
                throw new BusinessException(VosErrCodeEnum.Number_Call_Black, StrUtil.format("主叫账号黑名单号码: {}", callerNumber));
            }
            //检查主叫黑名单
            if (platCallBlackMobileDataService.isBlackMobile(PlatCallBlackMobileLevelEnum.CustomerNo, customerNo, callerNumber)) {
                log.warn("主叫客户黑名单号码: {}", callerNumber);
                throw new BusinessException(VosErrCodeEnum.Number_Call_Black, StrUtil.format("主叫客户黑名单号码: {}", callerNumber));
            }
        }

        //获取白名单
        boolean isWhite = isWhite(userInfo, calledNumber);
        if (isWhite) {
            //白名单不过黑名单
            return;
        }

        //退订黑名单检查
        AtomicReference<String> unsubMsg = new AtomicReference<>();
        UnsubLevelEnum unsubLevelEnum = isUnsubscribeBlack(userInfo, calledNumber, unsubMsg);
        if (unsubLevelEnum != null) {
            log.warn("{}[{}]", unsubMsg.get(), unsubLevelEnum.getCodeName());
            throw new BusinessException(VosErrCodeEnum.Number_Unsub_Black, StrUtil.format("{}[{}]", unsubMsg.get(), unsubLevelEnum.getCodeName()));
        }

        //投诉黑名单检查
        CustomerComplaintBlackLevelEnum complaintBlack = isComplaintBlack(userInfo, calledNumber);
        if (complaintBlack != null) {
            log.warn("投诉黑名单[{}]", complaintBlack.getCodeName());
            throw new BusinessException(VosErrCodeEnum.Number_Complaint_Black, StrUtil.format("投诉黑名单[{}]", complaintBlack.getCodeName()));
        }

        // 第三方黑名单检查
        if (thirdRiskMobileProcess(userInfo.getRiskProcessFlag(), calledNumber)) {
            throw new BusinessException(VosErrCodeEnum.Number_Third_Black, VosErrCodeEnum.Number_Third_Black.getMsg());
        }
    }

    @Override
    public ChannelNumberInfoDataBO checkCustPrivateNumber(CustomerUserInfoQueryRespDTO userInfo, CallRequestDataBO callReqBO) {
        //账号
        String userNo = callReqBO.getUserNo();
        //主叫号码
        String callingNumber = callReqBO.getCallingNumber();
        // 是否支持私有号码池
        if (NumberPoolTypeEnum.CallExclusive.getCodeId() != userInfo.getEnforcePreSign()) {
            log.info("账号不支持主叫独享, 账号: {}, 号码: {}", userNo, callingNumber);
            return null;
        }

        ChannelNumberInfoDataBO privateNumber = custPriNumPoolCacheService.getCustPriNum(userNo, callingNumber);
        if (privateNumber == null) {
            log.info("账号下的主叫号码不是私有号码, 账号: {}, 号码: {}", userNo, callingNumber);
            throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
        }
        if (!StatusEnum.ENABLE.eq(privateNumber.getStatus())) {
            log.info("号码非启用状态, 账号: {}, 号码: {}", userNo, callingNumber);
            throw new BusinessException(VosErrCodeEnum.Number_NotEnable);
        }
        //号码最大并发数
//        Integer numberMaxConc = privateNumber.getConcurrency();
        ChannelNumberPrivateDataBO priNumber = priNumberCacheService.getPriNumber(callingNumber, userNo);
        Integer numberMaxConc = priNumber.getConcurrency();
        //号码当前并发数 私有号
//        Integer numberConc = numberCacheService.getNumberConcurrency(callingNumber);
        Integer numberConc = priNumberCacheService.getNumberConcurrency(callingNumber, userNo);
        //检查号码并发数是否超过最大并发 按账号单独控并发数
        if (numberMaxConc != null && numberConc != null && numberConc >= numberMaxConc) {
            String channelNo = privateNumber.getChannelNo();
            String delayChannelNoStr = dataService.getSysParam(SysParamEnum.VOS_NUMBER_CONCURRENT_DELAY_CHANNEL_NO);
            List<String> delayChannelNoList = Arrays.asList(delayChannelNoStr.split(","));
            log.info("账号: {}, 通道: {}, 号码: {}, 并发数: {}, 超过最大并发数: {}", userNo, channelNo, callingNumber, numberConc, numberMaxConc);
//            throw new BusinessException(VosErrCodeEnum.Customer_Number_OutMaxConc);
            String delayFlag = dataService.getSysParam(SysParamEnum.VOS_NUMBER_CONCURRENT_DELAY_FLAG);
            if ("1".equals(delayFlag) && delayChannelNoList.contains(channelNo)) {
                int delayTime = dataService.getSysParamInt(SysParamEnum.VOS_NUMBER_CONCURRENT_DELAY_TIME);
                if (delayTime > 0) {
                    log.warn("[SBC网关] 小号并发数超限,延迟: {} ms", delayTime);
                    ThreadUtil.sleep(delayTime);
                }
            }
        }
        String channelNo = privateNumber.getChannelNo();
        String numberNo = privateNumber.getNumberNo();
        if (checkNumberFrequencyLimitOut(channelNo, numberNo)) {
            log.info("账号: {}, 号码: {}, 通道: {}, 使用频率超出{}分钟{}次", userNo, numberNo, channelNo,
                    getChannelNumberFrequencyLimitCycle(channelNo), getChannelNumberFrequencyLimitTimes(channelNo));
            throw new BusinessException(VosErrCodeEnum.Customer_Frequency_Out);
//            int delayTime = dataService.getSysParamInt(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_DELAY);
//            ThreadUtil.sleep(delayTime);
        }

        log.info("私有小号, 号码: {}, 通道: {}", numberNo, channelNo);

        callReqBO.setChannelNo(channelNo);
        callReqBO.setNumberA(numberNo);
        callReqBO.setCallingProvince(privateNumber.getProvince());
        callReqBO.setCallingCity(privateNumber.getCity());
        return privateNumber;
    }

    @Override
    public OperatorsInfoBO getMobileInfo(String mobileNo) {
        OperatorsInfoBO operatorsInfoBo = OperatorsInfoBO.builder().build();

        SysMobileTransferBusinessBO sysMobileTransfer = mobileTransferDataService.getCache(mobileNo);
        if (sysMobileTransfer != null) {
            operatorsInfoBo.setMobileNo(mobileNo);
            operatorsInfoBo.setOperatorType(sysMobileTransfer.getTelecomType());
            operatorsInfoBo.setProvince(sysMobileTransfer.getProvince());
            operatorsInfoBo.setCity(sysMobileTransfer.getCity());
            return operatorsInfoBo;
        }

        // 手机卡段
        SysMobileSegmentBusinessBO sysMobileSegment = mobileSegmentDataService.getCache(mobileNo);
        if (sysMobileSegment != null) {
            operatorsInfoBo.setMobileNo(mobileNo);
            operatorsInfoBo.setOperatorType(sysMobileSegment.getTelecomType());
            operatorsInfoBo.setProvince(sysMobileSegment.getProvince());
            operatorsInfoBo.setCity(sysMobileSegment.getCity());
            return operatorsInfoBo;
        }
        return null;
    }

    @Override
    public OperatorsInfoBO getPhoneInfo(String phone) {
        if (phone.length() < 5) {
            return null;
        }
        String code1 = phone.substring(0, 3);
        String code2 = phone.substring(0, 4);
        String code3 = phone.substring(0, 5);

        SysAreaDataBO areaData = getAreaData(code1, code2, code3);
        if (areaData == null) {
            return null;
        }

        return OperatorsInfoBO.builder()
                .province(areaData.getParentNo())
                .city(areaData.getAreaCode())
                .build();
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByArea(List<ChannelNumberInfoDataBO> numberList, CallRequestDataBO callReqBO, Byte numberFitlerType) {
        String calledProvince = callReqBO.getCalledProvince();
        String calledCity = callReqBO.getCalledCity();

        List<ChannelNumberInfoDataBO> retNumberList = null;
        if (StrUtil.isNotEmpty(calledCity)) {
            //通过城市过滤
            retNumberList = filterNumber(numberList, numberInfo -> calledCity.equals(numberInfo.getCity()));
            //过滤并发数
            retNumberList = filterNumberByConcurrency(retNumberList);
            //过滤小号使用频率
            retNumberList = filterNumberByFrequency(callReqBO.getChannelNo(), retNumberList);

            log.info("城市号码过滤, 城市: {}, 数量: {}", calledCity, retNumberList != null ? retNumberList.size() : 0);
        } else {
            log.info("无法获取到城市");
        }

        //严格筛选，只筛选
        if (NumberFilterTypeEnum.ExactMatch.eq(numberFitlerType) && CollUtil.isEmpty(retNumberList)) {
            log.warn("严格筛选后无可用号码");
            throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
        }

        if (CollUtil.isEmpty(retNumberList) && StrUtil.isNotEmpty(calledProvince)) {
            //如果列表为空，再通过省份再过滤一遍
            retNumberList = filterNumber(numberList, numberInfo -> calledProvince.equals(numberInfo.getProvince()));
            //过滤并发数
            retNumberList = filterNumberByConcurrency(retNumberList);
            //过滤小号使用频率
            retNumberList = filterNumberByFrequency(callReqBO.getChannelNo(), retNumberList);

            log.info("省份号码过滤, 省份: {}, 数量: {}", calledProvince, retNumberList != null ? retNumberList.size() : 0);
        } else {
            if (StrUtil.isEmpty(calledProvince)) {
                log.info("无法获取到省份");
            }
        }

        if (CollUtil.isEmpty(retNumberList)) {
            String national = "000000";
            //如果列表为空，再通过全国再过滤一遍
            retNumberList = filterNumber(numberList, numberInfo -> calledProvince.equals(national));
            //过滤并发数
            retNumberList = filterNumberByConcurrency(retNumberList);
            //过滤小号使用频率
            retNumberList = filterNumberByFrequency(callReqBO.getChannelNo(), retNumberList);

            log.info("全国号码过滤, 数量: {}", retNumberList != null ? retNumberList.size() : 0);
        }

        if (CollUtil.isEmpty(retNumberList)) {
            //如果还为空，再通过并发数过滤一遍
            retNumberList = filterNumberByConcurrency(numberList);
            //过滤小号使用频率
            retNumberList = filterNumberByFrequency(callReqBO.getChannelNo(), retNumberList);

            log.info("城市、省份、全国过滤后无号码可用,使用并发数满足的号码, 数量: {}, 城市: {}, 省份: {}", retNumberList != null ? retNumberList.size() : 0, calledCity, calledProvince);
        }



        if (CollUtil.isEmpty(retNumberList)) {
            log.warn("过滤后无可用号码");
            throw new BusinessException(VosErrCodeEnum.Number_Not_Available);
        }
        return retNumberList;
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByConcurrency(List<ChannelNumberInfoDataBO> numberList) {
        return filterNumber(numberList, this::filterNumberByConcurrency);
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByFrequency(String channelNo, List<ChannelNumberInfoDataBO> numberList) {
        return filterNumber(numberList, item-> !checkNumberFrequencyLimitOut(channelNo, item.getNumberNo()));
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByNotEnable(List<ChannelNumberInfoDataBO> numberList) {
        return filterNumber(numberList, numberInfo -> StatusEnum.ENABLE.eq(numberInfo.getStatus()));
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberBySendType(List<ChannelNumberInfoDataBO> numberList, String sendType) {
        if (StrUtil.isEmpty(sendType)) {
            return null;
        }
        List<Byte> sendTypes = Arrays.stream(sendType.split(",")).map(Byte::valueOf).collect(Collectors.toList());
        return filterNumber(numberList, numberInfo -> numberInfo.getNumberType() != null && sendTypes.stream().anyMatch(numberInfo.getNumberType()::equals));
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByOperatorType(List<ChannelNumberInfoDataBO> numberList, Byte operatorType) {
        return filterNumber(numberList, numberInfo -> operatorType.equals(numberInfo.getOperatorType()));
    }


    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByChannnelStatus(List<ChannelNumberInfoDataBO> numberList) {
        return filterNumber(numberList, numberInfo -> {
            String channelNo = numberInfo.getChannelNo();
            ChannelInfoDetailResDTO channelInfo = dataService.getChannelInfo(channelNo);
            return channelInfo != null && StatusEnum.ENABLE.eq(channelInfo.getStatus());
        });
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByRate(List<ChannelNumberInfoDataBO> numberList) {
        if (CollUtil.isEmpty(numberList) || numberList.size() == 1) {
            return numberList;
        }
        BigDecimal minFeeEver = null;
        for (ChannelNumberInfoDataBO numberInfo : numberList) {
            BigDecimal feeEver = getFeeEver(numberInfo);
            if (minFeeEver == null) {
                minFeeEver = feeEver;
            } else {
                minFeeEver = NumberUtil.min(feeEver, minFeeEver);
            }
        }
        BigDecimal finalMinFeeEver = minFeeEver;
        return filterNumber(numberList, numberInfo -> getFeeEver(numberInfo).equals(finalMinFeeEver));
    }

    @Override
    public List<ChannelNumberInfoDataBO> filterNumberByIndustry(List<ChannelNumberInfoDataBO> numberList, String userNo) {
        if (CollUtil.isEmpty(numberList) || numberList.size() == 1) {
            return numberList;
        }
        CustomerUserInfoQueryRespDTO userInfo = dataService.getCustomerUserInfo(userNo);
        String firstIndustryNo = userInfo.getFirstIndustryNo();

        String notifyIndustryId = dataService.getSysParam(SysParamEnum.NOTIFY_INDUSTRY_ID);

        if (notifyIndustryId.equals(firstIndustryNo)) {
            //通知类 呼叫次数最少的
            Integer minCallNum = null;
            for (ChannelNumberInfoDataBO numberInfo : numberList) {
                String numberNo = numberInfo.getNumberNo();
                Integer callNum = numberCacheService.getCallNum(numberNo);
                if (minCallNum == null) {
                    minCallNum = callNum;
                } else {
                    minCallNum = Math.min(minCallNum, callNum);
                }
            }

            Integer finalMinCallNum = minCallNum;
            return filterNumber(numberList, numberInfo -> numberCacheService.getCallNum(numberInfo.getNumberNo()).equals(finalMinCallNum));
        } else {
            //其它 呼叫次数>0
            List<ChannelNumberInfoDataBO> retNumberList = filterNumber(numberList, numberInfo -> numberCacheService.getCallNum(numberInfo.getNumberNo()) > 0);
            if (CollUtil.isEmpty(retNumberList)) {
                return numberList;
            }
            return retNumberList;
        }
    }

    private boolean checkMobileExists(String customerNo, String item) {
        CustTestLimitMobileDataBO dataBO = custTestLimitMobileDataService.getCache(customerNo, item);
        if (dataBO != null) {
            log.info("匹配被叫白名单, key: {}, item: {}", customerNo, item);
            return true;
        }
        return false;
    }

    private boolean checkMobileExists(String customerNo, String userNo, String item) {
        CustTestLimitMobileDataBO dataBO = custTestLimitMobileDataService.getCache(customerNo + "_" + userNo, item);
        if (dataBO != null) {
            log.info("匹配被叫白名单, key: {}, item: {}", customerNo + "_" + userNo, item);
            return true;
        }
        return false;
    }

    @Override
    public boolean checkNumberFrequencyLimitOut(String channelNo, String number) {
        String limitFlag = dataService.getSysParam(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_FLAG);
        if (!"1".equals(limitFlag)) {
            return false;
        }
        String limitChannelNos = dataService.getSysParam(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_CHANNEL_NO);
        List<String> channelNoList = Arrays.asList(limitChannelNos.split(","));
        if (!channelNoList.contains(channelNo)) {
            return false;
        }
        int limitCycle = getChannelNumberFrequencyLimitCycle(channelNo);
        int limitTimes = getChannelNumberFrequencyLimitTimes(channelNo);
        int expire = limitCycle * 60;
        long curr = redisCacheService.incrementAndGet(SysConstant.VOS_NUMBER_FREQUENCY + ":" + number, Integer.MAX_VALUE, 0, expire);
        return curr >= limitTimes;
    }

    @Override
    public long numberFrequencyLimitAdd(String channelNo, String number) {
        String limitFlag = dataService.getSysParam(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_FLAG);
        if (!"1".equals(limitFlag)) {
            return 0;
        }
        String limitChannelNos = dataService.getSysParam(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_CHANNEL_NO);
        List<String> channelNos = Arrays.asList(limitChannelNos.split(","));
        if (!channelNos.contains(channelNo)) {
            return 0;
        }
        int limitCycle = getChannelNumberFrequencyLimitCycle(channelNo);
        int expire = limitCycle * 60;
        return redisCacheService.incrementAndGet(SysConstant.VOS_NUMBER_FREQUENCY + ":" + number, Integer.MAX_VALUE, 1, expire);
    }

    @Override
    public int getChannelNumberFrequencyLimitTimes(String channelNo) {
        return dataService.getSysParamInt(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_TIMES);
    }

    @Override
    public int getChannelNumberFrequencyLimitCycle(String channelNo) {
        return dataService.getSysParamInt(SysParamEnum.VOS_NUMBER_FREQUENCY_LIMIT_CYCLE);
    }

    private BigDecimal getFeeEver(ChannelNumberInfoDataBO numberInfo) {
        BigDecimal fee = numberInfo.getFee();
        Integer billingCycle = numberInfo.getBillingCycle();
        //计算出单价费率
        BigDecimal feeEver = NumberUtil.div(fee, new BigDecimal(billingCycle));
        return feeEver;
    }

    //通过并发数过滤
    private boolean filterNumberByConcurrency(ChannelNumberInfoDataBO numberInfo) {
        String numberNo = numberInfo.getNumberNo();
        if (numberCacheService.getNumberConcurrency(numberNo) < numberInfo.getConcurrency()) {
            return true;
        }
        return false;
    }

    //过滤
    private List<ChannelNumberInfoDataBO> filterNumber(List<ChannelNumberInfoDataBO> numberList, Predicate<ChannelNumberInfoDataBO> predicate) {
        if (CollUtil.isEmpty(numberList)) {
            return null;
        }
        return numberList.stream().filter(predicate).collect(Collectors.toList());
    }

    private SysAreaDataBO getAreaData(String code1, String code2, String code3) {
        Map<String, SysAreaDataBO> araeMap = getAreaCodeMap();
        SysAreaDataBO areaData = araeMap.get(code1);
        if (areaData == null) {
            areaData = araeMap.get(code2);
            if (areaData == null) {
                areaData = araeMap.get(code3);
            }
        }
        return areaData;
    }


    private Map<String, SysAreaDataBO> getAreaCodeMap() {
        if (areaCodeMap == null || isAreaUpdated()) {
            synchronized (this) {
                if (areaCodeMap == null || isAreaUpdated()) {
                    initAreaCodeMap();
                }
            }
        }
        return areaCodeMap;
    }


    private boolean isAreaUpdated() {
        String openFlag = dataService.getSysParam(SysParamEnum.VOS_TASK_AREA_UPDATE_OPEN_FLAG);
        if (!"1".equals(openFlag)) {
            return false;
        }

        //上一次更新超过1分钟，再进行下一次更新
        if ((System.currentTimeMillis() - areaLastUpdateTime) / (1000 * 60) > 0) {
            return true;
        }
        return false;
    }


    private Long areaLastUpdateTime;

    private void initAreaCodeMap() {
        SysAreaQueryBO queryBO = new SysAreaQueryBO();
        queryBO.setPageSize(1000);
        PageData<SysAreaDataBO> query = sysAreaDataService.query(queryBO);
        areaCodeMap = new HashMap<>();

        for (SysAreaDataBO areaDate : query.getData()) {
            if (areaDate.getAreaLevel() != AreaLeavelEnum.CITY.getCodeId()) {
                continue;
            }
            if (!areaCodeMap.containsKey(areaDate.getAreaCode())) {
                areaCodeMap.put(areaDate.getAreaCode(), areaDate);
            }
        }
        areaLastUpdateTime = System.currentTimeMillis();

        log.warn("areaCodeMap更新完成");
    }

    private boolean isWhite(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        String customerNo = userInfo.getCustomerNo();
        String userNo = userInfo.getUserNo();

        String groupNo = customerWhiteBusinessService.getGroupNo(customerNo, userNo, calleeid);
        if (groupNo == null) {
            groupNo = customerWhiteBusinessService.getGroupNo(customerNo, calleeid);
            if (groupNo != null) {
                log.info("客户白名单, 账号: {}, 号码: {}", userNo, calleeid);
            }
        } else {
            log.info("账号白名单, 账号: {}, 号码: {}", userNo, calleeid);
        }
        if (groupNo != null) {
            return true;
        }
        log.debug("该号码不是白名单, 账号: {}, 号码: {}", userNo, calleeid);
        return false;
    }

    private UnsubLevelEnum isUnsubscribeBlack(CustomerUserInfoQueryRespDTO userInfo, String calleeid, AtomicReference<String> unsubMsg) {
        String customerNo = userInfo.getCustomerNo();
        String userNo = userInfo.getUserNo();
        if (
                CustomerCustUnsubeCheckFlagEnum.NOT_CHECK.eq(userInfo.getCustUnsubeCheckFlag())
                &&
                CustomerIndUnsubeCheckFlagEnum.NOT_CHECK.eq(userInfo.getIndUnsubeCheckFlag())
        ) {
            log.debug("该号码未配置退订检查, 账号: {}, 号码: {}", userNo, calleeid);
            return null;
        }

        List<UnsubscribeBlackMobileDataBO> blackList = unsubscribeBlackMobileCacheDataService.getUnsubscribeBlackList(calleeid);

        //客户退订
        if (CustomerCustUnsubeCheckFlagEnum.CUSTOMER_CHECK.getCodeId() == userInfo.getCustUnsubeCheckFlag()) {
            UnsubLevelEnum unsubLevel = getUnsubLevel(blackList, item -> item.getCustNo().equals(customerNo));
            if (unsubLevel != null) {
                unsubMsg.set("客户退订");
                log.info("满足客户退订, 客户: {}, 号码: {}, 级别: {}", customerNo, calleeid, unsubLevel.getCodeName());
                return unsubLevel;
            }
        }

        //账号退订
        if (CustomerCustUnsubeCheckFlagEnum.USER_CHECK.getCodeId() == userInfo.getCustUnsubeCheckFlag()) {
            //判断退订列表有未包含该账号
            UnsubLevelEnum unsubLevel = getUnsubLevel(blackList, item -> userNo.equals(item.getUserNo()));
            if (unsubLevel != null) {
                unsubMsg.set("账号退订");
                log.info("满足账号退订, 账号: {}, 号码: {}, 级别: {}", userNo, calleeid, unsubLevel.getCodeName());
                return unsubLevel;
            }
        }

        //行业退订
        String industryNo = userInfo.getSecondIndustryNo();
        if (CustomerIndUnsubeCheckFlagEnum.INDUSTRY_PRECISE_CHECK.eq(userInfo.getIndUnsubeCheckFlag()) && StrUtil.isNotEmpty(industryNo)) {
            UnsubLevelEnum unsubLevel = getUnsubLevel(blackList, item -> industryNo.equals(item.getIndustry()));
            if (unsubLevel != null) {
                unsubMsg.set("行业精确退订");
                log.info("满足行业精确退订, industryNo: {}, 级别: {}", userNo, unsubLevel.getCodeName());
                return unsubLevel;
            }
        }

        log.debug("该号码未退订, 账号: {}, 号码: {}", userNo, calleeid);
        return null;
    }

    private CustomerComplaintBlackLevelEnum isComplaintBlack(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        List<Integer> complaintBlackCheckFlag = userInfo.getComplaintBlackCheckFlag();
        if (CollUtil.isEmpty(complaintBlackCheckFlag)) {
            log.debug("该账号未配置投诉检查: {}", userInfo.getUserNo());
            return null;
        }

        List<ComplaintBlackMobileDataBO> complaintBlackList = complaintBlackMobileCacheDataService.getComplaintBlackList(calleeid);
        if (CollUtil.isEmpty(complaintBlackList)) {
            log.info("该手机号无投诉记录: {}", calleeid);
            return null;
        }
        for (Integer level : complaintBlackCheckFlag) {
            if (complaintBlackList.stream().anyMatch(item -> item.getLevel() == level)) {
                log.info("该号码{}是{}投诉", calleeid, CustomerComplaintBlackLevelEnum.parse(level).getCodeName());
                return CustomerComplaintBlackLevelEnum.parse(level);
            }
        }
        log.debug("该号码投诉级别未满足: {}", calleeid);
        return null;
    }

    private boolean thirdRiskMobileProcess(byte riskProcessFlag, String calleeId) {
        if (RiskProcessFlagEnum.YES.getCodeId() != riskProcessFlag) {
            return false;
        }

        String thirdBlackChannel = "Ok686"; //多个三方黑名单通道时，需要选择通道
        String serviceName = "ThirdBlack" + thirdBlackChannel;

        ThirdBlackService ok686ThirdBlack = thirdBlackServiceMap.get(serviceName);
        if (ok686ThirdBlack == null) {
            log.warn("三方黑名单调用未实现: {}", serviceName);
            return false;
        }
        addCallCount(thirdBlackChannel);
        addCallCount("ALL");
        boolean checkFlag = ok686ThirdBlack.checkBlack(calleeId);
        if (checkFlag) {
            addHitCount(thirdBlackChannel);
            addHitCount("ALL");
            // 如投诉黑名单库 投诉级别：二级，投诉来源：第三方， 备注：语音黑名单
            BlackMobileDTO blackDTO = BlackMobileDTO.builder()
                    .blackMobileType((byte) 1) //投诉黑名单
                    .mobileNo(calleeId)
                    .level((byte) CustomerComplaintBlackLevelEnum.LDEVE_2.getCodeId())
                    .complaintSource((byte)6) //第三方
                    .remark("JX语音黑名单")
                    .build();
            blackMobileProducer.send(blackDTO);
        }
        return checkFlag;
    }

    private void addCallCount(String thirdBlackChannel) {
        String key = StrUtil.format("ThirdBlackCallCount:{}:{}", thirdBlackChannel, DateUtil.date().toString(DatePattern.PURE_DATE_PATTERN));
        DateTime nextDayEnd = DateUtil.offsetDay(DateUtils.getDayEnd(), 1);
        DateTime currDate = DateUtil.date();
        long between = DateUtil.between(currDate, nextDayEnd, DateUnit.SECOND);
        redisCacheService.incrementAndGet(key, Integer.MAX_VALUE, 1, (int) between);
    }

    private void addHitCount(String thirdBlackChannel) {
        String key = StrUtil.format("ThirdBlackHitCount:{}:{}", thirdBlackChannel, DateUtil.date().toString(DatePattern.PURE_DATE_PATTERN));
        DateTime nextDayEnd = DateUtil.offsetDay(DateUtils.getDayEnd(), 1);
        DateTime currDate = DateUtil.date();
        long between = DateUtil.between(currDate, nextDayEnd, DateUnit.SECOND);
        redisCacheService.incrementAndGet(key, Integer.MAX_VALUE, 1, (int) between);
    }

    private UnsubLevelEnum getUnsubLevel(List<UnsubscribeBlackMobileDataBO> blackList, Predicate<UnsubscribeBlackMobileDataBO> predicate) {
        return blackList.stream().filter(predicate)
                .sorted(Comparator.comparing(UnsubscribeBlackMobileDataBO::getLevel).reversed())
                .map(UnsubscribeBlackMobileDataBO::getLevel)
                .map(UnsubLevelEnum::parse)
                .findFirst()
                .orElse(null);
    }
}
