package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.Hex;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class HexValidator implements ConstraintValidator<Hex, String> {

    Pattern pattern = Pattern.compile("^[A-Fa-f0-9]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StrUtil.isEmpty(value) || pattern.matcher(value).matches();
    }
}
