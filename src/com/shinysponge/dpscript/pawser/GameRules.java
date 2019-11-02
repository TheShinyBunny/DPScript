package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.function.Function;

public enum GameRules {
    ANNOUNCE_ADVANCEMENTS(true,"advancement_popup","show_advancements"),
    COMMAND_BLOCK_OUTPUT(true,"commands_messages"),
    DISABLE_ELYTRA_MOVEMENT_CHECK(false,"elytra_check","elytra_anticheat"),
    DISABLE_RAIDS(false,"raids"),
    DO_DAYLIGHT_CYCLE(true,"daylight_cycle"),
    DO_ENTITY_DROPS(true,"entity_drops"),
    DO_FIRE_TICK(true,"fire_tick","fire_spread"),
    DO_INSOMNIA(true,"phantoms","spawn_phantoms","insomnia"),
    DO_IMMEDIATE_RESPAWN(false,"immediate_respawn"),
    DO_MOB_LOOT(true,"mob_loot","mob_drops"),
    DO_MOB_SPAWNING(true,"mob_spawning","spawn_mobs"),
    DO_TILE_DROPS(true,"tile_drops","block_drops"),
    DO_WEATHER_CYCLE(true,"weather_cycle","weather"),
    DROWNING_DAMAGE(true),
    FALL_DAMAGE(true),
    FIRE_DAMAGE(true),
    KEEP_INVENTORY(false),
    LOG_ADMIN_COMMANDS(true,"log_commands"),
    MAX_COMMAND_CHAIN_LENGTH(65536,"command_chain_length","command_chain"),
    MAX_ENTITY_CRAMMING(24,"entity_cramming"),
    MOB_GRIEFING(true),
    NATURAL_REGENERATION(true,"regeneration"),
    RANDOM_TICK_SPEED(3,"random_ticks"),
    REDUCED_DEBUG_INFO(false,"no_coordinates","less_debug"),
    SEND_COMMAND_FEEDBACK(true,"command_feedback"),
    SHOW_DEATH_MESSAGES(true,"death_messages"),
    SPAWN_RADIUS(10),
    SPECTATORS_GENERATE_CHUNKS(true,"spectator_chunks");

    private final Function<TokenIterator, String> parser;
    private final String[] aliases;
    private Object defaultValue;

    GameRules(boolean defBool, String... aliases) {
        this.parser = (tokens)->tokens.expect("true","false");
        this.defaultValue = defBool;
        this.aliases = aliases;
    }

    GameRules(int defInt, String... aliases) {
        this.parser = (tokens)->tokens.expect(TokenType.INT,"integer rule value");
        this.defaultValue = defInt;
        this.aliases = aliases;
    }

    public static String parseGameruleStatement(TokenIterator tokens) {
        String name = tokens.expect(TokenType.IDENTIFIER,"gamerule ID");
        GameRules rule = null;
        for (GameRules gr : values()) {
            if (gr.name().equalsIgnoreCase(name) || Parser.toUpperCaseWords(gr.name()).equalsIgnoreCase(name)) {
                rule = gr;
                break;
            }
            for (String a : gr.aliases) {
                if (a.equalsIgnoreCase(name) || Parser.toUpperCaseWords(gr.name()).equalsIgnoreCase(name)) {
                    rule = gr;
                    break;
                }
            }
            if (rule != null) break;
        }
        if (rule == null) {
            Parser.compilationError(ErrorType.UNKNOWN,"gamerule ID " + name);
            return "gamerule unknownRule";
        }
        String camelName = Parser.toUpperCaseWords(rule.name());
        if (tokens.skip("=")) {
            String value = rule.parser.apply(tokens);
            return "gamerule " + camelName + " " + value;
        }
        if (tokens.skipAll(".","reset","(",")")) {
            return "gamerule " + camelName + " " + rule.defaultValue;
        }
        return "gamerule " + camelName;
    }
}
