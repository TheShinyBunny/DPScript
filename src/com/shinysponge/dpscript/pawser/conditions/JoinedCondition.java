package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JoinedCondition extends Condition {

    private String op;
    private Condition left;
    private Condition right;

    public JoinedCondition(String op, Condition left, Condition right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public List<String> toCommands(Parser parser, String command) {
        if (op.equals("||")) {
            List<String> tempCmds = left.toCommands(parser, command);
            if (!(left instanceof JoinedCondition))
                tempCmds = tempCmds.stream().map(s->s + " run " + command).collect(Collectors.toList());
            List<String> cmds = new ArrayList<>(tempCmds);
            tempCmds = right.toCommands(parser, command);
            if (!(right instanceof JoinedCondition))
                tempCmds = tempCmds.stream().map(s->s + " run " + command).collect(Collectors.toList());
            cmds.addAll(tempCmds);
            return cmds;
        } else {
            List<String> leftCmds = left.toCommands(parser, command);
            List<String> rightCmds = right.toCommands(parser, command);
            if (rightCmds.size() > 1) {
                String name = parser.generateFunction(rightCmds.stream().map(s->"execute " + s).collect(Collectors.toList()));
                leftCmds = leftCmds.stream().map(s -> s + " run function " + name).collect(Collectors.toList());
            } else {
                leftCmds = leftCmds.stream().map(s -> s + " " + rightCmds.get(0) + " run " + command).collect(Collectors.toList());
            }
            return leftCmds;
        }
    }
}