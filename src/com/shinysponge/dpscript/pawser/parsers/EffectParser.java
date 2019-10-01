package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenType;

public class EffectParser {

    public static Effect parseEffect(Parser parser) {
        String effect = parser.parseResourceLocation(false);
        int tier = -1;
        if (parser.tokens.isNext(TokenType.IDENTIFIER)) {
            tier = parser.readRomanNumber(parser.tokens.nextValue()) - 1;
        } else if (parser.tokens.isNext(TokenType.INT)) {
            tier = Integer.parseInt(parser.tokens.nextValue());
        }
        long seconds = -1;
        if (parser.tokens.skip(",")) {
            seconds = parser.parseDuration().getSeconds();
        }
        parser.tokens.skip(",");
        boolean hide = parser.tokens.skip("hide");
        return new Effect(effect,tier,seconds,hide);
    }

    public static class Effect {

        public final String id;
        public int tier;
        public long seconds;
        public boolean hide;

        public Effect(String id, int tier, long seconds, boolean hide) {
            this.id = id;
            this.tier = tier;
            this.seconds = seconds;
            this.hide = hide;
        }

        public boolean isDefault() {
            return seconds < 0 && tier < 0 && !hide;
        }

        public void defaultDurationAndTier() {
            if (seconds < 0) seconds = 30;
            if (tier < 0) tier = 0;
        }

        @Override
        public String toString() {
            return id + " " + seconds + " " + tier + " " + hide;
        }
    }

}
