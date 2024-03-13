package com.ytl.vos.gateway.sbc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("路由请求")
@Data
public class RouteRespDTO {

    private static final long serialVersionUID = 6282663763433603548L;

    /**
     * 路由请求结果
     */
    @ApiModelProperty("请求结果")
    private Boolean result;

    /**
     * 路由请求失败原因
     */
    @ApiModelProperty("失败原因")
    private String reason;

    /**
     * 是否录音标识，1录音，0不录音
     */
    @ApiModelProperty("是否录音")
    private Integer record;

    /**
     * 路由后的ip地址——运营商的ip
     */
    @ApiModelProperty("路由ip")
    @JsonProperty("route_host")
    private String routeHost;

    /**
     * 路由后的ip地址——运营商的端口
     */
    @ApiModelProperty("路由端口")
    @JsonProperty("route_port")
    private String routePort;

    /**
     * 送给运营商的被叫号码
     */
    @ApiModelProperty("被叫号码")
    @JsonProperty("route_cid")
    private String routeCid;

    /**
     * 送给运营商的主叫号码
     */
    @ApiModelProperty("主叫号码")
    @JsonProperty("route_rid")
    private String routeRid;
}
