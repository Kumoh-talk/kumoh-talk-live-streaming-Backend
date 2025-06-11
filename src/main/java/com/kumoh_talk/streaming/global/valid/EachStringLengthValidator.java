package com.kumoh_talk.streaming.global.valid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class EachStringLengthValidator implements ConstraintValidator<EachStringLength, List<String>> {
    private int min;
    private int max;

    @Override
    public void initialize(EachStringLength constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if (value == null) return true;

        return value.stream()
                .allMatch(s -> s != null && s.length() >= min && s.length() <= max);
    }
}
