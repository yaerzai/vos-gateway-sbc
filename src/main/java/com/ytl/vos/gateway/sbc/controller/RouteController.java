package com.ytl.vos.gateway.sbc.controller;

import com.ytl.vos.gateway.sbc.dto.RouteReqDTO;
import com.ytl.vos.gateway.sbc.dto.RouteRespDTO;
import com.ytl.vos.gateway.sbc.service.RouteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;


@RestController
@RequestMapping("/api/v1/vos")
public class RouteController {

    @Resource
    private RouteService routeService;

    @PostMapping("/route")
    public RouteRespDTO route(@Valid @RequestBody RouteReqDTO reqDTO) {
        return routeService.route(reqDTO);
    }

}
