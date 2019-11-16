package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.Selector;
import com.shinysponge.dpscript.tokenizew.TokenIterator;

public class LootParser {

    public static String parseLootSource() {
        if (!Parser.tokens.isNext("mining","fishing","killing","looting")) {
            return null;
        }
        String sourceName = Parser.tokens.expect("mining","fishing","killing","looting");
        for (Source s : Source.values()) {
            if (s.name.equals(sourceName)) {
                boolean par = Parser.tokens.skip("(");
                String src = s.name().toLowerCase() + " " + s.parseArguments(Parser.tokens);
                if (par) {
                    Parser.tokens.expect(')');
                }
                return src;
            }
        }
        return null;
    }

    public enum Source {
        FISH("fishing") {
            @Override
            public String parseArguments(TokenIterator tokens) {
                String lootTable = Parser.parseResourceLocation(false);
                String location = Parser.readPosition();
                String tool;
                if (tokens.isNext("mainhand","offhand")) {
                    tool = tokens.nextValue();
                } else {
                    tool = Parser.parseItem(false).toString();
                }
                return lootTable + " " + location + " " + tool;
            }
        },
        LOOT("looting") {
            @Override
            public String parseArguments(TokenIterator tokens) {
                return Parser.parseResourceLocation(false);
            }
        },
        KILL("killing") {
            @Override
            public String parseArguments(TokenIterator tokens) {
                tokens.expect('@');
                Selector selector = SelectorParser.parseAnySelector(false);
                return selector == null ? "@p" : selector.toString();
            }
        },
        MINE("mining") {
            @Override
            public String parseArguments(TokenIterator tokens) {
                String location = Parser.readPosition();
                String tool;
                if (tokens.isNext("mainhand","offhand")) {
                    tool = tokens.nextValue();
                } else {
                    tool = Parser.parseItem(false).toString();
                }
                return location + " " + tool;
            }
        };

        private String name;

        Source(String name) {
            this.name = name;
        }

        public abstract String parseArguments(TokenIterator tokens);
    }
}
