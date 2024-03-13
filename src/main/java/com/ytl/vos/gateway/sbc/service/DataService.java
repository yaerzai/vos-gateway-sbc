package com.ytl.vos.gateway.sbc.service;

import com.ytl.vos.channel.api.dto.ChannelGroupChannelConfigRespDTO;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailResDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerBaseInfoQueryRespDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.persistence.enums.SysParamEnum;

import java.util.List;

public interface DataService {

    /**
     * 获取系统参数
     * @param sysParamEnum
     * @return
     */
    String getSysParam(SysParamEnum sysParamEnum);

    /**
     * 获取系统参数int
     * @param sysParamEnum
     * @return
     */
    int getSysParamInt(SysParamEnum sysParamEnum);

    /**
     * 获取客户信息
     * @param customerNo
     * @return
     */
    CustomerBaseInfoQueryRespDTO getCustomerInfo(String customerNo);

    /**
     * 获取客户账号
     * @param userNo
     * @return
     */
    CustomerUserInfoQueryRespDTO getCustomerUserInfo(String userNo);

    /**
     * 获取
     * @param customerNo
     * @return
     */
    CustomerUserInfoQueryRespDTO getCustomerMasterUserInfo(String customerNo);

    /**
     * 获取通道组配置
     * @param groupNo
     * @return
     */
    List<ChannelGroupChannelConfigRespDTO> getChannelGroupChannelConfig(String groupNo);

    /**
     * 获取通道信息
     * @param channelNo
     * @return
     */
    ChannelInfoDetailResDTO getChannelInfo(String channelNo);
}
