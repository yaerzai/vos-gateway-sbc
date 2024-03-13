package com.ytl.vos.gateway.sbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 号码池类型
 * @author codescript.build
 */
@AllArgsConstructor
@Getter
public enum NumberPoolTypeEnum {

    Share(0, "共享"),
    Exclusive(1, "独享"),
    CallExclusive(2, "主叫独享"),
 ;
    
    private int codeId;
    private String codeName;
    
    /**
     * 比较
     * @param codeId
     * @return
     */
    public boolean eq(Integer codeId) {
        return codeId == null ? isDefault() : this.codeId == codeId;
    }

    /**
     * 是否为默认
     * @return
     */
    public boolean isDefault() {
        return this == getDefault();
    }

    /**
     * 获取默认
     * @return
     */
    public static NumberPoolTypeEnum getDefault() {
        return Share;
    }

    /**
     * 解析
     * @param codeId
     * @return
     */
    public static NumberPoolTypeEnum parse(Integer codeId) {
        if (codeId == null) {
            return getDefault();
        }
        return Arrays.stream(values()).filter(item->item.eq(codeId)).findAny().orElse(getDefault());
    }
}
