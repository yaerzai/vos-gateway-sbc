package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.LetterDigitalSymbol;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class LetterDigitalSymbolValidator implements ConstraintValidator<LetterDigitalSymbol, String> {

    Pattern pattern = Pattern.compile("^[A-Za-z0-9~!@%^&*_+`=#()$/\\-\\.\\s{}\\[\\]\\|\\\\:;'\"<>,./\\?]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StrUtil.isEmpty(value) || pattern.matcher(value).matches();
    }
}
