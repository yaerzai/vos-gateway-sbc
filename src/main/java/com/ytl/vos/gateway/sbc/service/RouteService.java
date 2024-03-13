package com.ytl.vos.gateway.sbc.service;

import com.ytl.vos.gateway.sbc.dto.RouteReqDTO;
import com.ytl.vos.gateway.sbc.dto.RouteRespDTO;

/**
 * 路由服务
 */
public interface RouteService {

    /**
     * 测试方法
     * @param reqDTO
     * @return
     */
    RouteRespDTO route(RouteReqDTO reqDTO);
}
