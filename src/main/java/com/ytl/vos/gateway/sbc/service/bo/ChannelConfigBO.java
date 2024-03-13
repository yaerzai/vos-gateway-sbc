package com.ytl.vos.gateway.sbc.service.bo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelConfigBO {

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 权重
     */
    private Integer weight;

}
