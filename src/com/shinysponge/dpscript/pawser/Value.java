package com.shinysponge.dpscript.pawser;

public class Value {

    private String str;
    private boolean literal;

    public Value(String str, boolean literal) {
        this.str = str;
        this.literal = literal;
    }

    public boolean isLiteral() {
        return literal;
    }

    public String getStr() {
        return str;
    }

    @Override
    public String toString() {
        return str;
    }

    public String toRange(String op) {
        if (literal) {
            int n = Integer.parseInt(str);
            if ("=".equals(op)) return str;
            switch (op) {
                case ">":
                    return (n + 1) + "..";
                case ">=":
                    return n + "..";
                case "<":
                    return ".." + (n - 1);
                case "<=":
                    return ".." + n;
            }
            return "InvalidRange";
        }
        return "NotLiteral";
    }
}
