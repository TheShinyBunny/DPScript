package com.shinysponge.dpscript.pawser.conditions;

import java.util.Collections;
import java.util.List;

public class BlockCondition extends Condition {

    private String pos;
    private String block;

    public BlockCondition(String pos, String block, boolean negate) {
        super(negate);
        this.pos = pos;
        this.block = block;
    }

    @Override
    public List<String> toCommands(String command) {
        return Collections.singletonList(negation() + " block " + pos + " " + block);
    }
}
