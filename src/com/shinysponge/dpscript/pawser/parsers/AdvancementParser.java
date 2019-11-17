package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.advancements.Advancement;
import com.shinysponge.dpscript.advancements.FrameType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

public class AdvancementParser {

    public static void parseAdvancement(String type, TokenIterator tokens) {
        FrameType frame = FrameType.TASK;
        if (!type.equalsIgnoreCase("advancement")) {
            tokens.skip("advancement");
            frame = FrameType.valueOf(type.toUpperCase());
        }
        String id = tokens.expect(TokenType.IDENTIFIER,"advancement name");
        Advancement advancement = new Advancement(Parser.getContext().getNamespace(),id).frame(frame);
        advancement.parseDeclaration(tokens);
        Parser.getContext().getNamespace().addItem(advancement);
    }



}
