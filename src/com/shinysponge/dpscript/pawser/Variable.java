package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.oop.LazyValue;

public class Variable {

    private VariableType type;
    private LazyValue<?> value;

    public Variable(VariableType type, LazyValue<?> value) {
        this.type = type;
        this.value = value;
    }

    public Variable(VariableType type, Object literal) {
        this(type,LazyValue.literal(literal));
    }

    public <T> T get(Class<T> type) {
        return type.cast(value.eval());
    }

    public LazyValue<?> getLazyValue() {
        return value;
    }

    public VariableType getType() {
        return type;
    }

    public Object get() {
        return value.eval();
    }
}
