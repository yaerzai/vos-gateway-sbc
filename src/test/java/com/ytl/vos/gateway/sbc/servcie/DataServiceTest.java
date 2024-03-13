package com.ytl.vos.gateway.sbc.servcie;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.javatlacati.contiperf.PerfTest;
import com.github.javatlacati.contiperf.junit.ContiPerfRule;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.impl.DataServiceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DataServiceTest {

    private DataService dataService = new DataServiceImpl();

    @Rule
    public ContiPerfRule contiPerfRule = new ContiPerfRule();

    List<String> userList = ListUtil.of("702860","830576","374656","497978","629199","373230","291548");

    @Test
    @PerfTest(threads = 200, duration = 20_000)
    public void getCustomerUserInfo() {
        String userNo = RandomUtil.randomEle(userList);
//        dataService.getCustomerUserInfo(userNo);
//        int i = 0;
    }
}
