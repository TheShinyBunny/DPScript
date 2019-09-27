package com.shinysponge.dpscript.pawser;

public enum ErrorType {
    EXPECTED("Expected"),
    MISSING("Missing"),
    INVALID("Invalid"),
    DUPLICATE("Duplicate"),
    UNKNOWN("Unknown"),
    INVALID_STATEMENT("Invalid statement");

    private final String name;

    ErrorType(String s) {
        this.name = s;
    }

    public String getName() {
        return name;
    }
}
