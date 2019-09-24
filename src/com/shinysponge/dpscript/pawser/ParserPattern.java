package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.old_tokenizer_which_is_bad.TokenTypeBad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParserPattern {

    TokenTypeBad[] value();
    int priority() default 0;

}
