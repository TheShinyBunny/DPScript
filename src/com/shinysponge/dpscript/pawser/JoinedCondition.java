package com.shinysponge.dpscript.pawser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JoinedCondition extends Condition {

    private Condition left;
    private Condition right;

    public JoinedCondition(String op, Condition left, Condition right) {
        super(null, op, null);
        this.left = left;
        this.right = right;
    }

    @Override
    public List<String> toCommands(Parser parser, String function) {
        if (op.equals("||")) {
            List<String> tempCmds = left.toCommands(parser,function);
            if (!(left instanceof JoinedCondition))
                tempCmds = tempCmds.stream().map(s->s + " run function " + function).collect(Collectors.toList());
            List<String> cmds = new ArrayList<>(tempCmds);
            tempCmds = right.toCommands(parser,function);
            if (!(right instanceof JoinedCondition))
                tempCmds = tempCmds.stream().map(s->s + " run function " + function).collect(Collectors.toList());
            cmds.addAll(tempCmds);
            return cmds;
        } else {
            List<String> leftCmds = left.toCommands(parser,function);
            List<String> rightCmds = right.toCommands(parser,function);
            if (rightCmds.size() > 1) {
                rightCmds = rightCmds.stream().map(s -> s + " run function " + function).collect(Collectors.toList());
                String name = parser.genrateFunction(rightCmds);
                leftCmds = leftCmds.stream().map(s -> s + " run function " + name).collect(Collectors.toList());
            } else {
                List<String> finalRightCmds = rightCmds;
                leftCmds = leftCmds.stream().map(s -> s + " " + finalRightCmds.get(0) + " run function " + function).collect(Collectors.toList());
            }
            return leftCmds;
        }
    }
}
