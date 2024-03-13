package com.ytl.vos.gateway.sbc.valid.validator;

import cn.hutool.core.util.StrUtil;
import com.ytl.vos.gateway.sbc.valid.OrderBy;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class OrderByValidator implements ConstraintValidator<OrderBy, String> {

    Pattern pattern = Pattern.compile("\\s*(order\\s+by\\s+)?(([a-z_$]\\w*\\.)*[a-z_$]\\w*(\\s+(asc|desc))?\\s*,\\s*)*([a-z_$]\\w*\\.)*[a-z_$]\\w*(\\s+(asc|desc))?\\s*");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StrUtil.isEmpty(value) || pattern.matcher(value).matches();
    }
}
