package com.shinysponge.dpscript.entities;

import com.shinysponge.dpscript.pawser.parsers.EffectParser;

public enum Entities {
    LIVING_ENTITY(Tags.ACTIVE_EFFECTS),
    ARMOR_STAND();


    public enum Tags {
        ACTIVE_EFFECTS("ActiveEffects","effects","effect") {
            @Override
            public void parse(NBTReader reader, String key, NBT nbt) {
                if (key.equalsIgnoreCase("effect")) {
                    NBT effect = EffectParser.parseEffect(reader.parser).toNBT();
                }
            }
        };

        Tags(String... keys) {

        }

        public abstract void parse(NBTReader reader, String key, NBT nbt);
    }
}
