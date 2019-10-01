package com.shinysponge.dpscript.pawser.conditions;

import java.util.Collections;
import java.util.List;

public class BlockAreaCondition extends Condition {
    private final String pos;
    private final String end;
    private final String dest;
    private final String mode;

    public BlockAreaCondition(String pos, String end, String dest, String mode, boolean negate) {
        super(negate);
        this.pos = pos;
        this.end = end;
        this.dest = dest;
        this.mode = mode;
    }

    @Override
    public List<String> toCommands(String command) {
        return Collections.singletonList(negation() + " blocks " + pos + " " + end + " " + dest + " " + mode);
    }
}
