package com.shinysponge.dpscript.pawser.conditions;

import java.util.Collections;
import java.util.List;

public class HasDataCondition extends Condition {
    private final String type;
    private final String selector;
    private final String path;

    public HasDataCondition(String type, String selector, String path, boolean negate) {
        super(negate);
        this.type = type;
        this.selector = selector;
        this.path = path;
    }

    @Override
    public List<String> toCommands(String command) {
        return Collections.singletonList(negation() + " data " + type + " " + selector + " " + path);
    }
}
