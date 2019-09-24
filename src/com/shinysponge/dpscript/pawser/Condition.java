package com.shinysponge.dpscript.pawser;

import java.util.Collections;
import java.util.List;

public class Condition {
    private final Value first;
    protected final String op;
    private final Value second;

    public Condition(Value first, String op, Value second) {
        this.first = first;
        this.op = op;
        this.second = second;
    }

    public List<String> toCommands(Parser parser, String function) {
        if (first.isLiteral() && second.isLiteral()) throw new RuntimeException("Cannot compare two literal values!");
        if (first.isLiteral()) {
            return Collections.singletonList("if score " + second + " matches " + first.toRange(op));
        } else if (second.isLiteral()){
            return Collections.singletonList("if score " + first + " matches " + second.toRange(op));
        } else {
            return Collections.singletonList("if score " + first + " " + op + " " + second);
        }
    }
}
