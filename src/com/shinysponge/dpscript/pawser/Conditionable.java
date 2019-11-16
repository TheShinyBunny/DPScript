package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.score.EntryScore;
import com.shinysponge.dpscript.pawser.score.LazyScoreValue;
import com.shinysponge.dpscript.pawser.states.BlockProperties;
import com.shinysponge.dpscript.pawser.states.StatePropertyPredicate;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.List;

public interface Conditionable {

    void storeValue(List<String> cmds, EntryScore dest, int tempCount);

    ConditionHolder getHolder();

    static Conditionable parse(TokenIterator tokens) {
        if (tokens.skip("blockprop","property")) {
            String block = Parser.parseResourceLocation(true);
            tokens.expect('[');
            String prop = tokens.expect(TokenType.IDENTIFIER,"property key");
            tokens.expect(']');
            String pos = Parser.readPosition();
            return new StatePropertyPredicate(pos,block, BlockProperties.get(prop));
        }
        return LazyScoreValue.parseExpression();
    }

}
