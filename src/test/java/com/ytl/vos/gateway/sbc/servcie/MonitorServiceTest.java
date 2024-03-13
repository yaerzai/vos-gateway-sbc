package com.ytl.vos.gateway.sbc.servcie;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.gateway.sbc.constant.SysConstant;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.persistence.dataservice.bo.MonitorCustomerFlowDataBO;
import com.ytl.vos.persistence.mapper.PlatMonitorMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MonitorServiceTest {

    List<String> userList = ListUtil.of("702860","830576","374656","497978","629199","373230","291548");
    @Resource
    private DataService dataService;
    @Resource
    private PlatMonitorMapper platMonitorMapper;

    @Value("${spring.application.name}")
    private String appName;
    @Value("${server.nodeId}")
    private String nodeId;

    @Test
    public void test() {

        for (int i = 0; i < 100; i++) {
            List<MonitorCustomerFlowDataBO> list = new ArrayList<>();
            for (String userNo : userList) {
//                String userNo = RandomUtil.randomEle(userList);
                CustomerUserInfoQueryRespDTO userInfo = dataService.getCustomerUserInfo(userNo);
                list.add(MonitorCustomerFlowDataBO.builder()
                        .customerNo(userInfo.getCustomerNo())
                        .userNo(userNo)
                        .monitorSource(appName + "-" + nodeId)
                        .monitorTime(DateUtil.date())
                        .reqFlow(RandomUtil.randomInt(1000))
                        .routeFlow(RandomUtil.randomInt(800))
                        .build());
            }
            SysConstant.monitorExecutors.execute(()-> platMonitorMapper.batchInsertMonitorCustomerFlow(list));
            ThreadUtil.sleep(1000);
        }

    }
}
