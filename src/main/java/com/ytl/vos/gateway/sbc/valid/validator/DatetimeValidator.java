package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.DateTime;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.SimpleDateFormat;

public class DatetimeValidator implements ConstraintValidator<DateTime, String> {

    String format;

    @Override
    public void initialize(DateTime constraintAnnotation) {
        this.format = constraintAnnotation.format();
    }

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
