package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;

import java.util.Collections;
import java.util.List;

public class EntityExistsCondition extends Condition {
    private final String selector;

    public EntityExistsCondition(String selector) {
        this.selector = selector;
    }

    @Override
    public List<String> toCommands(Parser parser, String command) {
        return Collections.singletonList("if entity " + selector);
    }
}
