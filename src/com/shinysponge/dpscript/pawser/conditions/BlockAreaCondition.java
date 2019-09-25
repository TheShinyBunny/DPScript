package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;

import java.util.Collections;
import java.util.List;

public class BlockAreaCondition extends Condition {
    private final String pos;
    private final String end;
    private final String dest;
    private final String mode;

    public BlockAreaCondition(String pos, String end, String dest, String mode) {

        this.pos = pos;
        this.end = end;
        this.dest = dest;
        this.mode = mode;
    }

    @Override
    public List<String> toCommands(Parser parser, String function) {
        return Collections.singletonList("if blocks " + pos + " " + end + " " + dest + " " + mode);
    }
}
