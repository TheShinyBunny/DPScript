package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.Value;

import java.util.Collections;
import java.util.List;

public class ScoreCondition extends Condition {

    private final Value first;
    private final String op;
    private final Value second;

    public ScoreCondition(Value first, String op, Value second) {
        this.first = first;
        this.op = op;
        this.second = second;
    }

    @Override
    public List<String> toCommands(Parser parser, String command) {
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
