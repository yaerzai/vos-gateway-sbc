package com.ytl.vos.gateway.sbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CumulativeTypeEnum {

    DAY("Day", "日"), MONTH("Month", "月"), ALL("All", "总累计");

    private String codeId;
    private String name;

}
