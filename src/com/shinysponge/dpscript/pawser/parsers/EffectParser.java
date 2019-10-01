package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.entities.Key;
import com.shinysponge.dpscript.entities.NBT;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenType;

public class EffectParser {

    public static Effect parseEffect() {
        String effect = Parser.parseResourceLocation(false);
        int tier = -1;
        if (Parser.tokens.isNext(TokenType.IDENTIFIER)) {
            tier = Parser.readRomanNumber(Parser.tokens.nextValue()) - 1;
        } else if (Parser.tokens.isNext(TokenType.INT)) {
            tier = Integer.parseInt(Parser.tokens.nextValue());
        }
        long seconds = -1;
        if (Parser.tokens.skip(",")) {
            seconds = Parser.parseDuration().getSeconds();
        }
        Parser.tokens.skip(",");
        boolean hide = Parser.tokens.skip("hide");
        return new Effect(effect,tier,seconds,hide);
    }

    public static class Effect {

        @Key("Id")
        public final String id;
        @Key("Amplifier")
        public int tier;
        @Key("Duration")
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

        public NBT toNBT() {
            return new NBT(this).set("ShowParticles",!hide);
        }
    }

}
