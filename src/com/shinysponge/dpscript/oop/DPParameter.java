package com.shinysponge.dpscript.oop;

public class DPParameter {

    private String name;
    private AbstractClass type;
    private boolean optional;
    private Object defaultValue;
    private boolean varargs;

    public DPParameter(String name, AbstractClass type, boolean optional, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.defaultValue = defaultValue;
    }

    public AbstractClass getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isVarargs() {
        return varargs;
    }

    public DPParameter setVarargs() {
        this.varargs = true;
        this.optional = true;
        return this;
    }

    @Override
    public String toString() {
        return (type == null ? "object" : type.getName()) + " " + name + (varargs ? "..." : defaultValue == null ? optional ? "?" : "" : "=" + defaultValue);
    }
}
