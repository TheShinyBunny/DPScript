package com.shinysponge.dpscript.tokenizew;

public class Token {

    public static final Token EOF = new Token(CodePos.END, TokenType.LINE_END, "end of file");
    private CodePos pos;
    private TokenType type;
    private final String value;

    public Token(CodePos pos, TokenType type, String value) {
        this.pos = pos;
        this.type = type;
        this.value = value;
    }

    public CodePos getPos() {
        return pos;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "(" + type + ": \"" + value + "\")";
    }
}
