package com.ytl.vos.gateway.sbc.controller;

import com.alibaba.fastjson.JSONObject;
import com.ytl.vos.gateway.sbc.dto.RouteReqDTO;
import com.ytl.vos.gateway.sbc.util.HTTPClient;
import com.ytl.vos.gateway.sbc.util.RandomStringGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;




/**
 * @author kf-zhanghui
 * @date 2023/7/11 14:33
 */
@Slf4j
public class RouteControllerTest {

    /** test */
    //客户IP
    private String userNo = "392291";
    private String customerIp = "127.0.0.1";
    private String address = "http://192.168.0.188:8811";

    /**
     * 1. 主叫号码不加前缀UserNo测试
     */
    @Test
    public void test01() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     *客户账号关闭测试
     */
    @Test
    public void test02() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     *费率组---客户账号未配置费率组测试
     */
    @Test
    public void test03() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 添加了费率组，账号IP白名单---IP不在IP白名单中测试；
     */
    @Test
    public void test04() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 预付费余额---测试预付费账号余额为0的账号测试；
     */
    @Test
    public void test05() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户余额充值后，测试，
     */
    @Test
    public void test06() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid("13118241096");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户余额充值后，测试，
     */
    @Test
    public void test07() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid("13118241096");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户--退订黑名单检查，将号码添加到退订黑名单中,并设置账号为客户退订
     */
    @Test
    public void test08() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid("13118241096");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 账号--退订黑名单检查，将号码添加到退订黑名单中,并设置账号为账号退订
     * 15614009682
     */
    @Test
    public void test09() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid("13118241096");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 投诉黑名单检查，将号码添加到投诉黑名单中,设置为一级投诉，并设置账号为一级投诉
     *
     */
    @Test
    public void test10() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid("13118241096");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 第三方黑名单检查，设置账号为第三方黑名单标识
     *
     */
    @Test
    public void test11() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid("13118241096");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 号码状态检查---该号码状态为暂停或注销状态测试
     *
     */
    @Test
    public void test12() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + "15126555992");
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 私有号码的通道被删除，测试
     */
    @Test
    public void test13() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + "15126555992");
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 私有号码的通道正常，网关没有配置IP和端口测试
     */
    @Test
    public void test14() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + "15265383189");
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 私有号码的通道正常，通道配置了网关IP和端口测试
     */
    @Test
    public void test15() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + "15265383189");
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
        //{"result":true,"reason":"请求成功","record":0,"route_host":"192.168.0.161:25632","route_cid":"15602949569","route_rid":"15265383189"}
    }

    /**
     * 主叫号码非客户私有号码测试-------------------------------------------------------------------------------------------------------------------
     */

    /**
     * 客户账号配置的通道组，但是通道组没有配置通道
     */
    @Test
    public void test16() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户账号配置的通道组，该通道组配置了通道，但是该通道是关闭状态
     */
    @Test
    public void test17() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户账号配置的通道组，该通道组配置了一个通配全国的通道
     */
    @Test
    public void test18() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了一个只支持湖北省的通道
     */
    @Test
    public void test19() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //非湖北省
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户账号配置的通道组，该通道组配置了一个只支持湖北省的通道
     */
    @Test
    public void test20() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002569");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了一个只屏蔽湖北省的通道--使用非湖北省的手机号测试
     */
    @Test
    public void test21() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了一个只屏蔽湖北省的通道--使用湖北省的手机号测试
     */
    @Test
    public void test22() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002568");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }



    /**
     * 客户账号配置的通道组，该通道组配置了三个通道--但是三个通道都是关闭状态测试
     */
    @Test
    public void test23() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002568");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样，关闭通配全国和屏蔽省份配置，只打开支持省份配置测试
     */
    @Test
    public void test24() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002568");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样，关闭通配全国和屏蔽省份配置，只打开支持省份配置测试
     */
    @Test
    public void test25() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }



    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样，关闭通配全国和支持省份配置，只打开屏蔽省份配置测试
     */
    @Test
    public void test26() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002567");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样，关闭通配全国和支持省份配置，只打开屏蔽省份配置测试
     */
    @Test
    public void test27() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，多个通道检查---一个通配全国添加省份，一个支持省份配置，优先级、权重一样测试
     */
    @Test
    public void test28() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002566");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样，关闭通配全国和支持省份配置，只打开屏蔽省份配置测试
     */
    @Test
    public void test29() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872002565");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样，关闭通配全国和支持省份配置，只打开屏蔽省份配置测试
     */
    @Test
    public void test30() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }



    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级、权重一样
     */
    @Test
    public void test31() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级不一样，权重一样测试
     */
    @Test
    public void test32() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 客户账号配置的通道组，该通道组配置了三个通道优先级一样，权重不一样测试
     */
    @Test
    public void test33() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid(RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }



    /**
     * 通道组路由到的通道是语音小号类型，该通道没有没有配置公共号码池号码
     */
    @Test
    public void test34() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872001234");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 通道组路由到的通道是语音小号类型，该通道配置的一个号码为暂停状态
     */
    @Test
    public void test35() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("950318763");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 通道组路由到的通道是语音小号类型，配置一个城市不和被叫号码相同的号码到公共号码池
     */
    @Test
    public void test36() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872003753");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 通道组路由到的通道是语音小号类型，配置一个城市和省份都不和被叫号码相同的号码到公共号码池
     */
    @Test
    public void test37() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872003752");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 通道组路由到的通道是语音小号类型，配置三个和被叫号码相同城市的公共号码池中，查看最小费率
     */
    @Test
    public void test38() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872007461");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }



    /**
     * 通道组路由到的通道是语音小号类型，配置三个和被叫号码相同城市的公共号码池中，查看最小费率
     */
    @Test
    public void test39() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872007466");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 通道组路由到的通道是语音小号类型，配置三个公共号码，费率相同,呼叫次数不相同
     */
    @Test
    public void test40() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872007467");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }


    /**
     * 通道组路由到的通道是语音小号类型，配置三个公共号码，费率相同、呼叫次数相同,呼叫次数不相同--营销行业
     */
    @Test
    public void test41() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872007473");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }

    /**
     * 通道组路由到的通道是语音小号类型，配置三个公共号码，费率相同、呼叫次数相同,呼叫次数都是0 呼叫次数不相同--营销行业
     */
    @Test
    public void test42() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid(userNo + RandomStringGeneratorUtil.generateRandomPhoneNumber());
        //被叫号码
        //湖北省手机号
        routeReqDTO.setCalleeid("15872007476");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }





    /**
     * 预付费余额---测试预付费账号余额为0的账号测试；
     */
    @Test
    public void test100() {
        //参数组装
        RouteReqDTO routeReqDTO = new RouteReqDTO();
        //主叫号码
        routeReqDTO.setCallerid("374656" + "15872005214");
        //被叫号码
        routeReqDTO.setCalleeid("15971005212");
        //ip
        routeReqDTO.setFromip(customerIp);
        //话单ID
        routeReqDTO.setCallid(RandomStringGeneratorUtil.generateRandomString(20));
        String body = JSONObject.toJSONString(routeReqDTO);

        System.out.println("body:" + body);
        String post = HTTPClient.postJsonStream(address + "/api/v1/vos/route", body);
        System.out.println(post);
    }
}
