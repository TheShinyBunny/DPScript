package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.Parser;

import java.util.function.Consumer;

public interface SelectorMember {

    String[] getIdentifiers();

    void parse(String selector, Parser parser, Consumer<String> commands);

}
