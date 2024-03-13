package com.ytl.vos.gateway.sbc.enums;

import com.ytl.common.base.exception.ServerCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum VosErrCodeEnum implements ServerCode {

    System_Maintain("SY.0000", "SY.0000", "系统维护"),
    System_Error("SY.9999", "SY.9999", "系统未知错误"),

    Param_Validate_Error("PA.0001", "PA.0001", "参数格式校验错误"),
    Param_Invalid_Retrun("PA.0002", "PA.0002", "无效返回值"),
    System_Param_Config_Error("PA.0003", "PA.0003", "系统参数配置异常"),

    Customer_NotExists("CS.0001", "CS.0001", "客户不存在"),
    Customer_Stoped("CS.0002", "CS.0002", "客户已停用"),
    Customer_Balance_Less("CS.0003", "CS.0003", "客户余额不足"),
    Customer_User_NotExists("CS.0004", "CS.0004", "客户账号不存在"),
    Customer_User_Stoped("CS.0005", "CS.0005", "客户账号已停用"),
    Customer_MasterUser_NotExists("CS.0006", "CS.0006", "客户主账号不存在"),
    Customer_IpCheck_Error("CS.0007", "CS.0007", "IP地址不合法"),
    Customer_OutFlow("CS.0008", "CS.0008", "账号请求超流速"),
    Customer_OutDayLimit("CS.0009", "CS.0009", "超过账号日限"),
    Customer_OutMonthLimit("CS.0010", "CS.0010", "超过账号月限"),
    Customer_Fee_UnBind("CS.0011", "CS.0011", "未绑定费率"),
    Customer_Number_OutMaxConc("CS.0012", "CS.0012", "超过号码最大并发"),
    Customer_Fee_UnConfig("CS.0013", "CS.0013", "费率信息未配置"),
    Customer_NotInTimeFrame("CS.0014", "CS.0014", "禁呼时间段内呼叫"),
    Customer_Holidays_Disable("CS.0015", "CS.0015", "节假日不允许发送"),
    Customer_Frequency_Out("CS.0016", "CS.0016", "超出使用频率"),

    Number_Unsub_Black("MO.0001", "MO.0001", "退订黑名单"),
    Number_Complaint_Black("MO.0002", "MO.0002", "投诉黑名单"),
    Number_Third_Black("MO.0003", "MO.0003", "第三方黑名单"),
    Number_Not_Available("MO.0004", "MO.0004", "无可用号码"),
    Number_Province_No_PublicNumber("MO.0005", "MO.0005", "省份无公共号码"),
    Number_NotEnable("MO.0006", "MO.0006", "号码非启用状态"),
    Number_Call_Black("MO.0007", "MO.0007", "主叫黑名单号码"),
    Number_NotCustTestLimitMobile("MO.0008", "MO.0008", "非客户白名单号码"),

    DB_Insert_Error("DB.0001", "DB.0001", "数据库插入异常"),

    Channel_No_Use("CH.0001", "CH.0001", "无可用通道"),
    Channel_Disable("CH.0002", "CH.0002", "通道已停用"),
    Channel_No_PublicNumber("CH.0003", "CH.0003", "通道无公共号码"),
    Channel_Invalid("CH.0004", "CH.0004", "无效通道"),
    Channel_ConfigErr("CH.0005", "CH.0005", "通道发送类型配置错误"),
    ;

    /**
     * 操作代码
     */
    private final String code;

    /**
     * 对外错误码
     */
    private final String outCode;

    /**
     * 描述
     */
    private final String msg;

    /**
     * 比较
     * @param code
     * @return
     */
    private boolean eq(String code) {
        return this.code.equals(code);
    }

    /**
     * 解析
     * @param code
     * @return
     */
    public static VosErrCodeEnum parse(String code) {
        return Arrays.stream(values()).filter(item-> item.code.equals(code)).findAny().orElse(null);
    }

}
