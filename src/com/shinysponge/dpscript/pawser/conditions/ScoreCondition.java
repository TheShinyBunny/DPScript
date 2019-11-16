package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.score.LiteralScore;
import com.shinysponge.dpscript.pawser.score.Score;

import java.util.Collections;
import java.util.List;

public class ScoreCondition extends Condition {

    private final Score first;
    private String op;
    private final Score second;

    public ScoreCondition(Score first, String op, Score second, boolean negate) {
        super(negate);
        this.first = first;
        this.op = op;
        this.second = second;
    }

    @Override
    public List<String> toCommands(String command) {
        if (first instanceof LiteralScore && second instanceof LiteralScore) Parser.compilationError(null,"Cannot compare two literal values!");
        if (first instanceof LiteralScore) {
            return Collections.singletonList(negation() + " score " + second + " matches " + ((LiteralScore) first).toRange(op));
        } else if (second instanceof LiteralScore) {
            return Collections.singletonList(negation() + " score " + first + " matches " + ((LiteralScore) second).toRange(op));
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
