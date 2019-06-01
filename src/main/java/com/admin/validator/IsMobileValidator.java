package com.admin.validator;

import com.admin.utils.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IsMobileValidator implements ConstraintValidator<IsMobile,String> {

    private  boolean required = false;

    @Override
    public void initialize(IsMobile isMobile) {
        required = isMobile.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (required){
                return ValidatorUtil.isMobile(value);
        }else {
              if (StringUtils.isEmpty(value)){
                  return true;
              }else {
                  return ValidatorUtil.isMobile(value);
              }
        }
    }
}
