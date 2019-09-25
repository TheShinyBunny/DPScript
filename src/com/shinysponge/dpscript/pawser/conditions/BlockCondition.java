package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;

import java.util.Collections;
import java.util.List;

public class BlockCondition extends Condition {

    private String pos;
    private String block;

    public BlockCondition(String pos, String block) {
        this.pos = pos;
        this.block = block;
    }

    @Override
    public List<String> toCommands(Parser parser, String command) {
        return Collections.singletonList("if block " + pos + " " + block);
    }
}
