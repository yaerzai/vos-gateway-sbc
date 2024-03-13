package com.ytl.vos.gateway.sbc.valid;

import com.ytl.vos.gateway.sbc.valid.validator.IpsValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = IpsValidator.class)
public @interface Ips {

    String message() default "必须为IP地址";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default { };
}
