package com.ytl.vos.gateway.sbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 退订级别
 * @author codescript.build
 */
@AllArgsConstructor
@Getter
public enum UnsubLevelEnum {

    CURSE(1, "骂人/粗口"),
    GENERAL(2, "普通退订"),
    CUST_REQUIRE(3, "客户要求");
    
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
    public static UnsubLevelEnum getDefault() {
        return GENERAL;
    }

    /**
     * 解析
     * @param codeId
     * @return
     */
    public static UnsubLevelEnum parse(Integer codeId) {
        if (codeId == null) {
            return getDefault();
        }
        return Arrays.stream(values()).filter(item->item.eq(codeId)).findAny().orElse(getDefault());
    }
}
