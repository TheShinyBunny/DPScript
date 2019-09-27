package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.*;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;
import com.shinysponge.dpscript.tokenizew.Tokenizer;

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
                criterion = tokens.next(TokenType.IDENTIFIER,"advancement criterion");
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
                String item = parser.parseItemAndCount();
                cmds.accept("clear " + selector + " " + item);
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

            String fadeIn = tokens.expect(TokenType.INT,"fade in value").getValue();
            tokens.expect(",");

            String stay = tokens.expect(TokenType.INT,"stay value").getValue();

            tokens.expect(",");
            String fadeOut = tokens.expect(TokenType.INT,"fade out value").getValue();

            tokens.expect(")");

            cmds.accept("title " + selector + " times " + fadeIn + " " + stay + " " + fadeOut);
        }, "titleTimes");

        addSelectorMember((selector,cmds)->{
            cmds.accept(NBTDataParser.parse("entity " + selector,parser));
        },"nbt","data");
        addSelectorMember((selector,cmds)->{
            tokens.expect('=');
            cmds.accept("gamemode " + Parser.parseIdentifierOrIndex(tokens,"gamemode",Parser.gamemodes) + " " + selector);
        },"gamemode");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String enchID = parser.parseResourceLocation(false);
            Enchantments ench = Enchantments.get(enchID);
            if (ench == null) {
                tokens.pushBack();
                parser.compilationError(ErrorType.UNKNOWN,"enchantment ID");
                ench = Enchantments.PROTECTION;
            }
            int level = 1;
            if (tokens.isNext(TokenType.INT)) {
                level = Integer.parseInt(tokens.nextValue());
            } else if (!tokens.isNext(")")){
                level = parser.readRomanNumber(tokens.nextValue());
            }
            if (level < 1) {
                parser.compilationError(ErrorType.INVALID,"enchantment level, expected a positive number or a roman number.");
            }
            if (level > ench.getMaxLevel()) {
                parser.compilationError(ErrorType.INVALID,"Enchantment " + ench + " level! It's greater than " + ench + " max level (" + ench.getMaxLevel() + ")!");
            }
            tokens.expect(')');
            cmds.accept("enchant " + selector + " " + ench.name().toLowerCase() + " " + level);
        },"enchant","ench");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String tag = tokens.next(TokenType.IDENTIFIER,"tag identifier");
            tokens.expect(')');
            cmds.accept("tag " + selector + " add " + tag);
        },"tag","addTag");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String tag = tokens.next(TokenType.IDENTIFIER,"tag identifier");
            tokens.expect(')');
            cmds.accept("tag " + selector + " remove " + tag);
        },"untag","removeTag");
        addSelectorMember((selector,cmds)->{
            if (tokens.isNext("++","+=","-=","--","=")) {
                String op = tokens.expect("++","+=","-=","--","=");
                String method;
                int amount = 1;
                method = "add";
                if (op.equals("+=") || op.equals("-=") || op.equals("=")) {
                    amount = Integer.parseInt(tokens.next(TokenType.INT,"xp value"));
                }
                if (op.equals("--") || op.equals("-=")) {
                    amount = -amount;
                }
                if (op.equals("=")) {
                    method = "set";
                }
                String pl = "points";
                if (tokens.skip("l","L","levels","lvl")) {
                    pl = "levels";
                } else if (tokens.skip("p","pts","points","P")) {
                    pl = "points";
                }
                cmds.accept("xp " + method + " " + selector + " " + amount + " " + pl);
            } else if (tokens.skip(".")) {
                cmds.accept("xp query " + selector + " " + tokens.expect("levels","points"));
            }
        },"xp","exp","experience");
        addSelectorMember((selector,cmds)->{
            tokens.expect('=');
            cmds.accept("spawnpoint " + parser.readPosition() + " " + selector);
        },"spawnpoint","spawn");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');tokens.expect(')');
            cmds.accept("kill " + selector);
        },"kill","remove","die","despawn","sendToHeaven");
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
        String json = JsonTextParser.readTextComponent(parser);

        tokens.expect(")");
        cmds.accept("title " + selector + " " + type + " " + json);
    }

    /**
     * Parses a selector from a literal string. Used for selectors inside JSON texts.
     *
     * @param p
     * @param selector The selector string
     * @return A vanilla selector string
     */
    public static String parseStringSelector(Parser p, String selector) {
        TokenIterator tokens = new TokenIterator(Tokenizer.tokenize(p.file,selector),p::compilationError);
        return parseSelectorFrom(p,tokens);
    }

    public static String parseSelectorFrom(Parser parser, TokenIterator tokens) {
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
            tokens.error(ErrorType.INVALID,"target selector");
            target = "e";
        }
        String selector = "@" + target;
        if (tokens.skip("[")) {
            if (type) {
                selector += ",";
            } else {
                selector += "[";
            }
            List<String> scores = new ArrayList<>();
            while (!tokens.skip("]")) {
                String f = tokens.next(TokenType.IDENTIFIER,"selector field");
                switch (f) {
                    case "name":
                        tokens.expect('=');
                        selector += "name=" + tokens.next(TokenType.STRING,"entity name");
                        break;
                    case "tag":
                        tokens.expect('=');
                        selector += "tag=" + tokens.next(TokenType.IDENTIFIER,"tag identifier");
                        break;
                    case "tags":
                        tokens.expect('=');
                        tokens.expect('(');
                        while (!tokens.skip(")")) {
                            selector += "tag=" + tokens.next(TokenType.IDENTIFIER,"tag identifier");
                            tokens.skip(",");
                        }
                        break;
                    case "gm":
                    case "gamemode":
                        tokens.expect("=");
                        selector += "gamemode=" + Parser.parseIdentifierOrIndex(tokens,"gamemode",Parser.gamemodes);
                    case "nbt":
                        tokens.expect('=');
                        boolean negate = tokens.skip("!");
                        selector += "nbt=" + (negate ? "!" : "") + Parser.parseNBT(tokens);
                        break;
                        default:
                            if (parser.hasObjective(f)) {
                                String op = tokens.expect(">","<",">=","<=","=","==");
                                int value = Integer.parseInt(tokens.next(TokenType.INT,"objective value"));
                                String range = "0";
                                switch (op) {
                                    case ">":
                                        range = (value + 1) + "..";
                                        break;
                                    case "<":
                                        range = ".." + (value - 1);
                                        break;
                                    case ">=":
                                        range = value + "..";
                                        break;
                                    case "<=":
                                        range = ".." + value;
                                        break;
                                    case "=":
                                    case "==":
                                        range = value + "";
                                        break;
                                        default:
                                            break;
                                }
                                scores.add(f + "=" + range);
                            } else {
                                tokens.error(ErrorType.UNKNOWN,"selector field");
                            }
                }
                if (tokens.skip(",")) {
                    selector += ",";
                } else if (!tokens.isNext("]")) {
                    tokens.error(ErrorType.INVALID,"entity selector: expected , or ]");
                }
            }
            if (!scores.isEmpty()) {
                selector += "scores={" + String.join(",",scores) + "}";
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
            String field = tokens.next(TokenType.IDENTIFIER,"selector field");
            for (SelectorMember m : selectorMembers) {
                for (String id : m.getIdentifiers()) {
                    if (id.equals(field)) {
                        m.parse(selector,parser,cmds::add);
                        return cmds;
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
                    int slot = Integer.parseInt(tokens.next(TokenType.INT,"slot index"));
                    if (slot < 0 || slot >= Parser.INVENTORY_SIZES.get(field))
                        parser.compilationError(ErrorType.INVALID,"Inventory/Enderchest slot index, it's out of bounds!");
                    tokens.expect(']');
                    tokens.expect('=');
                    String item = parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " " + (field + "." + slot) + " " + item + " " + parser.readOptionalInt());
                    break;
                }
                case "mainhand":
                case "hand":
                case "righthand": {
                    tokens.expect('=');
                    String item = parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " weapon.mainhand " + item + " " + parser.readOptionalInt());
                    break;
                }
                case "offhand":
                case "lefthand": {
                    tokens.expect('=');
                    String item = parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " weapon.offhand " + item + " " + parser.readOptionalInt());
                    break;
                }
                case "boots":
                case "chestplate":
                case "helmet":
                case "leggings": {
                    tokens.expect('=');
                    String item = parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " armor." + Parser.ARMOR_SLOT_NAMES.get(field) + " " + item + " " + parser.readOptionalInt());
                    break;
                }
                default:
                    if (tokens.peek().getValue().equals("(")) {
                        if (parser.findFunction(field)) {
                            tokens.expect(')');
                            cmds.add("execute as " + selector + " at @s run function " + field);
                            return cmds;
                        } else {
                            parser.compilationError(ErrorType.UNKNOWN,"function " + field + "()");
                            return cmds;
                        }
                    }
                    if (parser.hasObjective(field)) {
                        cmds.addAll(parseScoreOperators(selector + " " + field));
                    } else {
                        parser.compilationError(ErrorType.UNKNOWN,"selector field " + field);
                    }
            }
        }
        return cmds;
    }

    public String parseSelector() {
        return parseSelectorFrom(parser,tokens);
    }

    public String parseObjectiveSelector() {
        String selector = parseSelector();
        tokens.expect('.');
        String obj = tokens.next(TokenType.IDENTIFIER,"objective name");
        if (!parser.hasObjective(obj)) parser.compilationError(ErrorType.UNKNOWN,"objective " + obj);
        return selector + " " + obj;
    }

    public List<String> parseScoreOperators(String access) {
        List<String> cmds = new ArrayList<>();
        for (ObjectiveOperators op : ObjectiveOperators.values()) {
            if (tokens.skip(op.getOperator())) {
                if (tokens.skip("@")) {
                    String source = parseObjectiveSelector();
                    cmds.add("scoreboard players operation " + access + " " + op.getOperationOperator() + " " + source);
                    return cmds;
                } else if (tokens.isNext(TokenType.INT)) {
                    int value = Integer.parseInt(tokens.nextValue());
                    if (op.getLiteralCommand() == null) {
                        parser.createConstant(String.valueOf(value),value);
                        cmds.add("scoreboard players operation " + access + " " + op.getOperationOperator() + " " + value + " Constants");
                    } else {
                        cmds.add("scoreboard players " + op.getLiteralCommand() + " " + access + " " + value);
                    }
                    return cmds;
                } else if (op == ObjectiveOperators.EQUALS) {
                    cmds.add(parser.parseExecuteStore("score " + access));
                    return cmds;
                }
                parser.compilationError(ErrorType.EXPECTED,"a literal value or another score after score operator");
                return cmds;
            }
        }
        parser.compilationError(ErrorType.INVALID,"score operation");
        return cmds;
    }
}
