package com.shinysponge.dpscript.pawser;

import com.shinybunny.utils.Array;
import com.shinybunny.utils.ListUtils;
import com.shinybunny.utils.MapBuilder;
import com.shinybunny.utils.Pair;
import com.shinybunny.utils.fs.Files;
import com.shinysponge.dpscript.entities.EntityClass;
import com.shinysponge.dpscript.entities.NBT;
import com.shinysponge.dpscript.oop.*;
import com.shinysponge.dpscript.pawser.conditions.*;
import com.shinysponge.dpscript.pawser.parsers.AdvancementParser;
import com.shinysponge.dpscript.pawser.parsers.JsonTextParser;
import com.shinysponge.dpscript.pawser.parsers.NBTDataParser;
import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.pawser.score.EntryScore;
import com.shinysponge.dpscript.pawser.score.LazyScoreValue;
import com.shinysponge.dpscript.pawser.score.Score;
import com.shinysponge.dpscript.project.CompilationContext;
import com.shinysponge.dpscript.tokenizew.*;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * This is the main class to parse the DPScript code.
 * It's very long and complex, but it works somehow.
 */
public class Parser {

    private static CompilationContext ctx;
    /**
     * The current token iterator of the current parsing file. This is the main way how to read the code.
     */
    public static TokenIterator tokens;

    private static String originalCode;

    private static Condition lastIf;
    private static Variable returnedValue;
    public static ClassInstance currentInstance;
    private static boolean switchBroken;
    private static EntryScore switchBrokenScore = Score.global("_switchBroken");

    /**
     * Initializes the Parser class with the current parsing context
     */
    public static void init(CompilationContext ctx) {
        Parser.ctx = ctx;
    }

    public static void parse(File file) {
        if (ctx == null) return;
        Parser.originalCode = Files.read(file);
        Parser.tokens = TokenIterator.from(originalCode);
        Parser.parse();
    }

    /**
     * The root function of parsing. Loops through the entire code and reads it statement by statement.
     */
    private static void parse() {
        while (tokens.hasNext()) {
            if (tokens.skip(TokenType.LINE_END)) {
                continue;
            }
            parseStatement(ScopeType.GLOBAL);
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

        Token token = null;
        CodePos pos = tokens.peek().getPos();
        if (type == null) {
            msg = description;
        } else {
            tokens.pushBack();
            token = tokens.next();
            pos = token.getPos();
            if (type == ErrorType.INVALID_STATEMENT) {
                msg = "Invalid " + description + " statement, found " + token;
            } else if (type != ErrorType.MISSING && type != ErrorType.DUPLICATE && type != ErrorType.UNKNOWN) {
                msg = type.getName() + " " + description + ", found: " + token.getValue();
            } else {
                msg = type.getName() + " " + description;
            }
        }
        ctx.addError(new CompilationError(msg,pos,token));
    }

    /**
     * Parses a single statement, or a block of statements. Will call either {@link #parseGlobal()} or {@link #parseNormal(ScopeType)} depending on the <code>scope</code>.
     * @return The command translated from the statement, or multiple commands if it was a block statement or a conditional statement with || operator/s.
     */
    public static List<String> parseStatement(ScopeType scope) {
        List<String> list = new ArrayList<>();
        if (tokens.skip(TokenType.LINE_END)) return list;
        if (scope == ScopeType.GLOBAL) {
            parseGlobal();
        } else {
            list.addAll(parseNormal(scope));
        }
        return list;
    }

    /**
     * Reads a single statement in the global (root) scope. This includes objective declarations, classes, functions, etc.
     */
    private static void parseGlobal() {
        Token t = tokens.next();
        switch (t.getValue()) {
            case "load":
                parseLoad();
            case "tick":
                parseTick();
                break;
            case "function": {
                String name = tokens.expect(TokenType.IDENTIFIER,"function name");
                if (ctx.getFunction(name) != null)
                    compilationError(ErrorType.DUPLICATE,"function " + name);
                tokens.expect('{');
                ctx.addFunction(name,parseBlock(ScopeType.NORMAL));
                break;
            }
            case "bossbar":
                String id = parseResourceLocation(false);
                String displayName = JsonTextParser.readTextComponent();
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
            case "objective": {
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
            case "class": {
                DPClass cls = ClassParser.parseClass(tokens);
                if (cls != null) {
                    ctx.classes.put(cls.getName(), cls);
                }
                break;
            }
            case "task":
            case "challenge":
            case "goal":
            case "advancement": {
                AdvancementParser.parseAdvancement(t.getValue(),tokens);
                break;
            }
            default:
                compilationError(ErrorType.INVALID_STATEMENT,"global");

        }
    }

    private static void parseLoad() {
        tokens.expect('{');
        tokens.nextLine();
        List<String> cmds = parseBlock(ScopeType.NORMAL);
        ctx.addLoadFunction("init",cmds);
    }

    private static void parseTick() {
        tokens.expect('{');
        tokens.nextLine();
        List<String> cmds = parseBlock(ScopeType.NORMAL);
        ctx.addTickFunction("loop",cmds);
    }

    /**
     * Parses a block of statements. The next token must be a open curly bracket ( { )
     * @return The list of commands compiled from the normal statements inside that block.
     */
    public static List<String> parseBlock(ScopeType scope) {
        List<String> list = new ArrayList<>();
        ctx.enterBlock();
        while (tokens.hasNext() && !tokens.isNext("}")) {
            if (tokens.skip(TokenType.LINE_END)) continue;
            list.addAll(parseStatement(scope));
            tokens.nextLine();
        }
        ctx.exitBlock();
        switchBroken = false;
        tokens.expect("}");
        return list;
    }

    /**
     * Parses a normal scope statement. This is mainly the actual minecraft commands.
     * @param scope The scope context of the statement. Can change what statements can be parsed.
     * @return The command/s compiled from the next statement.
     */
    private static List<String> parseNormal(ScopeType scope) {
        List<String> list = new ArrayList<>();
        boolean needsValue = scope == ScopeType.VALUE;
        if (needsValue) {
            returnedValue = null;
        }
        if (switchBroken) {
            compilationError(ErrorType.INVALID,"statement, unreachable code after switch break");
            return list;
        }
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
                list.addAll(parseBlock(scope));
                break;
            case "print":
                list.add("say " + tokens.expect(TokenType.STRING, "message"));
                break;
            case "if":
                list.addAll(parseIf());
                break;
            case "@":
                Pair<Selector,List<String>> pair = SelectorParser.parseSelectorAndCommand();
                if (pair.getSecond().isEmpty() && needsValue) {
                    returnedValue = new Variable(VariableType.SELECTOR,pair.getFirst());
                } else {
                    list.addAll(pair.getSecond());
                }
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
            case "new":
            case "summon": {
                String name = tokens.expect(TokenType.IDENTIFIER, "class name");
                AbstractClass c = ctx.classes.get(name);
                String entityId;
                NBT nbt = null;
                String pos;
                Selector selector;
                if (c instanceof EntityClass) {
                    EntityClass cls = ((EntityClass) c);
                    entityId = cls.getType().getId();
                    ClassInstance instance = cls.parseNewInstanceCreation();
                    pos = readPosition();
                    nbt = instance.toNBT();
                    selector = cls.createSelector(instance);
                } else {
                    tokens.pushBack();
                    entityId = parseResourceLocation(false);
                    if (!entityIds.contains(entityId.substring(entityId.indexOf(':') + 1))) {
                        compilationError(ErrorType.UNKNOWN, "entity ID");
                    }
                    pos = readPosition();
                    if (tokens.isNext("{")) {
                        nbt = NBT.parse();
                    }
                    selector = new Selector('e',MapBuilder.of("type",entityId));
                }
                String addAge = "scoreboard players add @e[type=" + entityId + "] _age 1";
                if (needsValue) {
                    ctx.ensureObjective("_age");
                    list.add(addAge);
                }
                list.add("summon " + entityId + " " + pos + (nbt == null ? "" : " " + nbt));
                if (needsValue) {
                    list.add(addAge);
                    returnedValue = new Variable(VariableType.SELECTOR,selector.toSingle().set("limit","1").set("scores","{_age=1}"));
                }
                break;
            }
            case "for": {
                Selector selector = SelectorParser.parseAnySelector(true);
                list.add("execute as " + selector + " at @s run " + readExecuteRunCommand("execute"));
                break;
            }
            case "as":
            case "at": {
                Selector selector = SelectorParser.parseAnySelector(true);
                list.add(chainExecute(value + " " + selector));
                break;
            }
            case "facing":
            case "face": {
                String args;
                if (tokens.isNext("@")) {
                    Selector selector = SelectorParser.parseAnySelector(false);
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
                if (tokens.isNext("@")) {
                    args = "as " + SelectorParser.parseAnySelector(false);
                } else {
                    args = readPosition();
                }
                list.add(chainExecute("positioned " + args));
                break;
            }
            case "rotate":
            case "rotated": {
                String args;
                if (tokens.isNext("@")) {
                    args = "as " + SelectorParser.parseAnySelector(false);
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
            case "gamerule":
                tokens.skip(".");
                list.add(GameRules.parseGameruleStatement(tokens));
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
            case "block": {
                tokens.expect('(');
                String pos = readPosition();
                tokens.expect(')');
                list.add(readBlockCommand(pos));
                break;
            }
            case "while":
                list.addAll(parseWhile());
                break;
            case "schedule":
                tokens.expect('(');
                String function = ctx.callFunction(token.getPos(),value);
                Duration duration = parseDuration();
                list.add("schedule " + function + " " + ((duration.getSeconds() * 20) + (duration.getNano() / 50_000_000)));
                break;
            case "repeat":
                if (tokens.isNext(TokenType.INT)) {
                    int count = tokens.readLiteralInt();
                    List<String> commands = parseStatement(ScopeType.NORMAL);
                    for (int i = 0; i < count; i++) {
                        list.addAll(commands);
                        list.add("");
                    }
                } else {
                    Variable var = parseVariable();
                    ctx.ensureGlobal();
                    list.add("scoreboard players set _i Global 0");
                    String func = generateFunction("repeat",ListUtils.add(parseStatement(ScopeType.NORMAL),"scoreboard players add _i Global 1"));
                    List<String> condCommands = new ScoreCondition(Score.global("_i"),"<",var.get(Score.class),false).toCommandsAll("function " + func);
                    ctx.getFunction(func).addAll(condCommands);
                    list.addAll(condCommands);
                }
                break;
            case "count":
                Selector selector = SelectorParser.parseAnySelector(true);
                list.add("execute if entity " + selector);
                break;
            case "switch":
                Conditionable c = Conditionable.parse(tokens);
                if (c != null) {
                    list.addAll(readSwitchBlock(c));
                }
                break;
            case "break":
                if (scope == ScopeType.SWITCH_CASE) {
                    list.add("scoreboard players set " + switchBrokenScore + " 1");
                    switchBroken = true;
                    break;
                }
            default:
                if (tokens.skipAll("(",")")) {
                    list.add(ctx.callFunction(token.getPos(),value));
                } else if (ctx.hasGlobal(value)) {
                    list.add(SelectorParser.parseScoreOperators(getScoreAccess(value)));
                } else if (ctx.bossbars.contains(value)) {
                    list.add(parseBossbarCommand(value));
                } else if (ctx.getVariable(value) != null) {
                    Variable var = ctx.getVariable(value);
                    if (tokens.skip("=")) {
                        list.addAll(parseStatement(ScopeType.VALUE));
                        if (var.getType() == VariableType.SCORE) {
                            createGlobal(value);
                            var.get(LazyScoreValue.class).storeValue(list,Score.global(value), 1);
                        }
                        ctx.putVariable(value,var);
                    } else if (var.getType().getAccessParser() != null){
                        list.addAll(var.getType().getAccessParser().apply(var.getLazyValue(),value));
                    } else {
                        compilationError(null,"Invalid variable access");
                    }
                } else if (tokens.skip("=")) {
                    list.addAll(parseStatement(ScopeType.VALUE));
                    if (returnedValue != null) {
                        if (returnedValue.getType() == VariableType.SCORE) {
                            createGlobal(value);
                            returnedValue.get(LazyScoreValue.class).storeValue(list,Score.global(value), 1);
                        }
                        ctx.putVariable(value,returnedValue);
                    }
                } else if (needsValue) {
                    tokens.pushBack();
                    returnedValue = new Variable(VariableType.SCORE,LazyScoreValue.parseExpression());
                } else {
                    compilationError(ErrorType.INVALID_STATEMENT,"function");
                }
        }
        if (needsValue && returnedValue == null) {
            compilationError(ErrorType.INVALID_STATEMENT,"value");
        }
        return list;
    }

    private static List<String> readSwitchBlock(Conditionable value) {
        List<String> list = new ArrayList<>();
        EntryScore score = Score.global("_switchValue");
        value.storeValue(list,score,1);
        list.add("scoreboard players set " + switchBrokenScore + " 0");
        switchBroken = false;
        tokens.expect('{');
        ctx.enterBlock();
        while (tokens.hasNext() && !tokens.isNext("}")) {
            if (tokens.skip(TokenType.LINE_END)) continue;
            if (tokens.skip("case")) {
                ConditionOption option = value.getHolder().parseOption();
                if (option == null) {
                    compilationError(ErrorType.INVALID,"case value");
                    option = Score.of(0);
                }
                tokens.expect(":");
                List<String> block = parseCase();
                list.add("execute if score " + switchBrokenScore + " matches 0 " + value.getHolder().compareAndRun(option,score,generateFunction("case_" + option.getName(),block)));
            } else if (tokens.skip("default")) {
                tokens.expect(":");
                List<String> block = parseCase();
                list.add("execute if score " + switchBrokenScore + " matches 0 run " + generateFunction("case_default",block));
            }
        }
        ctx.exitBlock();
        list.add("scoreboard players set " + switchBrokenScore + " 0");
        tokens.expect("}");
        return list;
    }

    private static List<String> parseCase() {
        if (tokens.skip(TokenType.LINE_END)) {
            List<String> block = new ArrayList<>();
            while (!tokens.isNext("case","default","}")) {
                if (tokens.skip(TokenType.LINE_END)) continue;
                block.addAll(parseStatement(ScopeType.SWITCH_CASE));
                tokens.nextLine();
            }
            switchBroken = false;
            return block;
        } else {
            List<String> block = parseStatement(ScopeType.SWITCH_CASE);
            switchBroken = false;
            return block;
        }
    }

    private static Variable parseVariable() {
        if (tokens.skip("@")) {
            Selector selector = SelectorParser.parseSelector();
            tokens.expect(".");
            String obj = tokens.expect(TokenType.IDENTIFIER,"selector objective");
            if (!hasObjective(obj)) {
                compilationError(ErrorType.UNKNOWN,"objective " + obj + " for entity selector");
            }
            return new Variable(VariableType.SCORE,LazyValue.literal(new EntryScore(obj,selector.toString())));
        }
        String name = tokens.expect(TokenType.IDENTIFIER, "variable name");
        if (!ctx.hasConstant(name) && !ctx.hasGlobal(name)) {
            compilationError(ErrorType.UNKNOWN,"variable " + name);
        }
        return new Variable(VariableType.SCORE,LazyValue.literal(getScoreAccess(name)));
    }

    private static String readBlockCommand(String pos) {
        if (tokens.skip("=")) {
            return "setblock " + pos + " " + parseBlockId(false);
        }
        tokens.expect('.');
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
                return NBTDataParser.parse("block " + pos);
            case "container":
                tokens.expect('[');
                int slot = Integer.parseInt(tokens.expect(TokenType.INT,"a slot index in the container"));
                tokens.expect(']');
                tokens.expect('=');
                Item item = parseItemAndCount();
                return "replaceitem block " + pos + " container." + slot + " " + item;
        }
        compilationError(ErrorType.UNKNOWN,"block operation " + member);
        return "";
    }

    public static String parseNBTSource() {
        if (tokens.isNext("{")) {
            return "value " + NBT.parseValue();
        }
        String source;
        if (tokens.isNext("@") || tokens.isNext(TokenType.IDENTIFIER)) {
            source = "entity " + SelectorParser.parseAnySelector(false);
        } else {
            source = "block " + readPosition();
        }
        tokens.expect('.');
        tokens.expect('[');
        String path = tokens.expect(TokenType.STRING,"NBT path");
        tokens.expect(']');
        return "from " + source + " " + path;
    }

    /**
     * Reads a statement right after a /execute statement, to concat it with the given chain command.
     * @param chain The execute sub-command string that was just parsed, to concat to /execute [chain] run [next statement]
     * @return An execute command according to the next statement. If it was a block, will compile it into a function. Otherwise, will concat the command to the execute.
     * <br/>Will also omit the 'run execute' if the following command is an execute command too.
     */
    private static String chainExecute(String chain) {
        List<String> cmds = parseStatement(ScopeType.NORMAL);
        if (cmds.size() == 1) {
            if (cmds.get(0).startsWith("execute")) {
                return "execute " + chain + cmds.get(0).substring("execute".length());
            } else {
                return "execute " + chain + " run " + cmds.get(0);
            }
        } else {
            String fName = generateFunction("execute",cmds);
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
                String name = JsonTextParser.readTextComponent();
                return "bossbar set " + bossbar + " name " + name;
            case "players":
                if (tokens.skip("=")) {
                    tokens.expect('@');
                    Selector selector = SelectorParser.parseSelector();
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
        compilationError(ErrorType.UNKNOWN,"bossbar field/command");
        return "";
    }

    /**
     * Parses a value to be stored in a /execute store command.
     * If the next token is either <code>result</code> or <code>success</code>, will parse the command inside following parentheses.
     * Otherwise, will just use the next statement's result.
     * @param storeCommand The sub-command of execute store. <code>execute store (result|success) [storeCommand] run [chained command]</code>
     */
    public static String parseExecuteStore(String storeCommand) {
        String method = "result";
        String cmd;
        if (tokens.isNext("result","success")) {
            method = tokens.nextValue();
            tokens.expect('(');
            cmd = readExecuteRunCommand("execute");
            tokens.expect(')');
        } else {
            cmd = readExecuteRunCommand("execute");
        }

        return "execute store " + method + " " + storeCommand + " run " + cmd;
    }

    /**
     * Parses an identifier or an index of the identifier from the specified values. Used currently for gamemode and difficulty IDs.
     * @param tokens The TokenIterator to use. Necessary because this method is used by {@link SelectorParser#parseSelector(Token)}}.
     * @param name The name of the items. Used to throw an exception.
     * @param values The ids of the values
     * @return The matched identifier
     */
    public static String parseIdentifierOrIndex(TokenIterator tokens, String name, String... values) {
        if (tokens.isNext(TokenType.INT)) {
            int index = Integer.parseInt(tokens.expect(TokenType.INT,null));
            if (index < 0 || index > values.length) {
                tokens.error(ErrorType.INVALID,name + " index, must be between 0-3");
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

    public static int readOptionalInt() {
        if (tokens.isNext(TokenType.INT)) return Integer.parseInt(tokens.expect(TokenType.INT,null));
        return 1;
    }

    public static Item parseItem(boolean tag) {
        String id = parseResourceLocation(tag);
        NBT nbt = null;
        if (tokens.isNext("{")) {
            nbt = NBT.parse();
        }
        return new Item(id,nbt,1);
    }

    public static Duration parseDuration() {
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

    public static int readRomanNumber(String roman) {
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

    public static final List<String> entityIds = Arrays.asList("creeper","skeleton","item","tnt","spider","zombie","ender_dragon","armor_stand");

    public static final Map<String,String> posNameMap = new HashMap<String, String>() {{
        put("south","~ ~ ~1");
        put("north","~ ~ ~-1");
        put("west","~-1 ~ ~");
        put("east","~1 ~ ~");
        put("up","~ ~1 ~");
        put("down","~ ~-1 ~");
    }};

    /**
     * Reads position coordinates. Joins 3 {@link #readCoordinate()} calls.
     */
    public static String readPosition() {
        if (tokens.skip(">")) {
            String posName = tokens.expect(TokenType.IDENTIFIER,"relative position name");
            String pos = posNameMap.get(posName);
            if (pos == null) {
                Parser.compilationError(ErrorType.INVALID,"relative position name");
                return "~ ~ ~";
            }
            return pos;
        }
        return readCoordinates(3);
    }

    /**
     * Reads rotation coordinates. Joins 2 {@link #readCoordinate()} calls.
     */
    private static String readRotation() {
        return readCoordinates(2);
    }

    public static String readCoordinates(int count) {
        String[] coords = new String[count];
        for (int i = 0; i < count; i++) {
            coords[i] = readCoordinate();
        }
        return String.join(" ",coords);
    }

    /**
     * Reads a single coordinate (absolute, relative (with ~) or rotated (with ^)
     * @return A valid coordinate string
     */
    private static String readCoordinate() {
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
    public static String parseBlockId(boolean tag) {
        String block = parseResourceLocation(tag);
        boolean hadState = false;
        if (tokens.skip("[")) {
            block += parseState();
            hadState = true;
        }
        if (tokens.isNext("{")) {
            if (tokens.peek(1).getType() != TokenType.LINE_END) {
                block += NBT.parse();
            }
        }
        if (!hadState && tokens.skip("[")) {
            block += parseState();
        }
        return block;
    }

    private static String parseState() {
        String state = "[";
        while (!tokens.isNext("]")) {
            state += tokens.expect(TokenType.IDENTIFIER,"state property");
            tokens.expect('=');
            state += "=" + tokens.nextValue();
            if (tokens.isNext(",")) state += ",";
            if (!tokens.isNext("]",",")) {
                compilationError(ErrorType.INVALID,"block state, expected ] or ,");
                break;
            }
        }
        tokens.skip("]");
        state += "]";
        return state;
    }

    /**
     * Upper cases a string on every underscore (_).
     * So for example, hello_world would be converted to HelloWorld.
     */
    public static String toUpperCaseWords(String s) {
        String[] parts = s.split("_");
        String result = "";
        for (String part : parts){
            result = result + toProperCase(part);
        }
        return result;
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    private static List<String> parseIf() {
        Condition cond = parseCondition(false);
        String command = readExecuteRunCommand("if");
        List<String> cmds = cond.toCommandsAll(command);
        if (tokens.skip("else")) {
            cmds.addAll(parseElse(cond));
        } else {
            lastIf = cond;
        }
        return cmds;
    }

    private static List<String> parseElse(Condition condition) {
        String command = readExecuteRunCommand("else");
        condition.negate();
        return condition.toCommandsAll(command);
    }

    private static List<String> parseWhile() {
        Condition condition = parseCondition(false);
        List<String> then = parseStatement(ScopeType.NORMAL);
        String func = generateFunction("while",then);
        List<String> condCommands = condition.toCommandsAll("function " + func);
        ctx.getFunction(func).addAll(condCommands);
        return condCommands;
    }

    /**
     * Parses a condition tree. Used for <code>execute if...</code>.
     * @return A {@link Condition} object that holds the information for generating the execute commands, using {@link Condition#toCommands(String)}.
     * @param negatable whether this condition should be negatable.
     */
    public static Condition parseCondition(boolean negatable) {
        Token t = tokens.next();
        switch (t.getValue()) {
            case "!": {
                if (negatable) {
                    compilationError(null,"Cannot have an exclamation point twice in a row (!!)");
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
                Selector selector = SelectorParser.parseSelector();
                if (tokens.skipAll(".","exists","(",")")) {
                    return chainConditions(new EntityExistsCondition(selector,false));
                }
                if (tokens.skipAll(".","has")) {
                    tokens.expect('(');
                    String path = tokens.expect(TokenType.STRING,"entity NBT data path");
                    tokens.expect(')');
                    return chainConditions(new HasDataCondition("entity",selector.toString(),path,false));
                }
                if (tokens.skip(".")) {
                    String obj = tokens.expect(TokenType.IDENTIFIER,"selector objective");
                    if (!hasObjective(obj)) {
                        compilationError(ErrorType.UNKNOWN,"objective " + obj + " for entity selector");
                    }
                    return parseScoreOperators(new EntryScore(obj,selector.toString()));
                }
                break;
        }
        if (negatable) compilationError(ErrorType.INVALID,"condition. This condition cannot be negated using '!'");
        switch (t.getType()) {
            case IDENTIFIER:
                if (!ctx.hasGlobal(t.getValue()) && !ctx.hasConstant(t.getValue())) {
                    compilationError(ErrorType.UNKNOWN, "constant or global " + t.getValue());
                }
                return parseScoreOperators(getScoreAccess(t.getValue()));
            case INT:
                return parseScoreOperators(Score.of(Integer.parseInt(t.getValue())));
                default:
                    String pos;
                    tokens.pushBack();
                    if (tokens.isNext("~","^",">") || tokens.isNext(TokenType.INT,TokenType.DOUBLE)) {
                        pos = readPosition();
                        if (tokens.skip(",")) {
                            String end = readPosition();
                            boolean negate = false;
                            if (!tokens.skip("=", "==") && tokens.skip("!=")) {
                                negate = true;
                            }
                            String mode = "all";
                            String dest;
                            if (tokens.skip("masked", "mask")) {
                                mode = "masked";
                                tokens.expect('(');
                                dest = readPosition();
                                tokens.expect(')');
                            } else if (tokens.skip("all")) {
                                tokens.expect('(');
                                dest = readPosition();
                                tokens.expect(')');
                            } else {
                                dest = readPosition();
                            }
                            return chainConditions(new BlockAreaCondition(pos, end, dest, mode, negate));
                        } else {
                            if (tokens.skip("has")) {
                                tokens.expect('(');
                                String path = tokens.expect(TokenType.STRING, "block NBT data path");
                                tokens.expect(')');
                                return chainConditions(new HasDataCondition("block", pos, path, false));
                            }
                            boolean negate = false;
                            if (!tokens.skip("=", "==") && tokens.skip("!=")) {
                                negate = true;
                            }
                            String block = parseBlockId(true);
                            return chainConditions(new BlockCondition(pos, block, negate));
                        }
                    } else {
                        compilationError(ErrorType.INVALID,"token in condition");
                        return Condition.DUMMY;
                    }
        }
    }

    /**
     *
     * checks if this variable is a const or a local var, and creates a &lt;name&gt; &lt;objective&gt;
     *
     */
    public static EntryScore getScoreAccess(String name) {
        if (ctx.hasConstant(name)) {
            return Score.constant(name);
        }
        if (ctx.hasGlobal(name)) {
            return Score.global(name);
        }
        return new EntryScore(name,"@s");
    }

    /**
     * Parses operators after a score access, for comparing 2 score values. Used for if(condition)s
     * @param first The first score
     * @return A {@link ScoreCondition} joined by the next condition.
     */
    private static Condition parseScoreOperators(Score first) {
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
                    return new ScoreCondition(first,"noop",Score.of(0),false);
        }
        tokens.skip();
        Score second = Score.of(0);
        switch (tokens.peek().getType()) {
            case IDENTIFIER:
                Token t = tokens.next();
                if (!ctx.hasGlobal(t.getValue()) && !ctx.hasConstant(t.getValue())) {
                    compilationError(ErrorType.UNKNOWN, "variable");
                }
                second = getScoreAccess(t.getValue());
                break;
            case INT:
                second = Score.of(tokens.readLiteralInt());
                break;
            default:
                if (tokens.skip("@")) {
                    second = SelectorParser.parseObjectiveSelector();
                } else {
                    compilationError(ErrorType.INVALID,"token in condition");
                }
        }

        return chainConditions(new ScoreCondition(first,op,second,negate));
    }

    /**
     * Chains two conditions together. Checks for logic gates.
     * @param cond The first condition
     * @return A {@link JoinedCondition} of the given condition and the next condition, or just the given condition if no logic gate token is present.
     */
    private static Condition chainConditions(Condition cond) {
        String chain = tokens.peek().getValue();
        switch (chain) {
            case "&&":
            case "||":
                tokens.skip();
                return new JoinedCondition(chain,cond,parseCondition(false),false);
        }
        return cond;
    }

    public static <T> List<T> readList(char open, char close, Function<TokenIterator,T> valueParser) {
        List<T> list = new ArrayList<>();
        tokens.expect(open);
        while (tokens.hasNext() && !tokens.isNext(close + "")) {
            list.add(valueParser.apply(tokens));
            if (!tokens.skip(",") && !tokens.isNext(close + "")) {
                compilationError(ErrorType.EXPECTED,", or " + close + " to end list");
                return list;
            }
        }
        tokens.expect(close);
        return list;
    }

    public static <T> LazyValue<List<T>> readLazyList(char open, char close, Function<TokenIterator,LazyValue<T>> valueParser) {
        List<LazyValue<T>> list = new ArrayList<>();
        tokens.expect(open);
        while (tokens.hasNext() && !tokens.isNext(close + "")) {
            list.add(valueParser.apply(tokens));
            if (!tokens.skip(",") && !tokens.isNext(close + "")) {
                compilationError(ErrorType.EXPECTED,", or " + close + " to end list");
            }
        }
        tokens.expect(close);
        return LazyValue.of(()->{
            List<T> actual = new ArrayList<>();
            for (LazyValue<T> lazy : list) {
                actual.add(lazy.eval());
            }
            return actual;
        },null);
    }


    public static LazyValue<String> readJsonText() {
        if (tokens.isNext(TokenType.STRING)) {
            return LazyValue.of(()->JsonTextParser.readTextComponent(tokens.nextValue()),DPClass.STRING);
        }
        return LazyValue.of(JsonTextParser::readTextComponent,DPClass.STRING);
    }



    /**
     * Generates a new function combining the specified commands.
     * @param type The name of the function. Will add an index to the name to avoid generating functions with the same name.
     * @param commands The commands to include
     * @return The functions identifier, for using with /function &lt;location&gt;
     */
    public static String generateFunction(String type, List<String> commands) {
        String s = ctx.generateFunctionName(type);
        ctx.addFunction(s,commands);
        return ctx.getNamespace() + ":" + s;
    }

    /**
     * Reads the next statement, and if it is a block statement, will combine them into a function command.
     * @return A single command or a /function command referring to multiple commands
     */
    public static String readExecuteRunCommand(String type) {
        List<String> statement = parseStatement(ScopeType.NORMAL);
        if (statement.isEmpty()) {
            return "say Empty Statement!";
        }
        if (statement.size() > 1) {
            return "function " + generateFunction(type,statement);
        }
        return statement.get(0);
    }

    public static Item parseItemAndCount() {
        Item item = parseItem(false);
        tokens.skip("*");
        if (tokens.isNext(TokenType.INT)) {
            return new Item(item.getId(),item.getTag(),Integer.parseInt(tokens.nextValue()));
        }
        return item;
    }

    public static boolean hasObjective(String name) {
        return ctx.objectives.contains(name);
    }

    public static void createConstant(String name, int value) {
        ctx.ensureConstants();
        ctx.consts.put(name,value);
        ctx.addLoad("scoreboard players set " + name + " Consts " + value);
    }

    public static void createGlobal(String name) {
        ctx.ensureGlobal();
        ctx.globals.add(name);
    }

    public static boolean hasTrigger(String name) {
        return ctx.triggers.contains(name);
    }

    public static CompilationContext getContext() {
        return ctx;
    }


}
