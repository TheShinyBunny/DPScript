package com.shinysponge.dpscript.pawser;

public enum ErrorType {
    EXPECTED("Expected"),
    MISSING("Missing"),
    INVALID("Invalid"),
    DUPLICATE("Duplicate"),
    UNKNOWN("Unknown");

    private final String name;

    ErrorType(String s) {
        this.name = s;
    }

    public String getName() {
        return name;
    }
}
