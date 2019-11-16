package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Selector;

import java.util.Collections;
import java.util.List;

public class EntityExistsCondition extends Condition {
    private final Selector selector;

    public EntityExistsCondition(Selector selector, boolean negate) {
        super(negate);
        this.selector = selector;
    }

    @Override
    public List<String> toCommands(String command) {
        return Collections.singletonList(negation() + " entity " + selector);
    }
}
