package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.old_tokenizer_which_is_bad.TokenBad;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ParserMethod {

    private Method method;
    private int priority;

    public ParserMethod(Method method, int priority) {
        this.method = method;
        this.priority = priority;
    }

    public Method getMethod() {
        return method;
    }

    public int getPriority() {
        return priority;
    }

    public TokenBad invoke(TokenBad[] tokens) {
        try {
            return (TokenBad) method.invoke(null, (Object) tokens);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
