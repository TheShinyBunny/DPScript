package com.shinysponge.dpscript.pawser.states;

import com.shinysponge.dpscript.pawser.ConditionHolder;
import com.shinysponge.dpscript.pawser.ConditionOption;
import com.shinysponge.dpscript.pawser.Conditionable;
import com.shinysponge.dpscript.pawser.score.EntryScore;

import java.util.List;

public class StatePropertyPredicate implements Conditionable {

    private String pos;
    private String blockId;
    private final BlockProperties property;

    public StatePropertyPredicate(String pos, String blockId, BlockProperties property) {
        this.pos = pos;
        this.blockId = blockId;
        this.property = property;
    }

    @Override
    public void storeValue(List<String> cmds, EntryScore dest, int tempCount) {
        for (int i = 0; i < property.getOptions().length; i++) {
            ConditionOption opt = property.getOption(i);
            cmds.add("execute if block " + pos + " " + blockId + "[" + property.getName() + "=" + opt.getName() + "] run scoreboard players set " + dest + " " + i);
        }
    }

    @Override
    public ConditionHolder getHolder() {
        return property;
    }
}
