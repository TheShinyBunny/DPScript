package com.shinysponge.dpscript.pawser;

import java.util.Arrays;
import java.util.List;

public enum Enchantments {
    AQUA_AFFINITY(1),
    BANE_OF_ARTHROPODS(5),
    BLAST_PROTECTION(4,"blast_prot"),
    CHANNELING(1),
    BINDING_CURSE(1,"curse_of_binding"),
    VANISHING_CURSE(1,"curse_of_vanishing"),
    DEPTH_STRIDER(3),
    EFFICIENCY(5),
    FEATHER_FALLING(4),
    FIRE_ASPECT(2),
    FIRE_PROTECTION(4,"fire_prot"),
    FLAME(1),
    FORTUNE(3),
    FROST_WALKER(2),
    IMPALING(5),
    INFINITY(1),
    KNOCKBACK(2,"knock_back"),
    LOOTING(3),
    LOYALTY(3),
    LUCK_OF_THE_SEA(3),
    LURE(3),
    MENDING(1),
    MULTISHOT(1,"multi_shot"),
    PIERCING(4),
    POWER(5),
    PROJECTILE_PROTECTION(4,"projectile_prot","proj_prot"),
    PROTECTION(4,"prot"),
    PUNCH(2),
    QUICK_CHARGE(3,"quickcharge"),
    RESPIRATION(3),
    RIPTIDE(3),
    SHARPNESS(5,"sharp"),
    SILK_TOUCH(1,"silktouch"),
    SMITE(5),
    SWEEPING(3,"sweeping_edge"),
    THORNS(3),
    UNBREAKING(3);

    private final int maxLevel;
    private final List<String> aliases;

    Enchantments(int maxLevel, String... aliases) {
        this.maxLevel = maxLevel;
        this.aliases = Arrays.asList(aliases);
    }

    public static Enchantments get(String enchID) {
        String name = enchID.substring(enchID.indexOf(':')+1);
        for (Enchantments e : values()) {
            if (e.name().equalsIgnoreCase(name)) return e;
            for (String a : e.aliases) {
                if (a.equalsIgnoreCase(name)) return e;
            }
        }
        return null;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}
