package com.shinysponge.dpscript.tokenizew;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public enum TokenType {

    STRING("[\"'](.*)[\"']"),
    INT("-?\\d+"),
    DOUBLE("-?\\d+\\.?\\d+"),
    IDENTIFIER("[a-zA-Z$_][a-zA-Z$_0-9]*"),
    SYMBOL(s->s.length() == 1 && Tokenizer.isSymbol(s.charAt(0))),
    OPERATOR(Tokenizer::isOperator),
    LINE_END("\\n"),
    RAW_COMMAND(s->false);

    private Pattern pattern;
    private Predicate<String> test;

    TokenType(Predicate<String> test) {
        this.test = test;
    }

    TokenType(String pattern) {
        this.pattern = Pattern.compile("^(" + pattern + ")");
    }

    public boolean test(String s) {
        return (test == null ? pattern.matcher(s).matches() : test.test(s));
        /*if (this == SYMBOL && s.length() == 1) return Tokenizer.isSymbol(s.charAt(0)) ? s : null;
        if (this == OPERATOR) return Tokenizer.isOperator(s) ? s : null;
        if (this == KEYWORD) return Tokenizer.isKeyword(s) ? s : null;

        return pattern.matcher(s).matches() ? s : null;*/
    }

    public String remainder(String s) {
        return this == IDENTIFIER ? s.substring(s.length()-1).trim() : "";
    }


}
