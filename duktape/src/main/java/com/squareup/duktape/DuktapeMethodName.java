package com.squareup.duktape;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DuktapeMethodName {
    String name() default "";
}