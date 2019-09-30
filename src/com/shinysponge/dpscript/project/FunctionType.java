package com.shinysponge.dpscript.project;

public enum FunctionType {
    LOAD("load"), TICK("tick"), FUNCTION(null);

    private final String tag;

    FunctionType(String tag) {
        this.tag = tag;
    }
}
