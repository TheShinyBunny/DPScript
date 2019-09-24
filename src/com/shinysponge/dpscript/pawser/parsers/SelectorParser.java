package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SelectorParser {

    private Parser parser;
    private TokenIterator tokens;

    private List<SelectorMember> selectorMembers = new ArrayList<>();

    public SelectorParser(Parser parser) {
        this.parser = parser;
        this.tokens = parser.tokens;

        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String effect = parser.parseResourceLocation(false);
            int tier = -1;
            if (tokens.isNext(TokenType.IDENTIFIER)) {
                tier = parser.readRomanNumber(tokens.nextValue()) - 1;
            } else if (tokens.isNext(TokenType.INT)) {
                tier = Integer.parseInt(tokens.nextValue());
            }
            long seconds = -1;
            if (tokens.skip(",")) {
                seconds = parser.parseDuration().getSeconds();
            }
            tokens.expect(')');
            boolean hide = tokens.skip("hide");
            if (!hide && seconds < 0 && tier < 0 && tokens.skip("clear","remove","cure")) {
                cmds.accept("effect clear " + selector + " " + effect);
            } else if (seconds > 0 || hide || tier >= 0) {
                if (tier < 0) tier = 0;
                if (seconds < 0) seconds = 30;
                cmds.accept("effect give " + selector + " " + effect + " " + seconds + " " + tier + " " + hide);
            } else {
                cmds.accept("effect give " + selector + " " + effect);
            }
        },"effect");
        addSelectorMember((selector, cmds) -> {
            String command = tokens.previous();
            tokens.expect('(');
            String method = "only";
            boolean addCriterion = true;
            if (tokens.skip("all","everything")) {
                tokens.expect(')');
                cmds.accept("advancement " + command + " " + selector + " everything");
                return;
            }
            if (tokens.isNext("from","until")) {
                method = tokens.expect("from","until");
                addCriterion = false;
            } else {
                tokens.skip("only");
            }
            String adv = parser.parseResourceLocation(false);

            String criterion = "";
            if (addCriterion && tokens.skip("[")) {
                criterion = tokens.next(TokenType.IDENTIFIER);
                tokens.expect(']');
            }
            tokens.expect(')');
            cmds.accept("advancement " + command + " " + selector + " " + method + " " + adv + " " + criterion);
        },"grant","revoke");
        addSelectorMember((selector, cmds)->{
            tokens.expect('(');
            if (tokens.isNext(")")) {
                cmds.accept("clear " + selector);
            } else {
                String item = parser.parseItemId(true);
                if (tokens.skip("*")) {
                    cmds.accept("clear " + selector + " " + item + " " + tokens.next(TokenType.INT));
                } else {
                    cmds.accept("clear " + selector + " " + item);
                }
            }
        },"clear");

        addSelectorMember((selector, cmds) -> {
            doTitle(selector, cmds, "title");
        }, "title");

        addSelectorMember((selector, cmds) -> {
            doTitle(selector, cmds, "subtitle");
        }, "subtitle");

        addSelectorMember((selector, cmds) -> {
            doTitle(selector, cmds, "actionbar");
        }, "action", "actionbar");

        addSelectorMember((selector, cmds) -> {
            tokens.expect("(");

            String fadeIn = tokens.expect(TokenType.INT).getValue();
            tokens.expect(",");

            String stay = tokens.expect(TokenType.INT).getValue();

            tokens.expect(",");
            String fadeOut = tokens.expect(TokenType.INT).getValue();

            tokens.expect(")");

            cmds.accept("title " + selector + " times " + fadeIn + " " + stay + " " + fadeOut);
        }, "titleTimes");

        addSelectorMember((selector,cmds)->{
            if (tokens.skip("=")) {
                String nbt = parser.parseNBT();
                cmds.accept("data merge entity " + selector + " " + nbt);
                return;
            }
            tokens.expect('[');
            String path = tokens.next(TokenType.STRING);
            tokens.expect(']');
            if (tokens.skip("=")) {
                String source = parser.parseNBTSource();
                cmds.accept("data modify entity " + selector + " " + path + " set " + source);
            } else if (tokens.skip(".")) {
                String methodLabel = tokens.next(TokenType.IDENTIFIER);
                String method = null;
                tokens.expect('(');
                if ("insert".equals(methodLabel)) {
                    method = "insert " + tokens.next(TokenType.INT);
                    tokens.expect(',');
                }
                String source = parser.parseNBTSource();
                switch (methodLabel) {
                    case "push":
                    case "add":
                    case "append":
                        method = "append";
                        break;
                    case "merge":
                        method = "merge";
                        break;
                    case "prepend":
                    case "unshift":
                        method = "prepend";
                        break;
                }
                cmds.accept("data modify entity " + selector + " " + path + " " + method + " " + source);
            } else {
                double scale = 1;
                if (tokens.skip("*")) {
                    if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) {
                        scale = Double.parseDouble(tokens.nextValue());
                    }
                }
                cmds.accept("data get entity " + selector + " " + path + " " + scale);
            }
        },"nbt","data");
        addSelectorMember((selector,cmds)->{
            tokens.expect('=');
            cmds.accept("gamemode " + parser.parseIdentifierOrIndex("gamemode",Parser.gamemodes) + " " + selector);
        },"gamemode");
    }

    private void addSelectorMember(BiConsumer<String, Consumer<String>> parser, String... ids) {
        selectorMembers.add(new SelectorMember() {
            @Override
            public String[] getIdentifiers() {
                return ids;
            }

            @Override
            public void parse(String selector, Parser p, Consumer<String> commands) {
                parser.accept(selector,commands);
            }
        });
    }

    /**
     * @param type title, subtitle, actionbar
     */
    private void doTitle(String selector, Consumer<String> cmds, String type) {
        tokens.expect("(");
        String json = parser.readJsonText();

        tokens.expect(")");
        cmds.accept("title " + selector + " " + type + " " + json);
    }

    public String parseSelector() {
        String target;
        boolean type = false;
        if (tokens.skip("all","any","e","entity","entities")) {
            target = "e";
        } else if (tokens.skip("players","a","everyone","allplayers")) {
            target = "a";
        } else if (tokens.skip("closest","p","nearest","player")) {
            target = "p";
        } else if (tokens.skip("this","self","s","me")) {
            target = "s";
        } else if (tokens.skip("random","r")) {
            target = "r";
        } else if (Parser.entityIds.contains(tokens.peek().getValue())) {
            type = true;
            target = "e[type=" + tokens.nextValue();
        } else {
            throw new RuntimeException("Invalid target selector");
        }
        String selector = "@" + target;
        if (tokens.skip("[")) {
            if (type) {
                selector += ",";
            } else {
                selector += "[";
            }
            while (!tokens.skip("]")) {
                String f = tokens.next(TokenType.IDENTIFIER);
                switch (f) {
                    case "name":
                        tokens.expect('=');
                        selector += "name=" + tokens.next(TokenType.STRING);
                        break;
                    case "tag":
                        tokens.expect('=');
                        selector += "tag=" + tokens.next(TokenType.IDENTIFIER);
                        break;
                    case "tags":
                        tokens.expect('=');
                        tokens.expect('(');
                        while (!tokens.skip(")")) {
                            selector += "tag=" + tokens.next(TokenType.IDENTIFIER);
                            tokens.skip(",");
                        }
                        break;
                    case "gm":
                    case "gamemode":
                        tokens.expect("=");
                        selector += "gamemode=" + parser.parseIdentifierOrIndex("gamemode",Parser.gamemodes);
                    case "scores":
                        break;
                    case "rot":
                    case "rotation":

                }
                if (tokens.skip(",")) {
                    selector += ",";
                } else if (!tokens.isNext("]")) {
                    throw new RuntimeException("Invalid entity selector, expected , or ]");
                }
            }
            selector += "]";
        } else if (type){
            selector += "]";
        }
        return selector;
    }

    public List<String> parseSelectorCommand() {
        List<String> cmds = new ArrayList<>();
        String selector = parseSelector();
        if (tokens.skip(".")) {
            String field = tokens.next(TokenType.IDENTIFIER);
            for (SelectorMember m : selectorMembers) {
                for (String id : m.getIdentifiers()) {
                    if (id.equals(field)) {
                        m.parse(selector,parser,cmds::add);
                        break;
                    }
                }
            }
            switch (field) {
                case "inventory":
                case "enderchest":
                case "hotbar":
                case "horse":
                case "container":
                case "villager":{
                    tokens.expect('[');
                    int slot = Integer.parseInt(tokens.next(TokenType.INT));
                    if (slot < 0 || slot >= Parser.INVENTORY_SIZES.get(field))
                        throw new RuntimeException("Inventory/Enderchest slot index is out of bounds!");
                    tokens.expect(']');
                    tokens.expect('=');
                    String item = parser.parseItemId(false);
                    cmds.add("replaceitem entity " + selector + " " + (field + "." + slot) + " " + item + " " + parser.readOptionalInt());
                    break;
                }
                case "mainhand":
                case "hand":
                case "righthand": {
                    tokens.expect('=');
                    String item = parser.parseItemId(false);
                    cmds.add("replaceitem entity " + selector + " weapon.mainhand " + item + " " + parser.readOptionalInt());
                    break;
                }
                case "offhand":
                case "lefthand": {
                    tokens.expect('=');
                    String item = parser.parseItemId(false);
                    cmds.add("replaceitem entity " + selector + " weapon.offhand " + item + " " + parser.readOptionalInt());
                    break;
                }
                case "boots":
                case "chestplate":
                case "helmet":
                case "leggings": {
                    tokens.expect('=');
                    String item = parser.parseItemId(false);
                    cmds.add("replaceitem entity " + selector + " armor." + Parser.ARMOR_SLOT_NAMES.get(field) + " " + item + " " + parser.readOptionalInt());
                    break;
                }
            }
        }
        return cmds;
    }
}
