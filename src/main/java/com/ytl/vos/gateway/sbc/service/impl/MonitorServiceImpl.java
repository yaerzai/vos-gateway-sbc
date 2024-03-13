package com.ytl.vos.gateway.sbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.ytl.common.base.controller.SystemController;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.gateway.sbc.constant.SysConstant;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.MonitorService;
import com.ytl.vos.persistence.dataservice.bo.MonitorCustomerFlowDataBO;
import com.ytl.vos.persistence.mapper.PlatMonitorMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MonitorServiceImpl implements MonitorService {

    @Resource
    private PlatMonitorMapper platMonitorMapper;
    @Resource
    private DataService dataService;

    @Value("${spring.application.name}")
    private String appName;
    @Value("${server.nodeId}")
    private String nodeId;

    private Map<String, AtomicInteger> reqMonitor = new ConcurrentHashMap<>();

    private Map<String, AtomicInteger> routeMonitor = new ConcurrentHashMap<>();


    @Override
    public void incrCustReq(String userNo) {
        AtomicInteger counter = reqMonitor.computeIfAbsent(userNo, key -> new AtomicInteger());
        counter.incrementAndGet();
    }

    @Override
    public void incrCustRoute(String userNo) {
        AtomicInteger counter = routeMonitor.computeIfAbsent(userNo, key -> new AtomicInteger());
        counter.incrementAndGet();
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void monitor() {
        if (!SystemController.getSystemOpenFlag()) {
            return;
        }
        Set<String> userNos1 = reqMonitor.keySet();
        Set<String> userNos2 = routeMonitor.keySet();
        if (CollUtil.isEmpty(userNos1) && CollUtil.isEmpty(userNos2)) {
            return;
        }
        Set<String> userNos = new HashSet<>();
        userNos.addAll(userNos1);
        userNos.addAll(userNos2);

        List<MonitorCustomerFlowDataBO> list = new ArrayList<>();
        for (String userNo : userNos) {
            AtomicInteger userReqCount = reqMonitor.get(userNo);
            AtomicInteger userRouteCount = routeMonitor.get(userNo);

            CustomerUserInfoQueryRespDTO userInfo = dataService.getCustomerUserInfo(userNo);
            if (userInfo == null) {
                continue;
            }

            int reqFlow = 0;
            int routeFlow = 0;

            if (userReqCount != null) {
                reqFlow = userReqCount.getAndSet(0);
            }
            if (userRouteCount != null) {
                routeFlow = userRouteCount.getAndSet(0);
            }

            if (reqFlow == 0 && routeFlow == 0) {
                continue;
            }

            list.add(MonitorCustomerFlowDataBO.builder()
                    .customerNo(userInfo.getCustomerNo())
                    .userNo(userNo)
                    .monitorSource(appName + "-" + nodeId)
                    .monitorTime(DateUtil.date())
                    .reqFlow(reqFlow)
                    .routeFlow(routeFlow)
                    .build());
        }
        if (CollUtil.isEmpty(list)) {
            return;
        }
        SysConstant.monitorExecutors.execute(()-> platMonitorMapper.batchInsertMonitorCustomerFlow(list));
    }


}
