package com.shinysponge.dpscript.pawser.score;

import com.shinysponge.dpscript.pawser.ConditionOption;

public abstract class Score implements ConditionOption {


    public static EntryScore global(String name) {
        return new EntryScore("Global",name);
    }

    public static EntryScore constant(String name) {
        return new EntryScore("Consts",name);
    }

    public static LiteralScore of(int value) {
        return new LiteralScore(value);
    }

    @Override
    public abstract String toString();

    @Override
    public String getName() {
        return toString();
    }

    public abstract String toAssignCommand(EntryScore dest);
}
