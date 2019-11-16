package com.shinysponge.dpscript.pawser.score;

import com.shinysponge.dpscript.pawser.ConditionHolder;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;

public class ScoreHolder implements ConditionHolder<Score> {

    public static final ScoreHolder INSTANCE = new ScoreHolder();

    @Override
    public String compareAndRun(Score option, EntryScore value, String command) {
        if (option instanceof LiteralScore) {
            return "execute if score " + value + " matches " + option + " run " + command;
        } else {
            return "execute if score " + value + " = " + option + " run " + command;
        }
    }

    @Override
    public Score parseOption() {
        TokenIterator tokens = Parser.tokens;
        if (tokens.skip("@")) {
            return SelectorParser.parseObjectiveSelector();
        }
        return Score.of(tokens.readLiteralInt());
    }
}
