package com.shinysponge.dpscript.tokenizew;

public enum TokenType {

    STRING(""),
    INT(0),
    DOUBLE(0.0),
    IDENTIFIER("identifier"),
    SYMBOL("~"),
    OPERATOR("+"),
    LINE_END("\n"),
    RAW_COMMAND("say Error"),
    DUMMY("");

    private Object def;

    TokenType(Object def) {
        this.def = def;
    }


    public Object getDefault() {
        return def;
    }
}
