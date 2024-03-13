package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.Digital;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class DigitalValidator implements ConstraintValidator<Digital, String> {

    Pattern pattern = Pattern.compile("^[0-9]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StrUtil.isEmpty(value) || pattern.matcher(value).matches();
    }
}
