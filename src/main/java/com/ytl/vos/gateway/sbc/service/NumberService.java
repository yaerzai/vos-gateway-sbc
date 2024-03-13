package com.ytl.vos.gateway.sbc.service;

import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.gateway.sbc.service.bo.OperatorsInfoBO;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;
import com.ytl.vos.persistence.dataservice.bo.ChannelNumberInfoDataBO;

import java.util.List;

/**
 * 号码服务
 */
public interface NumberService {

    /**
     * 号码黑名单检查
     * @param userDTO
     * @param callerNumber  主叫号码
     * @param calledNumber   被叫号码
     */
    void checkNumber(CustomerUserInfoQueryRespDTO userDTO, String callerNumber, String calledNumber);

    /**
     * 获取手机号信息
     * @param mobileNo
     * @return
     */
    OperatorsInfoBO getMobileInfo(String mobileNo);

    /**
     * 获取座机号信息
     * @param phone
     * @return
     */
    OperatorsInfoBO getPhoneInfo(String phone);

    /**
     * 检查否为空客户号码
     * @param callReqBO
     * @return
     */
    ChannelNumberInfoDataBO checkCustPrivateNumber(CustomerUserInfoQueryRespDTO userInfo, CallRequestDataBO callReqBO);

    /**
     * 通过省份、城市和并发数过滤
     * @param numberList
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByArea(List<ChannelNumberInfoDataBO> numberList, CallRequestDataBO callReqBO, Byte numberFitlerType);

    /**
     * 通过并发数过滤
     * @param numberList
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByConcurrency(List<ChannelNumberInfoDataBO> numberList);

    /**
     * 小号使用频率过滤
     * @param channelNo
     * @param numberList
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByFrequency(String channelNo, List<ChannelNumberInfoDataBO> numberList);

    /**
     * 过滤非启用状态
     * @param numberList
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByNotEnable(List<ChannelNumberInfoDataBO> numberList);

    /**
     * 通过发送类型过滤
     * @param numberList
     * @param sendType
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberBySendType(List<ChannelNumberInfoDataBO> numberList, String sendType);

    /**
     * 通过发送类型过滤
     * @param numberList
     * @param operatorType
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByOperatorType(List<ChannelNumberInfoDataBO> numberList, Byte operatorType);


    /**
     * 通过通道状态过滤
     * @param numberList
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByChannnelStatus(List<ChannelNumberInfoDataBO> numberList);

    /**
     * 过滤出最小费率
     * @param numberList
     * @return
     */
    List<ChannelNumberInfoDataBO> filterNumberByRate(List<ChannelNumberInfoDataBO> numberList);

    /**
     * 通过行业再过滤
     * @param numberList
     * @param userNo
     */
    List<ChannelNumberInfoDataBO> filterNumberByIndustry(List<ChannelNumberInfoDataBO> numberList, String userNo);

    /**
     * 检查小号频率是否超限
     * @param number
     * @return
     */
    boolean checkNumberFrequencyLimitOut(String channelNo, String number);

    /**
     * 小号使用频率增加
     * @param channelNo
     * @param number
     * @return
     */
    long numberFrequencyLimitAdd(String channelNo, String number);

    /**
     * 获取通道小号频率限制周期
     * @param channelNo
     * @return
     */
    int getChannelNumberFrequencyLimitCycle(String channelNo);

    /**
     * 获取通道小号频率限制次数
     * @param channelNo
     * @return
     */
    int getChannelNumberFrequencyLimitTimes(String channelNo);

}
