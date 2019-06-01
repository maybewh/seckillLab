package com.admin.validator;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.FIELD,ElementType.ANNOTATION_TYPE,ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = {IsMobileValidator.class}
)
public @interface IsMobile {

    boolean required() default true; //默认不能为空

    String message() default "手机号码格式错误"; //校验不通过输出信息

    Class<?>[] groups() default {}; //？？ 这个验证什么？

    Class<? extends Payload>[] payload() default {}; //这个验证什么？
}
