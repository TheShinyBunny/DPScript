package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.selector.Selector;

import java.util.function.Consumer;

public interface SelectorMember {

    String[] getIdentifiers();

    void parse(Selector selector, Consumer<String> commands);

}
