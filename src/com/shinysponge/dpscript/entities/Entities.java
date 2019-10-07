package com.shinysponge.dpscript.entities;

import com.shinysponge.dpscript.pawser.parsers.EffectParser;

import java.util.Collections;
import java.util.function.Function;

public enum Entities {
    ENTITY(Tags.MOTION,Tags.ROTATION,Tag.bool("Invulnerable"),Tag.bool("NoGravity"),Tag.bool("Silent"),Tag.bool("Glowing"),Tag.text("CustomName"),Tag.bool("CustomNameVisible","ShowName","DisplayName")),
    LIVING_ENTITY(ENTITY,Tags.ACTIVE_EFFECTS),
    ARMOR_STAND(LIVING_ENTITY,Tag.bool("Invisible"),Tag.bool("NoBasePlate","nobase"),Tag.bool("ShowArms","arms"),Tag.bool("Small"));

    Entities(Tag... tags) {
        this(null,tags);
    }

    Entities(Entities parent, Tag... tags) {

    }

    public interface Tag {

        static Tag bool(String key, String... aliases) {
            return new SimpleTag(key,aliases, NBTReader::readBoolean);
        }

        static Tag text(String key, String... aliases) {
            return new SimpleTag(key,aliases, NBTReader::readJsonText);
        }

        String getKey();

        String[] getAliases();

        Object parse(NBTReader reader, String key);

    }

    public enum Tags implements Tag {
        ACTIVE_EFFECTS("ActiveEffects","effects","effect") {
            @Override
            public Object parse(NBTReader reader, String key) {
                if (key.equalsIgnoreCase("effect")) {
                    NBT effect = EffectParser.parseEffect().toNBT();
                    return Collections.singletonList(effect);
                } else {
                    return reader.readNBTList(()->EffectParser.parseEffect().toNBT());
                }
            }
        }, MOTION("Motion") {
            @Override
            public Object parse(NBTReader reader, String key) {
                return reader.readNBTList(reader::readDouble);
            }
        }, ROTATION("Rotation", "rot") {
            @Override
            public Object parse(NBTReader reader, String key) {
                return reader.readNBTList(()->Double.valueOf(reader.readDouble()).floatValue());
            }
        };

        private final String key;
        private final String[] aliases;


        Tags(String key, String... aliases) {
            this.key = key;
            this.aliases = aliases;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String[] getAliases() {
            return aliases;
        }

        public abstract Object parse(NBTReader reader, String key);
    }

    public static class SimpleTag implements Tag {

        private final String key;
        private final String[] aliases;
        private final Function<NBTReader,Object> parser;

        public SimpleTag(String key, String[] aliases, Function<NBTReader, Object> parser) {
            this.key = key;
            this.aliases = aliases;
            this.parser = parser;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String[] getAliases() {
            return aliases;
        }

        @Override
        public Object parse(NBTReader reader, String key) {
            return parser.apply(reader);
        }
    }
}
