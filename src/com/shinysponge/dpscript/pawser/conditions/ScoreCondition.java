package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.Value;

import java.util.Collections;
import java.util.List;

public class ScoreCondition extends Condition {

    private final Value first;
    private String op;
    private final Value second;

    public ScoreCondition(Value first, String op, Value second, boolean negate) {
        super(negate);
        this.first = first;
        this.op = op;
        this.second = second;
    }

    @Override
    public List<String> toCommands(String command) {
        if (first.isLiteral() && second.isLiteral()) Parser.compilationError(null,"Cannot compare two literal values!");
        if (first.isLiteral()) {
            return Collections.singletonList(negation() + " score " + second + " matches " + first.toRange(op));
        } else if (second.isLiteral()){
            return Collections.singletonList(negation() + " score " + first + " matches " + second.toRange(op));
        } else {
            return Collections.singletonList(negation() + " score " + first + " " + op + " " + second);
        }
    }

    @Override
    public void negate() {
        if (">".equals(op)) op = "<=";
        else if ("<".equals(op)) op = ">=";
        else if (">=".equals(op)) op = "<";
        else if ("<=".equals(op)) op = ">";
        else super.negate();
    }
}
