package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.LetterDigital;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class LetterDigitalValidator implements ConstraintValidator<LetterDigital, String> {

    Pattern pattern = Pattern.compile("^[A-Za-z0-9]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StrUtil.isEmpty(value) || pattern.matcher(value).matches();
    }
}
