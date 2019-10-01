package com.shinysponge.dpscript.pawser.parsers;

import java.util.function.Consumer;

public interface SelectorMember {

    String[] getIdentifiers();

    void parse(String selector, Consumer<String> commands);

}
