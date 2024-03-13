package com.ytl.vos.gateway.sbc.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@ApiModel("路由请求")
@Data
public class RouteReqDTO {

    private static final long serialVersionUID = 6282663763433603548L;

    /**
     * 客户送过来的主叫号码
     */
    @ApiModelProperty("主叫号码")
    @NotBlank
    @Length(min = 6, max = 32)
    private String callerid;

    /**
     * 客户送过来的被叫号码
     */
    @ApiModelProperty("被叫号码")
    @NotBlank
    @Length(max = 32)
    private String calleeid;

    /**
     * 客户送过来的ip
     */
    @ApiModelProperty("ip")
    @NotBlank
    private String fromip;

    /**
     * Callid 唯一
     */
    @ApiModelProperty("callid")
    @NotBlank
    @Length(max = 128)
    private String callid;
}
