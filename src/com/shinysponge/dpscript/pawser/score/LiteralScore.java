package com.shinysponge.dpscript.pawser.score;

public class LiteralScore extends Score {

    private int value;

    public LiteralScore(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public String toAssignCommand(EntryScore dest) {
        return "scoreboard players set " + dest + " " + value;
    }

    public String toRange(String op) {
        if ("=".equals(op)) return toString();
        switch (op) {
            case ">":
                return (value + 1) + "..";
            case ">=":
                return value + "..";
            case "<":
                return ".." + (value - 1);
            case "<=":
                return ".." + value;
        }
        return "InvalidRange";
    }
}
