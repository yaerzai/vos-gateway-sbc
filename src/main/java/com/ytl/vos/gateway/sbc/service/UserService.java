package com.ytl.vos.gateway.sbc.service;

import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;

/**
 * 账号服务
 */
public interface UserService {


    /**
     * 检查客户账号
     * @param callReqBO
     * @param fromip
     * @return
     */
    CustomerUserInfoQueryRespDTO checkUser(CallRequestDataBO callReqBO, String fromip);

    void userLimitIncrement(CustomerUserInfoQueryRespDTO userInfo, String calleeid);

    boolean isRecord(CustomerUserInfoQueryRespDTO userInfo);
}
