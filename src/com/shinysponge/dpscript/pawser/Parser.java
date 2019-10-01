package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.conditions.*;
import com.shinysponge.dpscript.pawser.parsers.JsonTextParser;
import com.shinysponge.dpscript.pawser.parsers.NBTDataParser;
import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.project.CompilationContext;
import com.shinysponge.dpscript.tokenizew.*;

import java.time.Duration;
import java.util.*;

/**
 * This is the main class to parse the DPScript code.
 * It's very long and complex, but it works wonders.
 */
public class Parser {

    private static CompilationContext ctx;
    private static ScopeType scope;
    public static TokenIterator tokens;

    private static List<String> originalCode;

    private static Condition lastIf;


    public Parser(CompilationContext ctx) {

    }


    public static void parse(CompilationContext ctx) {
        Parser.ctx = ctx;
        Parser.originalCode = ctx.getFile().getCode();
        Parser.tokens = new TokenIterator(Tokenizer.tokenize(ctx.getFile(),String.join("\n",originalCode)),Parser::compilationError);
        Parser.scope = ScopeType.GLOBAL;
        Parser.parse();
    }

    /**
     * The root function of pawsing. Loops through the entire code and reads it statement by statement.
     * When it's done, prints out compilation errors if there are any, or otherwise, prints the generated functions.
     */
    private static void parse() {
        while (tokens.hasNext()) {
            if (tokens.skip(TokenType.LINE_END)) {
                continue;
            }
            parseStatement();
            tokens.nextLine();
        }
    }

    /**
     * Tells the parser that there is an error with the parsed code, and it should be informed to the user.<br/>
     * Note that this will not throw a {@link CompilationError}, but rather will add it to {@link CompilationContext#addError(CompilationError)}.<br/>
     * When the {@link #parse()} method is done, it will output all errors if there are any.<br/>
     * If you use any ErrorType <b>besides</b> {@link ErrorType#UNKNOWN}, {@link ErrorType#MISSING}, {@link ErrorType#DUPLICATE} or {@link ErrorType#INVALID_STATEMENT},
     * you need make sure to have the next token be the problematic one, so it can be added to the error message.
     * @param type The error type. Used for different error message formats and as the first word in the message itself.
     * @param description A description about the error. For example, when expecting an integer index: ErrorType = {@link ErrorType#EXPECTED}, description = "array index". Will be formatted as "Expected array index, found: Hello"
     */
    public static void compilationError(ErrorType type, String description) {
        String msg;
        CodePos pos = tokens.peek().getPos();
        if (type == null) {
            msg = "Compilation error at " + pos + ": " + description;
        } else if (type == ErrorType.INVALID_STATEMENT) {
            msg = "Compilation error at " + pos + ": Invalid " + description + " statement";
        } else if (type != ErrorType.MISSING && type != ErrorType.DUPLICATE && type != ErrorType.UNKNOWN) {
            Token token = tokens.next();
            msg = "Compilation error at " + pos + ": " + type.getName() + " " + description + ", found: " + token.getValue();
        } else {
            msg = "Compilation error at " + pos + ": " + type.getName() + " " + description;
        }
        ctx.addError(new CompilationError(msg,pos));
    }

    /**
     * Parses a single statement, or a block of statements. Will call either {@link #parseGlobal()} or {@link #parseNormal()} depending on the current {@link #scope ScopeType}.
     * @return The command translated from the statement, or multiple commands if it was a block statement or a conditional statement with || operator/s.
     */
    public static List<String> parseStatement() {
        List<String> list = new ArrayList<>();
        if (tokens.skip(TokenType.LINE_END)) return list;
        if (scope == ScopeType.GLOBAL) {
            parseGlobal();
        } else {
            list.addAll(parseNormal());
        }
        return list;
    }

    private static void parseGlobal() {
        Token t = tokens.next();
        switch (t.getValue()) {
            case "tick":
                parseTick();
                break;
            case "function": {
                String name = tokens.expect(TokenType.IDENTIFIER,"function name");
                if (ctx.getFunction(name) != null)
                    compilationError(ErrorType.DUPLICATE,"function " + name);
                tokens.expect('{');
                scope = ScopeType.NORMAL;
                ctx.addFunction(name,parseBlock());
                scope = ScopeType.GLOBAL;
                break;
            }
            case "bossbar":
                String id = parseResourceLocation(false);
                String displayName = JsonTextParser.readTextComponent(this);
                ctx.addLoad("bossbar add " + id + " " + displayName);
                ctx.bossbars.add(id);
                break;
            case "const": {
                String name = tokens.expect(TokenType.IDENTIFIER,"a name for the constant");
                if (ctx.hasConstant(name))
                    compilationError(ErrorType.DUPLICATE,"constant " + name);
                tokens.expect('=');
                int value = Integer.parseInt(tokens.expect(TokenType.INT,"an integer"));
                createConstant(name,value);
                break;
            }
            case "global": {
                String name = tokens.expect(TokenType.IDENTIFIER,"a name for the global variable");
                if (ctx.hasGlobal(name))
                    compilationError(ErrorType.DUPLICATE,"global " + name);
                createGlobal(name);
                break;
            }
            case "int": {
                String name = tokens.expect(TokenType.IDENTIFIER,"a variable name");
                if (hasObjective(name)) {
                    compilationError(ErrorType.DUPLICATE,"variable " + name);
                }
                ctx.objectives.add(name);
                ctx.addLoad("scoreboard objectives add " + name + " dummy");
                break;
            }
            case "trigger": {
                String name = tokens.expect(TokenType.IDENTIFIER,"a variable name");
                if (hasObjective(name)) {
                    compilationError(ErrorType.DUPLICATE,"variable " + name);
                }
                ctx.objectives.add(name);
                ctx.addLoad("scoreboard objectives add " + name + " trigger");
                ctx.triggers.add(name);
                break;
            }
            default:
                tokens.pushBack();
                compilationError(ErrorType.INVALID_STATEMENT,"global");

        }
    }

    private static void parseTick() {
        tokens.expect('{');
        tokens.nextLine();
        scope = ScopeType.NORMAL;
        List<String> cmds = parseBlock();
        scope = ScopeType.GLOBAL;
        cmds.forEach(ctx::addTick);
    }

    private static List<String> parseBlock() {
        List<String> list = new ArrayList<>();
        while (tokens.hasNext() && !tokens.isNext("}")) {
            if (tokens.skip(TokenType.LINE_END)) continue;
            list.addAll(parseStatement());
            tokens.nextLine();
        }
        tokens.expect("}");
        return list;
    }

    private static List<String> parseNormal() {
        List<String> list = new ArrayList<>();

        if(tokens.isNext(TokenType.RAW_COMMAND)) {
            list.add(tokens.nextValue());
            return list;
        }
        if (lastIf != null && tokens.skip("else")) {
            list.addAll(parseElse(lastIf));
            lastIf = null;
            return list;
        }
        lastIf = null;
        Token token = tokens.next();
        String value = token.getValue();
        switch (value){
            case "{":
                list.addAll(parseBlock());
                break;
            case "print":
                list.add("say " + tokens.expect(TokenType.STRING, "message"));
                break;
            case "if":
                list.addAll(parseIf());
                break;
            case "@":
                list.addAll(SelectorParser.parseSelectorCommand());
                break;
            case "clone": {
                String clone = "clone " + readPosition() + " " + readPosition() + " " + readPosition();
                String block = "";
                if (!tokens.isNext(TokenType.LINE_END)) {
                    if (tokens.skip("no_air", "non_air", "nonair", "masked")) {
                        clone += " masked";
                    } else if (tokens.skip("filter", "filtered", "only")) {
                        tokens.expect('(');
                        block = parseBlockId(true);
                        tokens.expect(')');
                        clone += " filtered";
                    } else if (tokens.skip("replace", "all")) {
                        clone += " replace";
                    } else {
                        compilationError(ErrorType.INVALID,"mask mode for clone command");
                    }

                    if (!tokens.isNext(TokenType.LINE_END)) {
                        if (tokens.skip("force", "forced", "overlap")) {
                            clone += " force";
                        } else if (tokens.skip("move")) {
                            clone += " move";
                        } else if (tokens.skip("normal", "copy")) {
                            clone += " normal";
                        } else {
                            compilationError(ErrorType.INVALID,"clone mode for clone command");
                        }
                    }
                }
                clone += " " + block;
                list.add(clone);

                break;
            }
            case "fill": {
                String fill = "fill " + readPosition() + " " + readPosition();
                fill += " " + parseBlockId(false);
                if (tokens.isNext(TokenType.IDENTIFIER)) {
                    String mode = tokens.expect("destroy","keep","replace","hollow","outline");
                    fill += " " + mode;
                    if ("replace".equals(mode)) {
                        tokens.skip("(");
                        fill += " " + parseBlockId(true);
                        tokens.skip(")");
                    }
                }
                list.add(fill);
                break;
            }
            case "summon":
                String entity = parseResourceLocation(false);
                if (!entityIds.contains(entity.substring(entity.indexOf(':')+1))) {
                    compilationError(ErrorType.UNKNOWN,"entity ID");
                }
                list.add("summon " + entity + " " + readPosition());
                break;
            case "for": {
                tokens.expect("@");
                String selector = selectors.parseSelector();
                list.add("execute as " + selector + " at @s run " + readExecuteRunCommand());
                break;
            }
            case "as":
            case "at": {
                tokens.expect('@');
                String selector = selectors.parseSelector();
                list.add(chainExecute(token + " " + selector));
                break;
            }
            case "facing":
            case "face": {
                String args;
                if (tokens.skip("@")) {
                    String selector = selectors.parseSelector();
                    tokens.expect('.');
                    String anchor = tokens.expect("feet","eyes");
                    args = "entity " + selector + " " + anchor;
                } else {
                    args = readPosition();
                }
                list.add("facing " + args);
                break;
            }
            case "in":
                String dim = tokens.expect("overworld","the_nether","the_end");
                list.add(chainExecute("in " + dim));
                break;
            case "offset":
            case "positioned": {
                String args;
                if (tokens.skip("@")) {
                    args = "as " + selectors.parseSelector();
                } else {
                    args = readPosition();
                }
                list.add(chainExecute("positioned " + args));
                break;
            }
            case "rotate":
            case "rotated": {
                String args;
                if (tokens.skip("@")) {
                    args = "as " + selectors.parseSelector();
                } else {
                    args = readRotation();
                }
                list.add(chainExecute("rotated " + args));
                break;
            }
            case "defaultgamemode":
                tokens.skip("=");
                if (tokens.isNext(TokenType.LINE_END)) {
                    list.add("defaultgamemode");
                } else {
                    list.add("defaultgamemode " + parseIdentifierOrIndex(tokens,"gamemode",gamemodes));
                }
                break;
            case "difficulty":
                tokens.skip("=");
                if (tokens.isNext(TokenType.LINE_END)) {
                    list.add("difficulty");
                } else {
                    list.add("difficulty " + parseIdentifierOrIndex(tokens,"difficulty",difficulties));
                }
                break;
            case "worldspawn":
                tokens.skip("=");
                list.add("setworldspawn " + readPosition());
                break;
            case "time":
                if (tokens.skip("=")) {
                    if (tokens.isNext(TokenType.INT)) {
                        list.add("time set " + tokens.nextValue());
                    } else {
                        list.add("time set " + tokens.expect("day","night","midnight","noon"));
                    }
                } else if (tokens.skip("+=")) {
                    list.add("time add " + tokens.expect(TokenType.INT,"an integer indicating the time in ticks to add"));
                } else if (tokens.skip(".")) {
                    list.add("time query " + tokens.expect("day","daytime","gametime"));
                }
                break;
            case "weather":
                tokens.skip("=");
                String weather = tokens.expect("clear","rain","thunder");
                tokens.skip("for");
                if (tokens.isNext(TokenType.INT)) {
                    int time = Integer.parseInt(tokens.nextValue());
                    if (time < 0 || time > 1000000) {
                        compilationError(ErrorType.INVALID,"duration argument for weather, must be between 0 - 1,000,000");
                    }
                    weather += " " + time;
                }
                list.add("weather " + weather);
                break;
            case "block":
                tokens.expect('(');
                String pos = readPosition();
                tokens.expect(')');
                tokens.expect('.');
                list.add(readBlockCommand(pos));
                break;
            case "while":
                list.addAll(parseWhile());
                break;
            default:
                if (tokens.skipAll("(",")")) {
                    list.add(ctx.callFunction(token.getPos(),value));
                } else if (ctx.hasGlobal(value)) {
                    list.addAll(selectors.parseScoreOperators(getVariableAccess(value)));
                } else if (ctx.bossbars.contains(value)) {
                    list.add(parseBossbarCommand(value));
                } else {
                    tokens.pushBack();
                    compilationError(ErrorType.INVALID_STATEMENT,"function");
                }
        }
        return list;
    }

    private static String readBlockCommand(String pos) {
        String member = tokens.expect(TokenType.IDENTIFIER,"a block function (break(), nbt/data, container[])");
        switch (member) {
            case "break":
                tokens.expect('(');
                String mode = "replace";
                if (tokens.skip("drop","destroy","true")) {
                    mode = "destroy";
                }
                tokens.expect(')');
                return "setblock " + pos + " air " + mode;
            case "nbt":
            case "data":
                return NBTDataParser.parse("block " + pos,this);
            case "container":
                tokens.expect('[');
                int slot = Integer.parseInt(tokens.expect(TokenType.INT,"a slot index in the container"));
                tokens.expect(']');
                tokens.expect('=');
                String item = parseItemAndCount();
                return "replaceitem block " + pos + " container." + slot + " " + item;
        }
        tokens.pushBack();
        compilationError(ErrorType.UNKNOWN,"block operation " + member);
        return "";
    }

    public static String parseNBTSource() {
        if (tokens.isNext("{")) {
            return "value " + parseNBTValue();
        }
        String source;
        if (tokens.skip("@")) {
            source = "entity " + selectors.parseSelector();
        } else {
            source = "block " + readPosition();
        }
        tokens.expect('[');
        String path = tokens.expect(TokenType.STRING,"NBT path");
        tokens.expect(']');
        return "from " + source + " " + path;
    }

    private static String chainExecute(String chain) {
        List<String> cmds = parseStatement();
        if (cmds.size() == 1) {
            if (cmds.get(0).startsWith("execute")) {
                return "execute " + chain + cmds.get(0).substring("execute".length());
            } else {
                return "execute " + chain + " run " + cmds.get(0);
            }
        } else {
            String fName = generateFunction(cmds);
            return "execute " + chain + " run function " + fName;
        }
    }

    private static String parseBossbarCommand(String bossbar) {
        tokens.expect('.');
        String field = tokens.expect(TokenType.IDENTIFIER,"a bossbar field (color,max,name,players,style,value,visible,show/display(),hide(),remove())");
        switch (field) {
            case "color":
                tokens.expect('=');
                String color = tokens.expect("blue","green","pink","purple","red","white","yellow");
                return "bossbar set " + bossbar + " color " + color;
            case "max":
                if (tokens.skip("=")) {
                    if (tokens.isNext(TokenType.INT)) {
                        int max = Integer.parseInt(tokens.expect(TokenType.INT,"an integer indicating the maximum bossbar value"));
                        return "bossbar set " + bossbar + " max " + max;
                    } else {
                        return parseExecuteStore("bossbar " + bossbar + " max");
                    }
                } else {
                    return "bossbar get " + bossbar + " max";
                }
            case "name":
                tokens.expect('=');
                String name = JsonTextParser.readTextComponent(this);
                return "bossbar set " + bossbar + " name " + name;
            case "players":
                if (tokens.skip("=")) {
                    tokens.expect('@');
                    String selector = selectors.parseSelector();
                    return "bossbar set " + bossbar + " players " + selector;
                } else {
                    return "bossbar get "+ bossbar + " players";
                }
            case "style":
                tokens.expect('=');
                String style = tokens.expect("notched_6","notched_10","notched_12","notched_20","progress");
                return "bossbar set " + bossbar + "style " + style;
            case "value":
                if (tokens.skip("=")) {
                    if (tokens.isNext(TokenType.INT)) {
                        int value = Integer.parseInt(tokens.expect(TokenType.INT,"an integer indicating the value of the bossbar"));
                        return "bossbar set " + bossbar + " value " + value;
                    } else {
                        return parseExecuteStore("bossbar " + bossbar + " value");
                    }
                } else {
                    return "bossbar get " + bossbar + " value";
                }
            case "visible":
                if (tokens.skip("=")) {
                    String bool = tokens.expect("true", "false");
                    return "bossbar set " + bossbar + " visible " + bool;
                } else {
                    return "bossbar get " + bossbar + " visible";
                }
            case "show":
            case "display":
                tokens.expect('(');tokens.expect(')');
                return "bossbar set " + bossbar + " visible true";
            case "hide":
                tokens.expect('(');tokens.expect(')');
                return "bossbar set " + bossbar + " visible false";
            case "remove":
                tokens.expect('(');tokens.expect(')');
                return "bossbar remove " + bossbar;
        }
        tokens.pushBack();
        compilationError(ErrorType.UNKNOWN,"bossbar field/command");
        return "";
    }

    public static String parseExecuteStore(String storeCommand) {
        String method = "result";
        String cmd;
        if (tokens.isNext("result","success")) {
            method = tokens.nextValue();
            tokens.expect('(');
            cmd = readExecuteRunCommand();
            tokens.expect(')');
        } else {
            cmd = readExecuteRunCommand();
        }

        return "execute store " + method + " " + storeCommand + " run " + cmd;
    }

    /**
     * Parses an identifier or an index of the identifier from the specified values. Used currently for gamemodes and difficulties.
     * @param tokens The TokenIterator to use. Necessary because this is a static method, to allow {@link SelectorParser#parseStringSelector(Parser, String)} to use this method.
     * @param name The name of the items. Used to throw an exception.
     * @param values The ids of the values
     * @return The matched identifier
     */
    public static String parseIdentifierOrIndex(TokenIterator tokens, String name, String... values) {
        if (tokens.isNext(TokenType.INT)) {
            int index = Integer.parseInt(tokens.expect(TokenType.INT,null));
            if (index < 0 || index > values.length) {
                tokens.error(ErrorType.INVALID,"gamemode index, must be between 0-3");
            }
            return values[index];
        } else if (tokens.isNext(TokenType.IDENTIFIER)) {
            for (String v : values) {
                if (v.equalsIgnoreCase(tokens.peek().getValue())) {
                    tokens.skip();
                    return v;
                }
            }
        }
        tokens.error(ErrorType.INVALID,name + " id");
        return values[0];
    }

    public static final String[] gamemodes = new String[]{"survival","creative","adventure","spectator"};
    private static final String[] difficulties = new String[]{"peaceful","easy","normal","hard"};
    public static final Map<String,Integer> INVENTORY_SIZES = new HashMap<String, Integer>(){{
        put("inventory",27);
        put("hotbar",9);
        put("container",54);
        put("enderchest",27);
        put("horse",15);
        put("villager",8);
    }};
    public static final Map<String,String> ARMOR_SLOT_NAMES = new HashMap<String, String>(){{
        put("chestplate","chest");
        put("boots","feet");
        put("leggings","legs");
        put("helmet","head");
    }};

    public static String parseResourceLocation(boolean taggable) {
        String loc = "";
        if (taggable && tokens.skip("#")) {
            loc += "#";
        }
        loc += tokens.expect(TokenType.IDENTIFIER,"resource location ID");
        boolean checkPath = false;
        if (tokens.skip(":")) {
            loc += ":";
            checkPath = true;
        }
        if (tokens.skip("/") || checkPath) {
            if (!checkPath) {
                loc += "/";
            }
            while (true) {
                loc += tokens.expect(TokenType.IDENTIFIER,"resource location path node");
                if (tokens.skip("/")) {
                    loc += "/";
                } else {
                    break;
                }
            }
        }
        return loc;
    }

    public int readOptionalInt() {
        if (tokens.isNext(TokenType.INT)) return Integer.parseInt(tokens.expect(TokenType.INT,null));
        return 1;
    }

    public String parseItemId(boolean tag) {
        String id = parseResourceLocation(tag);
        if (tokens.isNext("{")) {
            id += parseNBT();
        }
        return id;
    }

    public Duration parseDuration() {
        Duration d = Duration.ofNanos(0);
        int n = Integer.parseInt(tokens.expect(TokenType.INT,"duration value"));
        if (tokens.isNext(TokenType.IDENTIFIER)) {
            while (true) {
                String unit = tokens.expect(TokenType.IDENTIFIER,"duration unit");
                switch (unit) {
                    case "s":
                    case "seconds":
                    case "secs":
                        d = d.plusSeconds(n);
                        break;
                    case "t":
                    case "ticks":
                        d = d.plusMillis(n * 50);
                        break;
                    case "m":
                    case "mins":
                    case "minutes":
                        d = d.plusMinutes(n);
                        break;
                    case "h":
                    case "hrs":
                    case "hours":
                        d = d.plusHours(n);
                        break;
                    case "d":
                    case "days":
                        d = d.plusDays(n);
                        break;
                        default:
                            tokens.pushBack();
                            compilationError(ErrorType.INVALID,"duration unit, expected one of (s/seconds/secs, t/ticks, m/mins/minutes, h/hrs/hours, d/days)");
                }
                if (tokens.isNext(TokenType.INT)) {
                    n = Integer.parseInt(tokens.expect(TokenType.INT,"duration value"));
                } else {
                    break;
                }
            }
        } else {
            d = Duration.ofSeconds(n);
        }
        return d;
    }

    private static Map<Character,Integer> romanToNumber = new HashMap<Character, Integer>(){{
        put('I',1);
        put('V',5);
        put('X',10);
        put('L',50);
        put('C',100);
        put('D',500);
        put('M',1000);
    }};

    public int readRomanNumber(String roman) {
        System.out.println(roman);
        int res = 0;
        for (int i = 0; i<roman.length(); i++) {
            int s1 = romanToNumber.get(roman.charAt(i));
            if (i+1 < roman.length()) {
                int s2 = romanToNumber.get(roman.charAt(i+1));
                if (s1 >= s2) {
                    res += s1;
                }
                else {
                    res += s2 - s1;
                    i++;
                }
            }
            else {
                res += s1;
                i++;
            }
        }
        return res;
    }

    public static final List<String> entityIds = Arrays.asList("creeper","skeleton","item","tnt","spider","zombie","ender_dragon");

    /**
     * Reads position coordinates. Joins 3 {@link #readCoordinate()} calls.
     */
    public String readPosition() {
        return readCoordinates(3);
    }

    /**
     * Reads rotation coordinates. Joins 2 {@link #readCoordinate()} calls.
     */
    private String readRotation() {
        return readCoordinates(2);
    }

    public String readCoordinates(int count) {
        String pos = "";
        for (int i = 0; i < count; i++) {
            pos += readCoordinate() + " ";
        }
        return pos.trim();
    }

    /**
     * Reads a single coordinate (absolute, relative (with ~) or rotated (with ^)
     * @return A valid coordinate string
     */
    private String readCoordinate() {
        if (tokens.skip("~")) {
            if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) return "~" + tokens.nextValue();
            return "~";
        } else if (tokens.skip("^")) {
            if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) return "^" + tokens.nextValue();
            return "^";
        } else if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) return tokens.nextValue();
        compilationError(ErrorType.INVALID,"position coordinate");
        return "0";
    }

    /**
     * Parses a block ID in the format of <code>minecraft:block[property=value]{SomeNBT:"value"}</code>
     * @param tag Whether or not to read block tags (block ids that start with #)
     * @return A valid block state selector
     */
    private String parseBlockId(boolean tag) {
        String block = parseResourceLocation(tag);
        boolean hadState = false;
        if (tokens.skip("[")) {
            block += parseState();
            hadState = true;
        }
        if (tokens.isNext("{")) {
            block += parseNBT();
        }
        if (!hadState && tokens.skip("[")) {
            block += parseState();
        }
        return block;
    }

    /**
     * Parses an NBT object. Expects to have the next token be a curly bracket '{'
     * @return A validated NBT tag compound
     */
    public String parseNBT() {
        return parseNBT(tokens);
    }

    public static String parseNBT(TokenIterator tokens) {
        tokens.expect('{');
        String nbt = "{";
        while (!tokens.isNext("}")) {
            nbt += tokens.expect(TokenType.IDENTIFIER,"NBT key");
            tokens.expect(':');
            nbt += ":";
            nbt += parseNBTValue(tokens);
            if (tokens.skip(",")) {
                nbt += ",";
            } else if (!tokens.isNext("}")) {
                tokens.error(ErrorType.EXPECTED,"} or , after NBT entry");
            }
        }
        tokens.skip();
        nbt += "}";
        return nbt;
    }

    /**
     * Parses an NBT value. This can be any valid NBT value, including string literals, numbers, arrays and NBT objects (using {@link #parseNBT()}).
     * @return A string of the valid minecraft NBT.
     */
    private String parseNBTValue() {
        return parseNBTValue(tokens);
    }

    public static String parseNBTValue(TokenIterator tokens) {
        if (tokens.isNext(TokenType.INT,TokenType.DOUBLE)) {
            String v = tokens.nextValue();
            if (tokens.isNext(TokenType.IDENTIFIER)) {
                if (tokens.isNext("d","D","s","S","F","f","B","b")) {
                    v += tokens.nextValue();
                } else {
                    tokens.error(ErrorType.INVALID,"NBT number suffix");
                }
            }
            return v;
        } else if (tokens.isNext(TokenType.STRING)) {
            return  "\"" + tokens.nextValue() + "\"";
        } else if (tokens.isNext("{")) {
            return parseNBT(tokens);
        } else if (tokens.skip("[")) {
            String arr = "[";
            while (!tokens.isNext("]")) {
                arr += parseNBTValue(tokens);
                if (tokens.skip(",")) {
                    arr += ",";
                } else if (!tokens.isNext("]")) {
                    tokens.error(ErrorType.EXPECTED,"] or , after NBT array value");
                }
            }
            tokens.skip();
            arr += "]";
            return arr;
        }
        tokens.error(ErrorType.INVALID,"NBT value");
        return "{}";
    }

    private String parseState() {
        String state = "[";
        while (!tokens.isNext("]")) {
            state += tokens.expect(TokenType.IDENTIFIER,"state property");
            tokens.expect('=');
            state += "=" + tokens.nextValue();
            if (tokens.isNext(",")) state += ",";
            if (!tokens.isNext("]",",")) compilationError(ErrorType.INVALID,"block state, expected ] or ,");
        }
        tokens.skip("]");
        state += "]";
        return state;
    }

    /**
     * @deprecated Replaced by {@link JsonTextParser#readTextComponent(Parser)}
     */
    @Deprecated
    public String readJsonText() {
        if(tokens.isNext(TokenType.STRING) || tokens.isNext("{", "[")) {
            Token t = tokens.next();

            // Handling single strings
            if(t.getType() == TokenType.STRING) return "\"" + t.getValue() + "\"";

            // Handling things in brackets
            String s = t.getValue();
            String closer = s.equals("{") ? "}" : "]";

            String out = "" + s;

            int bracket = 1;
            while(bracket > 0) {
                t = tokens.next();
                if(t.getValue().equals(s)) {
                    bracket++;
                } else if(t.getValue().equals(closer)) {
                    bracket--;
                }

                if(t.getType() != TokenType.LINE_END) {
                    String added = t.getValue();
                    if(t.getType() == TokenType.STRING) {
                        added = "\"" + added + "\"";
                    }

                    out += added;
                }
            }

            return out;
        }

        compilationError(ErrorType.INVALID,"JSON token");
        return "";
    }

    private List<String> parseIf() {
        Condition cond = parseCondition(false);
        String command = readExecuteRunCommand();
        List<String> cmds = cond.toCommandsAll(this, command);
        if (tokens.skip("else")) {
            cmds.addAll(parseElse(cond));
        } else {
            lastIf = cond;
        }
        return cmds;
    }

    private List<String> parseElse(Condition condition) {
        String command = readExecuteRunCommand();
        condition.negate();
        return condition.toCommandsAll(this,command);
    }

    private List<String> parseWhile() {
        Condition condition = parseCondition(false);
        List<String> then = parseStatement();
        String func = generateFunction(then);
        List<String> condCommands = condition.toCommandsAll(this,"function " + func);
        ctx.getFunction(func).addAll(condCommands);
        return condCommands;
    }

    /**
     * Parses a condition tree. Used for <code>execute if...</code>.
     * @return A {@link Condition} object that holds the information for generating the execute commands, using {@link Condition#toCommands(Parser, String)}.
     * @param negatable whether this condition should be negatable.
     */
    private Condition parseCondition(boolean negatable) {
        Token t = tokens.next();
        switch (t.getValue()) {
            case "!": {
                if (negatable) {
                    compilationError(ErrorType.INVALID,"exclamation point twice in a row (!!)");
                }
                boolean all = tokens.isNext("(");
                Condition c = parseCondition(true);
                if (c instanceof JoinedCondition && !all) {
                    ((JoinedCondition) c).getLeft().negate();
                } else if (c != null){
                    c.negate();
                }
                return c;
            }
            case "(": {
                Condition c = parseCondition(false);
                tokens.expect(')');
                return chainConditions(c);
            }
            case "@":
                String selector = selectors.parseSelector();
                if (tokens.skipAll(".","exists","(",")")) {
                    return chainConditions(new EntityExistsCondition(selector,false));
                }
                if (tokens.skip(".")) {
                    String obj = tokens.expect(TokenType.IDENTIFIER,"selector objective");
                    if (!hasObjective(obj)) {
                        tokens.pushBack();
                        compilationError(ErrorType.UNKNOWN,"objective for entity selector");
                    }
                    return parseScoreOperators(selector + " " + obj,false);
                }
                break;
        }
        if (negatable) compilationError(ErrorType.INVALID,"condition. This condition cannot be negated using '!'");
        switch (t.getType()) {
            case IDENTIFIER:
                if (!ctx.hasGlobal(t.getValue()) && !ctx.hasConstant(t.getValue())) {
                    tokens.pushBack();
                    compilationError(ErrorType.UNKNOWN, "constant or global " + t.getValue());
                }
                return parseScoreOperators(getVariableAccess(t.getValue()),false);
            case INT:
                return parseScoreOperators(t.getValue(),true);
                default:
                    String pos;
                    try {
                        tokens.pushBack();
                        pos = readPosition();
                    } catch (Exception e) {
                        tokens.pushBack();
                        compilationError(ErrorType.INVALID,"token in condition");
                        return Condition.DUMMY;
                    }
                    if (tokens.skip(",")) {
                        String end = readPosition();
                        boolean negate = false;
                        if (!tokens.skip("=","==") && tokens.skip("!=")) {
                            negate = true;
                        }
                        String mode = "all";
                        String dest;
                        if (tokens.skip("masked","mask")) {
                            mode = "masked";
                            tokens.expect('(');
                            dest = readPosition();
                            tokens.expect(')');
                        } else if (tokens.skip("all")){
                            tokens.expect('(');
                            dest = readPosition();
                            tokens.expect(')');
                        } else {
                            dest = readPosition();
                        }
                        return chainConditions(new BlockAreaCondition(pos,end,dest,mode,negate));
                    } else {
                        if (tokens.skip("has")) {
                            tokens.expect('(');
                            String path = tokens.expect(TokenType.STRING,"block NBT data path");
                            tokens.expect(')');
                            return chainConditions(new HasDataCondition("block",pos,path,false));
                        }
                        boolean negate = false;
                        if (!tokens.skip("=","==") && tokens.skip("!=")) {
                            negate = true;
                        }
                        String block = parseBlockId(true);
                        return chainConditions(new BlockCondition(pos, block, negate));
                    }
        }
    }

    /**
     *
     * checks if this variable is a const or a local var, and creates a &lt;name&gt; &lt;objective&gt;
     *
     */
    private String getVariableAccess(String name) {
        if (ctx.hasConstant(name)) {
            return name + " Constants";
        }
        if (ctx.hasGlobal(name)) {
            return name + " Global";
        }
        return "@s " + name;
    }

    /**
     * Parses operators after a score access, for comparing 2 score values. Used for if(condition)s
     * @param first The first score access
     * @param literal Whether this score is a literal value, aka a constant hardcoded number.
     * @return A {@link ScoreCondition} joined by the next condition.
     */
    private Condition parseScoreOperators(String first, boolean literal) {
        String op = tokens.peek().getValue();
        boolean negate = false;
        switch (op) {
            case "<":
            case "<=":
            case ">":
            case ">=":
                break;
            case "==":
                op = "=";
                break;
            case "!=":
                op = "=";
                negate = true;
                break;
                default:
                    compilationError(ErrorType.INVALID,"operator in condition");
                    return new ScoreCondition(new Value(first,literal),"noop",new Value("",false),false);
        }
        tokens.skip();
        Token secondTok = tokens.next();
        String second = "";
        boolean secondLiteral = false;
        switch (secondTok.getType()) {
            case IDENTIFIER:
                if (!ctx.hasGlobal(secondTok.getValue()) && !ctx.hasConstant(secondTok.getValue())) {
                    tokens.pushBack();
                    compilationError(ErrorType.UNKNOWN, "variable");
                }
                second = getVariableAccess(secondTok.getValue());
                break;
            case INT:
                second = secondTok.getValue();
                secondLiteral = true;
                break;
            default:
                if (secondTok.getValue().equals("@")) {
                    second = selectors.parseObjectiveSelector();
                } else {
                    compilationError(ErrorType.INVALID,"token in condition");
                }
        }

        return chainConditions(new ScoreCondition(new Value(first,literal),op,new Value(second,secondLiteral),negate));
    }

    /**
     * Chains two conditions together. Checks for logic gates.
     * @param cond The first condition
     * @return A {@link JoinedCondition} of the given condition and the next condition, or just the given condition if no logic gate token is present.
     */
    private Condition chainConditions(Condition cond) {
        String chain = tokens.peek().getValue();
        switch (chain) {
            case "&&":
            case "||":
                tokens.skip();
                return new JoinedCondition(chain,cond,parseCondition(false),false);
        }
        return cond;
    }

    /**
     * Generates a new function combining the specified commands.
     * @param commands The commands to combine
     * @return The functions name, for using with /function &lt;name&gt;
     */
    public String generateFunction(List<String> commands) {
        String s = ctx.generateFunctionName();
        ctx.addFunction(s,commands);
        return s;
    }

    /**
     * Reads the next statement, and if it contains multiple commands, will combine them into a function command.
     * @return A single command, a normal command or a /function command
     */
    public String readExecuteRunCommand() {
        List<String> statement = parseStatement();
        if (statement.isEmpty()) {
            compilationError(ErrorType.MISSING,"chained command");
            return "say Empty Statement!";
        }
        if (statement.size() > 1) {
            return "function " + generateFunction(statement);
        }
        return statement.get(0);
    }

    public String parseItemAndCount() {
        String item = parseItemId(false);
        tokens.skip("*");
        if (tokens.isNext(TokenType.INT)) {
            return item + " " + tokens.nextValue();
        }
        return item;
    }

    public boolean hasObjective(String name) {
        return ctx.objectives.contains(name);
    }

    public void createConstant(String name, int value) {
        ensureConstants();
        ctx.consts.put(name,value);
        ctx.addLoad("scoreboard players set " + name + " Constants " + value);
    }

    public void createGlobal(String name) {
        ensureGlobal();
        ctx.globals.add(name);
    }

    private boolean createdGlobal;

    private void ensureGlobal() {
        if (createdGlobal) return;
        createdGlobal = true;
        ctx.addLoad("scoreboard objectives add Global dummy");
    }

    private boolean createdConsts;

    private void ensureConstants() {
        if (createdConsts) return;
        createdConsts = true;
        ctx.addLoad("scoreboard objectives add Constants dummy");
    }

    public boolean hasTrigger(String name) {
        return ctx.triggers.contains(name);
    }

    public CompilationContext getContext() {
        return ctx;
    }
}
