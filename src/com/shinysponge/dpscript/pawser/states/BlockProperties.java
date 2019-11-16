package com.shinysponge.dpscript.pawser.states;

import com.shinybunny.utils.ListUtils;
import com.shinysponge.dpscript.pawser.ConditionHolder;
import com.shinysponge.dpscript.pawser.ConditionOption;
import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.score.EntryScore;

import java.util.Arrays;

public enum BlockProperties implements ConditionHolder<BlockProperty> {
    FACING(Direction.class);

    private final BlockProperty[] options;

    <E extends Enum<E> & BlockProperty> BlockProperties(Class<E> cls) {
        this(cls.getEnumConstants());
    }

    BlockProperties(BlockProperty... options) {
        this.options = options;
    }

    public static BlockProperties get(String key) {
        for (BlockProperties p : values()) {
            if (p.getName().equalsIgnoreCase(key)) {
                return p;
            }
        }
        Parser.compilationError(ErrorType.UNKNOWN,"property " + key);
        return null;
    }

    public ConditionOption[] getOptions() {
        return options;
    }

    public BlockProperty getOption(int index) {
        return options[index];
    }

    @Override
    public String compareAndRun(BlockProperty option, EntryScore value, String command) {
        return "execute if score " + value + " matches " + indexOf(option) + " run " + command;
    }

    public int indexOf(BlockProperty option) {
        return ListUtils.arrayIndexOf(options,option);
    }

    @Override
    public BlockProperty parseOption() {
        String s = Parser.tokens.expect(Arrays.stream(options).map(ConditionOption::getName).toArray(String[]::new));
        return ListUtils.firstMatch(options,o->o.getName().equalsIgnoreCase(s));
    }

    public String getName() {
        return name().toLowerCase();
    }

    public enum Direction implements BlockProperty {
        SOUTH("~ ~ ~1"),
        NORTH("~ ~ ~-1"),
        EAST("~1 ~ ~"),
        WEST("~-1 ~ ~"),
        UP("~ ~1 ~"),
        DOWN("~ ~-1 ~");

        private String coords;

        Direction(String coords) {
            this.coords = coords;
        }

        @Override
        public String getName() {
            return name().toLowerCase();
        }

        public String getRelativeCoords() {
            return coords;
        }
    }

}
