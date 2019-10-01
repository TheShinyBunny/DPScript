package com.shinysponge.dpscript.pawser.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Condition {

    public static final Condition DUMMY = new Condition(false) {
        @Override
        public List<String> toCommands(String command) {
            return new ArrayList<>();
        }
    };
    protected boolean negate;

    public Condition(boolean negate) {
        this.negate = negate;
    }

    public abstract List<String> toCommands(String command);

    protected String negation() {
        return negate ? "unless" : "if";
    }

    public void negate() {
        negate = !negate;
    }

    public final List<String> toCommandsAll(String command) {
        return toCommands(command).stream().map(c->"execute " + c + (this instanceof JoinedCondition ? "" : " run " + command)).collect(Collectors.toList());
    }
}
