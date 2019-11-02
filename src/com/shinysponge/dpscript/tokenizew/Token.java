package com.shinysponge.dpscript.tokenizew;

import com.shinybunny.utils.MathUtils;

import java.io.File;

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

    public boolean isPositionInside(File file, int pos) {
        return this.pos.getFile().getFile().sameAs(file) && MathUtils.inRange(pos,this.pos.getPos(),this.pos.getPos() + this.value.length());
    }
}
