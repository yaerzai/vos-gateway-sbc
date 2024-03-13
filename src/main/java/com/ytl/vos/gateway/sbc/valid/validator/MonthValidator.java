package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.Month;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.SimpleDateFormat;

public class MonthValidator implements ConstraintValidator<Month, String> {

    String format = "yyyyMM";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StrUtil.isEmpty(value) || isDatetime(value, format);
    }

    public static boolean isDatetime(String value, String format){
        try{
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false);
            sdf.parse(value);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
