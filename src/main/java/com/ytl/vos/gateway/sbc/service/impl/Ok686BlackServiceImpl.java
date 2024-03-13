package com.ytl.vos.gateway.sbc.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.ytl.vos.gateway.sbc.constant.SysConstant;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.ThirdBlackService;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service("ThirdBlackOk686")
@Slf4j
public class Ok686BlackServiceImpl implements ThirdBlackService {

    @Resource
    private DataService dataService;

    @Override
    public boolean checkBlack(String calleeId) {
        // TODO 第三方黑名单需要优化性能  ,记录调用次数
        
        long startTime = System.currentTimeMillis();
        String url = dataService.getSysParam(SysParamEnum.RISK_CHECK_THIRD_URL);
        String user = dataService.getSysParam(SysParamEnum.RISK_CHECK_THIRD_USER);
        String pwd = dataService.getSysParam(SysParamEnum.RISK_CHECK_THIRD_PASSWORD);
        if (StringUtils.isBlank(url) || StringUtils.isBlank(user) || StringUtils.isBlank(pwd)) {
            return false;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("pwd", pwd);
        map.put("mobile", calleeId);
        String requestBody = JSON.toJSONString(map);
        // request: {"user":"xx","pwd":"xx","mobile":"15099904992"}
        // response :
        // {
        //  "code": "0",
        //  "msg": "OK",
        //  "mobile": "18320929905",
        //  "result": "9"
        //}
        try {
            String ret = HttpUtil.post(url, requestBody, 400);
            String result = JSON.parseObject(ret).get("result").toString();
            if (StringUtils.isBlank(result)) {
                log.warn("[Ok686]第三方黑名单返回结果为空{} [{}] {}", requestBody, ret, System.currentTimeMillis() - startTime);
                return false;
            }
            int resultInt = Integer.parseInt(result);
            if (resultInt > SysConstant.THIRD_BLACK_RESULT) {
                log.warn("[Ok686]命中第三方黑名单[{}] {} {}", calleeId, ret, System.currentTimeMillis() - startTime);
                return true;
            }
            log.info("[Ok686]未命中第三方黑名单返回结果为{} [{}] {} ms", requestBody, ret, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[Ok686]第三方黑名单调用异常{} [{}] {} ms", requestBody, e, System.currentTimeMillis() - startTime);
        }
        return false;

    }
}
