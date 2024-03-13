package com.ytl.vos.gateway.sbc.service;

import com.ytl.vos.channel.api.dto.ChannelInfoDTO;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailResDTO;
import com.ytl.vos.customer.api.dto.base.CustomerUserInfoDTO;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;

/**
 * 通道服务
 */
public interface ChannelService {

    /**
     * 通道路由
     * @param userDTO
     * @param callReqBO
     */
    ChannelInfoDetailResDTO checkRoute(CustomerUserInfoDTO userDTO, CallRequestDataBO callReqBO);

    void channelLimitIncrement(ChannelInfoDetailResDTO channelInfo, String calleeid);
}
