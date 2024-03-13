package com.ytl.vos.gateway.sbc.valid;

import com.ytl.vos.gateway.sbc.valid.validator.LetterDigitalValidator;

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
@Constraint(validatedBy = LetterDigitalValidator.class)
public @interface LetterDigital {

    String message() default "必须为字母数字格式";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default { };
}
