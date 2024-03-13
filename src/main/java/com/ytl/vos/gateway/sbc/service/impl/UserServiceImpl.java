package com.ytl.vos.gateway.sbc.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.common.redis.service.StringRedisCacheService;
import com.ytl.vos.channel.enums.RecordStatusEnum;
import com.ytl.vos.customer.api.dto.customer.CustomerBaseInfoQueryRespDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.customer.enums.CustomerUserFeeTypeEnum;
import com.ytl.vos.customer.enums.CustomerUserLevelEnum;
import com.ytl.vos.gateway.sbc.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.UserService;
import com.ytl.vos.persistence.dataservice.SysParamsDataService;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ytl.vos.gateway.sbc.util.DateUtils.inTimeFrame;
import static com.ytl.vos.gateway.sbc.util.DateUtils.isHolidays;

/**
 * 账号服务
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private DataService dataService;
    @Resource
    private RedisCacheService redisCacheService;
    @Resource
    private StringRedisCacheService stringRedisCacheService;
    @Resource
    private SysParamsDataService sysParamsDataService;

    @Override
    public CustomerUserInfoQueryRespDTO checkUser(CallRequestDataBO callReqBO, String fromip) {
        String userNo = callReqBO.getUserNo();
        String calledNumber = callReqBO.getCalledNumber();

        //检查账号
        CustomerUserInfoQueryRespDTO userInfo = checkUser(callReqBO, userNo, fromip);
//        callReqBO.setCustomerNo(userInfo.getCustomerNo());
//        callReqBO.setGroupNo(userInfo.getChannelGroupNo());

        //检查客户
        checkCustomer(userNo, userInfo);

        //检查账号余额
        if (CustomerUserFeeTypeEnum.PRE_CHARGE.getCodeId() == userInfo.getFeeType()) {
            checkBalance(userNo);
        }

        //流速检查
        checkFlow(userInfo);

        //频次检查
        //账号日限
        checkUserDayLimit(userInfo, calledNumber);
        //账号月限
        checkUserMonthLimit(userInfo, calledNumber);

//        //检查用户并发数
//        checkConcurrent(userInfo);
//        callReqBO.setSendType(userInfo.getSendType());
        callReqBO.setVosMode(userInfo.getSupportMode());
        return userInfo;
    }

    //用户限额增加
    @Override
    public void userLimitIncrement(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        //是限+1
        userDayLimitIncrement(userInfo, calleeid);
        //月限+1
        userMonthLimitIncrement(userInfo, calleeid);

//        //并发数增加
//        userConcurrentIncrement(userInfo);
    }

    @Override
    public boolean isRecord(CustomerUserInfoQueryRespDTO userInfo) {
        Integer sampleRatio = userInfo.getSampleRatio();
        if (sampleRatio == null || sampleRatio <= 0) {
            return false;
        } else if (sampleRatio >= 100) {
            return true;
        }
        log.info("录音比率: {}%", sampleRatio);
        String userNo = userInfo.getUserNo();
        String totalCounterKey = StrUtil.format("RecordCouter:{}:{}", userNo, "Total");
        String recordCounterKey = StrUtil.format("RecordCouter:{}:{}", userNo, "Record");
        int todaySecond = todaySecond(); //计数到明日凌晨
        //总计数
        long totolCount = redisCacheService.incrementAndGet(totalCounterKey, Integer.MAX_VALUE, 0, todaySecond);
        //流量计数
        long detainCount = redisCacheService.incrementAndGet(recordCounterKey, Integer.MAX_VALUE, 0, todaySecond);

        log.info("当前录音计数: {}/{}", detainCount, totolCount);
        //动态百分比
        boolean detainCalc = dynamicDetainCalc(sampleRatio, totolCount, detainCount);
        if (detainCalc) {
            //命中
            redisCacheService.incrementAndGet(recordCounterKey, Integer.MAX_VALUE, 1, todaySecond);
        }
        //总数增加
        redisCacheService.incrementAndGet(totalCounterKey, Integer.MAX_VALUE, 1, todaySecond);
        return detainCalc;
    }

    private void checkBalance(String userNo) {
        String userBalanceStr = stringRedisCacheService.getHash("UserBalance", userNo, String.class);
        if (StrUtil.isEmpty(userBalanceStr)) {
            throw new BusinessException(VosErrCodeEnum.Customer_Balance_Less);
        }
        BigDecimal userBalance = new BigDecimal(userBalanceStr);
        if (userBalance.compareTo(new BigDecimal(0)) <= 0) {
            throw new BusinessException(VosErrCodeEnum.Customer_Balance_Less);
        }
    }

    private static boolean dynamicDetainCalc(int scale, long totolCount, long detainCount) {
        if (scale >= 100) { //百分百中
            return true;
        }
        long dynamicScale = scale * totolCount - detainCount * 100;
//        System.out.print(StrUtil.format("t={},\td={},\ts={}", totolCount, detainCount, dynamicScale));
        if (dynamicScale >= 100) { //百分百中
            return true;
        } else if (dynamicScale <= 0) { //百分百不中
            return false;
        } else {
            return randomPercent((int)dynamicScale);
        }
    }

    public static boolean randomPercent(int robability) {
        int i = RandomUtil.randomInt(100);
        return i < robability;
    }

    private static int todaySecond() {
        DateTime currDate = DateUtil.date();
        DateTime tomorrowDate = DateUtil.offsetDay(DateUtil.beginOfDay(currDate), 1);
        return (int)DateUtil.between(currDate, tomorrowDate, DateUnit.SECOND);
    }

    //检查客户
    private void checkCustomer(String userNo, CustomerUserInfoQueryRespDTO userInfo) {
        String customerNo = userInfo.getCustomerNo();
        //获取客户信息
        CustomerBaseInfoQueryRespDTO custInfo = dataService.getCustomerInfo(customerNo);
        if (custInfo == null) {
            log.error("客户不存在, 账号: {}, 客户: {}", userNo, customerNo);
            throw new BusinessException(VosErrCodeEnum.Customer_NotExists);
        }
        //检查客户状态
        if (RecordStatusEnum.OK.getCodeId() != custInfo.getStatus()) {
            log.warn("客户已禁用, 账号: {}, 客户: {}", userNo, customerNo);
            throw new BusinessException(VosErrCodeEnum.Customer_Stoped);
        }

        //检查主账号
        if (CustomerUserLevelEnum.MASTER_USER.getCodeId() != userInfo.getUserLevel()) {
            CustomerUserInfoQueryRespDTO masterUserInfo = dataService.getCustomerMasterUserInfo(customerNo);
            if (masterUserInfo == null) {
                log.warn("客户主账号不存在, 账号: {}, 客户: {}", userNo, customerNo);
                throw new BusinessException(VosErrCodeEnum.Customer_MasterUser_NotExists);
            }
        }
    }

    //检查账号
    private CustomerUserInfoQueryRespDTO checkUser(CallRequestDataBO callReqBO, String userNo, String fromip) {
        //获取账号信息
        CustomerUserInfoQueryRespDTO userInfo = dataService.getCustomerUserInfo(userNo);
        if (userInfo == null) {
            log.error("客户账号不存在, 账号: {}", userNo);
            throw new BusinessException(VosErrCodeEnum.Customer_User_NotExists);
        }
        callReqBO.setCustomerNo(userInfo.getCustomerNo());
        callReqBO.setGroupNo(userInfo.getChannelGroupNo());

        //检查账号状态
        if (RecordStatusEnum.OK.getCodeId() != userInfo.getStatus()) {
            log.warn("客户账号已禁用, 账号: {}", userNo);
            throw new BusinessException(VosErrCodeEnum.Customer_User_Stoped);
        }

//        // 检查费率组
//        if (StringUtils.isBlank(userInfo.getFeeGroupNo())) {
//            log.error("费率组未绑定, 账号: {}", userNo);
//            throw new BusinessException(VosErrCodeEnum.Customer_Fee_UnBind);
//        }
        Integer paramLength = userInfo.getParamLength();
        BigDecimal feeValue = userInfo.getFeeValue();
        if (paramLength == null || feeValue == null || paramLength <= 0 || feeValue.compareTo(new BigDecimal(0)) <= 0) {
            log.error("费率信息未配置, 账号: {}", userNo);
            throw new BusinessException(VosErrCodeEnum.Customer_Fee_UnConfig);
        }

        //检查ip白名单
        String ipListStr = userInfo.getIpList();
        if (StrUtil.isNotEmpty(ipListStr)) {
            List<String> ipList = Arrays.asList(ipListStr.split(","));
            if (!ipList.contains(fromip)) {
                log.error("IP地址不合法, 账号: {}, IP: {}", userNo, fromip);
                throw new BusinessException(VosErrCodeEnum.Customer_IpCheck_Error);
            }
        }

        //检查呼叫时段
        String allowCallTimeBegin = userInfo.getAllowCallTimeBegin();
        String allowCallTimeEnd = userInfo.getAllowCallTimeEnd();
        DateTime now = DateUtil.date();
        if (!inTimeFrame(now, allowCallTimeBegin, allowCallTimeEnd)) {
            log.error("不在发送时段: {}-{}", allowCallTimeBegin, allowCallTimeEnd);
            throw new BusinessException(VosErrCodeEnum.Customer_NotInTimeFrame);
        }

        //检查节假日能否呼叫
        Byte holidaysCallFlag = userInfo.getHolidaysCallFlag();
        if (holidaysCallFlag != null && 0 == holidaysCallFlag && isHolidays(now, sysParamsDataService)) {
            log.error("节假日不能呼叫: {}", now.toString("yyyyMMdd"));
            throw new BusinessException(VosErrCodeEnum.Customer_Holidays_Disable);
        }

        return userInfo;
    }

    private void checkFlow(CustomerUserInfoQueryRespDTO userInfo) {
        Integer maxPerSecond = userInfo.getMaxPerSecond();
        if (maxPerSecond <= 0) {
            return;
        }
        String time = DateUtil.date().toString("HHmmss");
        String redisKeyName = StrUtil.format("CustFlowLimit:{}:{}", userInfo.getUserNo(), time);

        long custFlow = redisCacheService.incrementAndGet(redisKeyName, Integer.MAX_VALUE, 1, 1);
        if (custFlow > maxPerSecond) {
            log.info("账号超流速, 账号: {}, 限制流速: {}, 当前流速: {}", userInfo.getUserNo(), maxPerSecond, custFlow);
            throw new BusinessException(VosErrCodeEnum.Customer_OutFlow);
        }
    }

    //检查账号日限
    private void checkUserDayLimit(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        //日限周期
        Integer dayMobileLimitCycle = getDayMobileLimitCycle(userInfo.getDayMobileLimitCycle());
        //日阴笔数
        Integer dayMobileLimitNum = userInfo.getDayMobileLimitNum();

        if (dayMobileLimitCycle <= 0 || dayMobileLimitNum == null || dayMobileLimitNum <= 0) {
            return;
        }

        int expTime = DateUtils.getCurrent2TodayEndSecondTime();
        if (dayMobileLimitCycle > 1) {
            expTime += (dayMobileLimitCycle - 1) * 24 * 60 * 60;
        }

        String redisKeyName = StrUtil.format("CustDayLimit:{}:{}", userInfo.getUserNo(), calleeid);
        //TODO 可能会出现key太多的情况
        long callCount = redisCacheService.incrementAndGet(redisKeyName, Integer.MAX_VALUE, 0, expTime);
        if (callCount >= dayMobileLimitNum) {
            log.warn("超过账号日限, 账号: {}, 号码: {}, 日限: {}/{}", userInfo.getUserNo(), calleeid, callCount, dayMobileLimitNum);
            throw new BusinessException(VosErrCodeEnum.Customer_OutDayLimit);
        }
    }

    private int getDayMobileLimitCycle(Integer dayMobileLimitNum) {
        if (dayMobileLimitNum == null) {
            return 0;
        }
        switch (dayMobileLimitNum) {
            case 0:
                return 1;
            case 1:
                return 3;
            case 2:
                return 7;
            case 3:
                return 15;
        }
        return 0;
    }

    //检查账号月限
    private void checkUserMonthLimit(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        //月限笔数
        Integer monthDuplicateNum = userInfo.getMonthDuplicateNum();
        if (monthDuplicateNum == null || monthDuplicateNum <= 0) {
            return;
        }

        String userNo = userInfo.getUserNo();
        String currMonth = DateUtil.date().toString("yyyyMM");
        String redisKeyName = StrUtil.format("CustMonthLimit:{}:{}:{}", userNo, currMonth, calleeid.substring(0, 4));

        DateTime monthEnd = DateUtil.endOfMonth(DateUtil.date());
        int expTime = (int) DateUtil.between(DateUtil.date(), monthEnd, DateUnit.SECOND);

        int callCount = redisCacheService.incrementHash(redisKeyName, calleeid, 0, Integer.MAX_VALUE, expTime);
        if (callCount >= monthDuplicateNum) {
            log.warn("超过账号月限, 账号: {}, 号码: {}, 月限: {}/{}", userNo, calleeid, callCount, monthDuplicateNum);
            throw new BusinessException(VosErrCodeEnum.Customer_OutMonthLimit);
        }
    }

    //账号日限增加
    private void userDayLimitIncrement(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        //日限周期
        Integer dayMobileLimitCycle = getDayMobileLimitCycle(userInfo.getDayMobileLimitCycle());
        if (dayMobileLimitCycle <= 0) {
            return;
        }

        int expTime = DateUtils.getCurrent2TodayEndSecondTime();
        if (dayMobileLimitCycle > 1) {
            expTime += (dayMobileLimitCycle - 1) * 24 * 60 * 60;
        }

        String redisKeyName = StrUtil.format("CustDayLimit:{}:{}", userInfo.getUserNo(), calleeid);
        //TODO 可能会出现key太多的情况
        redisCacheService.incrementAndGet(redisKeyName, Integer.MAX_VALUE, 1, expTime);
    }

    //账号月限增加
    private void userMonthLimitIncrement(CustomerUserInfoQueryRespDTO userInfo, String calleeid) {
        //月限笔数
        DateTime monthEnd = DateUtil.endOfMonth(DateUtil.date());
        int expTime = (int) DateUtil.between(DateUtil.date(), monthEnd, DateUnit.SECOND);

        String redisKeyName = StrUtil.format("CustMonthLimit:{}:{}:{}", userInfo.getUserNo(), DateUtil.date().toString("yyyyMM"), calleeid.substring(0, 4));
        redisCacheService.incrementHash(redisKeyName, calleeid, 1, Integer.MAX_VALUE, expTime);
    }

//    private void userConcurrentIncrement(CustomerUserInfoQueryRespDTO userInfo) {
//        String redisKey = StrUtil.format("UserConcurrent:{}", userInfo.getUserNo());
//        redisCacheService.incrementAndGet(redisKey, Integer.MAX_VALUE, 0);
//    }
//
//
//    private void checkConcurrent(CustomerUserInfoQueryRespDTO userInfo) {
//        String redisKey = StrUtil.format("UserConcurrent:{}", userInfo.getUserNo());
//        long userConcurrent = redisCacheService.incrementAndGet(redisKey, Integer.MAX_VALUE);
//
//        CustomerBaseInfoQueryRespDTO customerInfo = dataService.getCustomerInfo(userInfo.getCustomerNo());
//        Integer warnNum = customerInfo.getWarnNum();
//        Integer avg = getUserCallAvg(userInfo.getUserNo());
//
//        PlatRateInfoDataBO rateInfo = rateService.getRateInfo(userInfo.getFeeGroupNo());
//        if (rateInfo == null) {
//            throw new BusinessException("未配置费率");
//        }
//
//        Integer billingCycle = rateInfo.getBillingCycle();
//        BigDecimal rate = rateInfo.getRate();
//
//        //计算最大并发数 warnNum / ((rate / billingCycle) * 60 * avg) =  (warnNum * billingCycle) / (60 * rate * avg)
//        BigDecimal m1 = mul(new BigDecimal(warnNum), new BigDecimal(billingCycle));
//        BigDecimal m2 = mul(new BigDecimal(60), new BigDecimal(avg), rate);
//        BigDecimal maxConcurrent = div(m1, m2);
//        BigDecimal currConcurrent = new BigDecimal(userConcurrent);
//        if (currConcurrent.compareTo(maxConcurrent) > 0) {
//            throw new BusinessException("超出预警最大并发");
//        }
//    }

    private Integer getUserCallAvg(String userNo) {
        Integer avg = getUserCallAvgMap().get(userNo);
        if (avg == null || avg <= 0) {
            Integer defAvg = dataService.getSysParamInt(SysParamEnum.VOS_SBC_USER_CALL_DEF_AVG);
            return defAvg;
        }
        return avg;
    }

    private Map<String, Integer> getUserCallAvgMap() {
        String strs = dataService.getSysParam(SysParamEnum.VOS_SBC_USER_CALL_AVG);
        if (StrUtil.isEmpty(strs)) {
            return new HashMap<>();
        }
        Map<String, Integer> map = new HashMap<>();
        for (String str : strs.split(",")) {
            String[] split = str.split(":");
            map.put(split[0], Integer.parseInt(split[1]));
        }
        return map;
    }



}
