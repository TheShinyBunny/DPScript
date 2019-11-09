package com.shinysponge.dpscript.pawser.parsers;

import com.shinybunny.utils.ListUtils;
import com.shinysponge.dpscript.entities.NBT;
import com.shinysponge.dpscript.pawser.*;
import com.shinysponge.dpscript.pawser.selector.Selector;
import com.shinysponge.dpscript.pawser.selector.SimpleSelector;
import com.shinysponge.dpscript.tokenizew.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SelectorParser {

    private static TokenIterator tokens;

    private static List<SelectorMember> selectorMembers = new ArrayList<>();

    static {

        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            EffectParser.Effect effect = EffectParser.parseEffect();
            tokens.expect(')');
            if (effect.isDefault() && tokens.skip("clear","remove","cure")) {
                cmds.accept("effect clear " + selector + " " + effect);
            } else if (effect.seconds > 0 || effect.hide || effect.tier >= 0) {
                effect.defaultDurationAndTier();
                cmds.accept("effect give " + selector + " " + effect);
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
            String adv = Parser.parseResourceLocation(false);

            String criterion = "";
            if (addCriterion && tokens.skip("[")) {
                criterion = tokens.expect(TokenType.IDENTIFIER,"advancement criterion");
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
                Item item = Parser.parseItemAndCount();
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

            String fadeIn = tokens.expect(TokenType.INT,"fade in value");
            tokens.expect(",");

            String stay = tokens.expect(TokenType.INT,"stay value");

            tokens.expect(",");
            String fadeOut = tokens.expect(TokenType.INT,"fade out value");

            tokens.expect(")");

            cmds.accept("title " + selector + " times " + fadeIn + " " + stay + " " + fadeOut);
        }, "titleTimes");

        addSelectorMember((selector,cmds)->{
            cmds.accept(NBTDataParser.parse("entity " + selector));
        },"nbt","data");
        addSelectorMember((selector,cmds)->{
            tokens.expect('=');
            cmds.accept("gamemode " + Parser.parseIdentifierOrIndex(tokens,"gamemode",Parser.gamemodes) + " " + selector);
        },"gamemode");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String enchID = Parser.parseResourceLocation(false);
            Enchantments ench = Enchantments.get(enchID);
            if (ench == null) {
                tokens.pushBack();
                Parser.compilationError(ErrorType.UNKNOWN,"enchantment ID");
                ench = Enchantments.PROTECTION;
            }
            int level = 1;
            if (tokens.isNext(TokenType.INT)) {
                level = Integer.parseInt(tokens.nextValue());
            } else if (!tokens.isNext(")")){
                level = Parser.readRomanNumber(tokens.nextValue());
            }
            if (level < 1) {
                Parser.compilationError(ErrorType.INVALID,"enchantment level, expected a positive number or a roman number.");
            }
            if (level > ench.getMaxLevel()) {
                Parser.compilationError(ErrorType.INVALID,"Enchantment " + ench + " level! It's greater than " + ench + " max level (" + ench.getMaxLevel() + ")!");
            }
            tokens.expect(')');
            cmds.accept("enchant " + selector + " " + ench.name().toLowerCase() + " " + level);
        },"enchant","ench");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String tag = tokens.expect(TokenType.IDENTIFIER,"tag identifier");
            tokens.expect(')');
            cmds.accept("tag " + selector + " add " + tag);
        },"tag","addTag");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');
            String tag = tokens.expect(TokenType.IDENTIFIER,"tag identifier");
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
                    amount = Integer.parseInt(tokens.expect(TokenType.INT,"xp value"));
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
            cmds.accept("spawnpoint " + Parser.readPosition() + " " + selector);
        },"spawnpoint","spawn");
        addSelectorMember((selector,cmds)->{
            tokens.expect('(');tokens.expect(')');
            cmds.accept("kill " + selector);
        },"kill","remove","die","despawn","sendToHeaven");
        addSelectorMember((selector, cmds) -> {
            tokens.expect("(");
            String pos = Parser.readPosition();
            tokens.expect(")");
            cmds.accept("tp " + selector + " " + pos);
        }, "tp");
        addSelectorMember((selector, cmds)->{
            tokens.expect('(');
            String json = JsonTextParser.readTextComponent();
            tokens.expect(')');
            cmds.accept("tellraw " + selector + " " + json);
        },"tellraw","tell");
        addSelectorMember((selector, cmds)->{
            if (!selector.targetsPlayers()) {
                Parser.compilationError(null,"selector " + selector + " can target non-players, but give() can only be used on players.");
            }
            tokens.expect("(");
            String src = LootParser.parseLootSource();
            if (src == null) {
                Item item = Parser.parseItemAndCount();
                tokens.expect(")");
                cmds.accept("give " + selector + " " + item);
            } else {
                cmds.accept("loot give " + selector + " " + src);
            }
        },"give");
    }

    private static void addSelectorMember(BiConsumer<Selector, Consumer<String>> parser, String... ids) {
        selectorMembers.add(new SelectorMember() {
            @Override
            public String[] getIdentifiers() {
                return ids;
            }

            @Override
            public void parse(Selector selector, Consumer<String> commands) {
                parser.accept(selector,commands);
            }
        });
    }

    /**
     * @param type title, subtitle, actionbar
     */
    private static void doTitle(Selector selector, Consumer<String> cmds, String type) {
        tokens.expect("(");
        String json = JsonTextParser.readTextComponent();

        tokens.expect(")");
        cmds.accept("title " + selector + " " + type + " " + json);
    }

    /**
     * Parses a selector from a literal string. Used for selectors inside JSON texts.
     *
     * @param selector The selector string
     * @return A vanilla selector string
     */
    public static Selector parseSelector(Token selector) {
        TokenIterator tokens = TokenIterator.from(selector);
        return parseSelectorFrom(tokens);
    }

    public static Selector parseSelector() {
        return parseSelectorFrom(Parser.tokens);
    }

    private static Selector parseSelectorFrom(TokenIterator tokens) {
        char target;
        String type = null;
        tokens.suggestHere(ListUtils.concat(Parser.entityIds, Arrays.asList("a","e","p","s","r")));
        if (tokens.skip("all","any","e","entity","entities")) {
            target = 'e';
        } else if (tokens.skip("players","a","everyone","allplayers")) {
            target = 'a';
        } else if (tokens.skip("closest","p","nearest","player")) {
            target = 'p';
        } else if (tokens.skip("this","self","s","me")) {
            target = 's';
        } else if (tokens.skip("random","r")) {
            target = 'r';
        } else if (Parser.entityIds.contains(tokens.peek().getValue())) {
            target = 'e';
            type = tokens.nextValue();
        } else {
            tokens.error(ErrorType.INVALID,"target selector");
            target = 'e';
        }
        Map<String,String> params = new HashMap<>();
        if (type != null) {
            params.put("type",type);
        }
        if (tokens.skip("[")) {
            List<String> scores = new ArrayList<>();
            while (!tokens.skip("]")) {
                tokens.suggestHere(Arrays.asList("name","tag","tags","gamemode","nbt"));
                String f = tokens.expect(TokenType.IDENTIFIER,"selector field");
                switch (f) {
                    case "name":
                        tokens.expect('=');
                        params.put("name",tokens.expect(TokenType.STRING,"entity name"));
                        break;
                    case "tag":
                        tokens.expect('=');
                        params.put("tag",tokens.expect(TokenType.IDENTIFIER,"tag identifier"));
                        break;
                    case "tags":
                        tokens.expect('=');
                        List<String> tags = Parser.readList('(',')',t->tokens.expect(TokenType.IDENTIFIER,"tag identifier"));
                        params.put("tag",Selector.toMultiParams("tag",tags));
                        break;
                    case "gm":
                    case "gamemode":
                        tokens.expect("=");
                        params.put("gamemode", Parser.parseIdentifierOrIndex(tokens,"gamemode",Parser.gamemodes));
                    case "nbt":
                        tokens.expect('=');
                        boolean negate = tokens.skip("!");
                        params.put("nbt",(negate ? "!" : "") + NBT.parse());
                        break;
                        default:
                            if (Parser.hasObjective(f)) {
                                String op = tokens.expect(">","<",">=","<=","=","==");
                                int value = Integer.parseInt(tokens.expect(TokenType.INT,"objective value"));
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
                                break;
                            }
                }
                if (!tokens.skip(",") && !tokens.isNext("]")) {
                    tokens.error(ErrorType.INVALID,"entity selector: expected , or ]");
                    break;
                }
            }
            if (!scores.isEmpty()) {
                params.put("scores","{" + String.join(",",scores) + "}");
            }
        }
        return new SimpleSelector(target,params);
    }

    public static List<String> parseSelectorCommand(Selector selector) {
        List<String> cmds = new ArrayList<>();
        tokens = Parser.tokens;
        if (tokens.skip(".")) {
            Token token = tokens.peek();
            String field = tokens.expect(TokenType.IDENTIFIER,"selector field");
            for (SelectorMember m : selectorMembers) {
                for (String id : m.getIdentifiers()) {
                    if (id.equals(field)) {
                        m.parse(selector,cmds::add);
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
                    int slot = Integer.parseInt(tokens.expect(TokenType.INT,"slot index"));
                    if (slot < 0 || slot >= Parser.INVENTORY_SIZES.get(field))
                        Parser.compilationError(ErrorType.INVALID,"Inventory/Enderchest slot index, it's out of bounds!");
                    tokens.expect(']');
                    tokens.expect('=');
                    Item item = Parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " " + (field + "." + slot) + " " + item);
                    break;
                }
                case "mainhand":
                case "hand":
                case "righthand": {
                    tokens.expect('=');
                    Item item = Parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " weapon.mainhand " + item);
                    break;
                }
                case "offhand":
                case "lefthand": {
                    tokens.expect('=');
                    Item item = Parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " weapon.offhand " + item);
                    break;
                }
                case "boots":
                case "chestplate":
                case "helmet":
                case "leggings": {
                    tokens.expect('=');
                    Item item = Parser.parseItemAndCount();
                    cmds.add("replaceitem entity " + selector + " armor." + Parser.ARMOR_SLOT_NAMES.get(field) + " " + item);
                    break;
                }
                case "enable": {
                    // Trigger enabling
                    tokens.expect("(");
                    String identifier = tokens.expect(TokenType.IDENTIFIER, "Trigger name");
                    tokens.expect(")");

                    if(Parser.hasTrigger(identifier)) {
                        cmds.add("scoreboard players enable " + selector + " " + identifier);
                    } else {
                        Parser.compilationError(ErrorType.UNKNOWN, "trigger " + identifier);
                    }
                    break;
                }
                default:
                    if (tokens.skip("(")) {
                        tokens.expect(')');
                        cmds.add("execute as " + selector + " at @s run " + Parser.getContext().callFunction(token.getPos(),field));
                        return cmds;
                    }
                    if (Parser.hasObjective(field)) {
                        cmds.addAll(parseScoreOperators(selector + " " + field));
                    } else {
                        Parser.compilationError(ErrorType.UNKNOWN,"selector field " + field);
                    }
            }
        }
        return cmds;
    }

    public static String parseObjectiveSelector() {
        TokenIterator tokens = Parser.tokens;
        Selector selector = parseSelector();
        tokens.expect('.');
        String obj = tokens.expect(TokenType.IDENTIFIER,"objective name");
        if (!Parser.hasObjective(obj)) Parser.compilationError(ErrorType.UNKNOWN,"objective " + obj);
        return selector + " " + obj;
    }

    public static List<String> parseScoreOperators(String access) {
        TokenIterator tokens = Parser.tokens;
        List<String> cmds = new ArrayList<>();
        for (ObjectiveOperators op : ObjectiveOperators.values()) {
            if (tokens.skip(op.getOperator())) {
                if (op.isUnary()) {
                    cmds.add("scoreboard players " + op.getLiteralCommand() + " " + access + " " + 1);
                    return cmds;
                } else if (tokens.skip("@")) {
                    String source = parseObjectiveSelector();
                    cmds.add("scoreboard players operation " + access + " " + op.getOperationOperator() + " " + source);
                    return cmds;
                } else if (tokens.isNext(TokenType.INT)) {
                    int value = Integer.parseInt(tokens.nextValue());
                    if (op.getLiteralCommand() == null) {
                        Parser.createConstant(String.valueOf(value),value);
                        cmds.add("scoreboard players operation " + access + " " + op.getOperationOperator() + " " + value + " Constants");
                    } else {
                        cmds.add("scoreboard players " + op.getLiteralCommand() + " " + access + " " + value);
                    }
                    return cmds;
                } else if (op == ObjectiveOperators.EQUALS) {
                    cmds.add(Parser.parseExecuteStore("score " + access));
                    return cmds;
                }
                Parser.compilationError(ErrorType.EXPECTED,"a literal value or another score after score operator");
                return cmds;
            }
        }
        Parser.compilationError(ErrorType.INVALID,"score operation");
        return cmds;
    }

    public static List<String> parseSelectorAndCommand() {
        return parseSelectorCommand(parseSelector());
    }

    /**
     * Parses either a normal @e[args] selector, or a variable representing a selector, such as an entity template or a /summon instance
     * @return A Selector object representing the parsed selector
     */
    public static Selector parseAnySelector(boolean multiple) {
        tokens = Parser.tokens;
        if (tokens.isNext(TokenType.IDENTIFIER)) {
            Object var = Parser.getContext().getVariable(tokens.peek().getValue()).eval();
            if (var instanceof Selector) {
                tokens.skip();
                return (Selector) var;
            }
            Parser.compilationError(ErrorType.INVALID,"entity selector");
            // parse summon instance variable
        } else {
            tokens.expect('@');
            return SelectorParser.parseSelector();
        }
        return null;
    }
}
