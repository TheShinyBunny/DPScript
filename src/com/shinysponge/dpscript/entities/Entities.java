package com.shinysponge.dpscript.entities;

import com.shinysponge.dpscript.oop.DPField;
import com.shinysponge.dpscript.oop.LazyValue;
import com.shinysponge.dpscript.pawser.Item;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.parsers.EffectParser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;

import java.util.Collections;
import java.util.function.Function;

public enum Entities {
    ENTITY(false,Tags.MOTION,Tags.ROTATION,Tag.bool("Invulnerable"),Tag.bool("NoGravity"),Tag.bool("Silent"),Tag.bool("Glowing"),Tag.text("CustomName"),Tag.bool("CustomNameVisible","ShowName","DisplayName")),
    LIVING_ENTITY(false,ENTITY,Tags.ACTIVE_EFFECTS),
    ARMOR_STAND(LIVING_ENTITY,Tag.bool("Invisible"),Tag.bool("NoBasePlate","nobase"),Tag.invertedBool("NoBasePlate","base","baseplate"),Tag.bool("ShowArms","arms"),Tag.bool("Small")),
    ITEM(ENTITY,Tag.integer("Age"),Tags.NO_DESPAWN_ITEM,Tag.integer("Health","HP"),Tag.integer("PickupDelay","delay","pickup_delay"),Tags.ITEM);

    private boolean spawnable;
    private Entities parent;
    private EntityClass entityClass;

    Entities(boolean spawnable, Entities parent, DPField... fields) {
        this.spawnable = spawnable;
        this.parent = parent;
        this.entityClass = new EntityClass(Parser.toUpperCaseWords(name()),parent == null ? null : parent.entityClass,this);
        this.entityClass.addFields(fields);
    }

    Entities(boolean spawnable, DPField... fields) {
        this(spawnable,null,fields);
    }

    Entities(Entities parent, DPField... fields) {
        this(false,parent,fields);
    }


    public static Entities getByID(String id) {
        for (Entities e : values()) {
            if (e.spawnable && (e.name().equalsIgnoreCase(id) || Parser.toUpperCaseWords(e.name()).equalsIgnoreCase(id))) {
                return e;
            }
        }
        return null;
    }

    public String getId() {
        return name().toLowerCase();
    }

    public EntityClass getTypeClass() {
        return entityClass;
    }

    public static class Tag implements DPField {

        private final String key;
        private final String[] aliases;
        private final Function<TokenIterator,LazyValue<?>> parser;
        private final boolean useKeyAsAlias;

        public Tag(String key, String[] aliases, Function<TokenIterator, LazyValue<?>> parser) {
            this(key,aliases,parser,true);
        }

        public Tag(String key, String[] aliases, Function<TokenIterator, LazyValue<?>> parser, boolean useKeyAsAlias) {
            this.key = key;
            this.aliases = aliases;
            this.parser = parser;
            this.useKeyAsAlias = useKeyAsAlias;
        }

        public static Tag bool(String key, String... aliases) {
            return new Tag(key,aliases, TokenIterator::readBoolean);
        }

        public static Tag text(String key, String... aliases) {
            return new Tag(key,aliases, (t)->Parser.readJsonText());
        }

        public static Tag invertedBool(String original, String... aliases) {
            return new Tag(original,aliases,t-> t.readBoolean().map(b->!b),false);
        }

        public static Tag integer(String key, String... aliases) {
            return new Tag(key,aliases, TokenIterator::readInt);
        }

        @Override
        public boolean useKeyAsAlias() {
            return useKeyAsAlias;
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
        public LazyValue<?> parse(TokenIterator tokens, String key) {
            return parser.apply(tokens);
        }
    }

    public enum Tags implements DPField {
        ACTIVE_EFFECTS("ActiveEffects","effects","effect","active_effects") {
            @Override
            public LazyValue<?> parse(TokenIterator tokens, String key) {
                if (key.equalsIgnoreCase("effect")) {
                    NBT effect = EffectParser.parseEffect().toNBT();
                    return LazyValue.literal(Collections.singletonList(effect));
                } else {
                    return Parser.readLazyList('[',']',t->LazyValue.literal(EffectParser.parseEffect().toNBT()));
                }
            }
        }, MOTION("Motion") {
            @Override
            public LazyValue<?> parse(TokenIterator tokens, String key) {
                return Parser.readLazyList('[',']', TokenIterator::readDouble);
            }
        }, ROTATION("Rotation", "rot") {
            @Override
            public LazyValue<?> parse(TokenIterator reader, String key) {
                return Parser.readLazyList('[',']',t->reader.readDouble().map(Double::floatValue));
            }
        },
        NO_DESPAWN_ITEM("Age","despawn","CanDespawn","can_despawn") {
            @Override
            public LazyValue<?> parse(TokenIterator reader, String key) {
                return reader.readBoolean().map(b->b ? 0 : -32768);
            }

            @Override
            public boolean useKeyAsAlias() {
                return false;
            }
        },
        ITEM("Item","stack","item_stack","ItemStack") {
            @Override
            public LazyValue<?> parse(TokenIterator tokens, String key) {
                Item item = Parser.parseItemAndCount();
                return LazyValue.literal(item.toNBT());
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

        public abstract LazyValue<?> parse(TokenIterator tokens, String key);
    }
}
