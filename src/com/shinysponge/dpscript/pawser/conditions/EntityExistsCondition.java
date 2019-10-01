package com.shinysponge.dpscript.pawser.conditions;

import java.util.Collections;
import java.util.List;

public class EntityExistsCondition extends Condition {
    private final String selector;

    public EntityExistsCondition(String selector, boolean negate) {
        super(negate);
        this.selector = selector;
    }

    @Override
    public List<String> toCommands(String command) {
        return Collections.singletonList(negation() + " entity " + selector);
    }
}
