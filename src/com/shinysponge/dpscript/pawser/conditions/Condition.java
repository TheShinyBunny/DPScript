package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;
import java.util.List;

public abstract class Condition {

    protected boolean negate;

    public Condition(boolean negate) {
        this.negate = negate;
    }

    public abstract List<String> toCommands(Parser parser, String command);

    protected String negation() {
        return negate ? "unless" : "if";
    }

    public void negate() {
        negate = !negate;
    }
}
