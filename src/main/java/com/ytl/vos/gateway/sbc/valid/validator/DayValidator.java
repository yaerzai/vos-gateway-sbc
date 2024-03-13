package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.Day;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.SimpleDateFormat;

public class DayValidator implements ConstraintValidator<Day, String> {

    String format = "yyyyMMdd";

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
