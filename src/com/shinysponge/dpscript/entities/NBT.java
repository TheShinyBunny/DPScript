package com.shinysponge.dpscript.entities;

import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.Token;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class NBT {

    private Map<String, Object> entries = new HashMap<>();

    public NBT() {
    }

    public NBT(Object model) {
        try {
            for (Field f : model.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Key k = f.getAnnotation(Key.class);
                if (k != null) {
                    entries.put(k.value(), f.get(model));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static NBT parse() {
        return parseNBT(Parser.tokens);
    }

    public static Object parseValue() {
        return parseNBTValue(Parser.tokens);
    }

    public NBT set(String key, int value) {
        return put(key,value);
    }

    public NBT set(String key, double value) {
        return put(key,value);
    }

    public NBT set(String key, float value) {
        return put(key,value);
    }

    public NBT set(String key, short value) {
        return put(key,value);
    }

    public NBT set(String key, long value) {
        return put(key,value);
    }

    public NBT set(String key, byte value) {
        return put(key,value);
    }

    public NBT set(String key, String value) {
        return put(key,value);
    }

    public NBT set(String key, NBT value) {
        return put(key,value);
    }

    public NBT set(String key, List<?> value) {
        return put(key,value);
    }

    public NBT put(String key, Object value) {
        this.entries.put(key,value);
        return this;
    }

    public NBT set(String key, boolean value) {
        return set(key,(byte)(value ? 1 : 0));
    }

    @Override
    public String toString() {
        return entries.entrySet().stream().map(e->e.getKey() + ":" + stringify(e.getValue())).collect(Collectors.joining(",","{","}"));
    }

    private String stringify(Object value) {
        if (value instanceof Double) {
            return value + "d";
        }
        if (value instanceof Float) {
            return value + "F";
        }
        if (value instanceof Long) {
            return value + "L";
        }
        if (value instanceof Short) {
            return value + "s";
        }
        if (value instanceof Byte) {
            return value + "b";
        }
        if (value instanceof Boolean) {
            return ((boolean)value) ? "1b" : "0b";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Collection) {
            return ((Collection<?>)value).stream().map(this::stringify).collect(Collectors.joining(",","[","]"));
        }
        return String.valueOf(value);
    }

    /**
     * Parses an NBT Tag Compound. The next token must be '{'
     * @return A string representation of the NBT combining the tokens that build up the NBT tag.
     */
    public static NBT parseNBT(TokenIterator tokens) {
        tokens.expect('{');
        NBT nbt = new NBT();
        while (!tokens.isNext("}")) {
            String key = tokens.expect(TokenType.IDENTIFIER,"NBT key");
            tokens.expect(':');
            Object value = parseNBTValue(tokens);
            nbt.put(key,value);
            if (!tokens.skip(",") && !tokens.isNext("}")) {
                tokens.error(ErrorType.EXPECTED,"} or , after NBT entry");
                break;
            }
        }
        tokens.skip();
        return nbt;
    }

    /**
     * Parses an NBT value. This can be any valid NBT value, including string literals, numbers, arrays and NBT objects (using {@link #parseNBT()}).
     * @return A string of the valid minecraft NBT.
     */
    public static Object parseNBTValue(TokenIterator tokens) {
        if (tokens.isNext(TokenType.INT,TokenType.DOUBLE)) {
            TokenType t = tokens.peek().getType();
            String v = tokens.nextValue();
            if (tokens.isNext(TokenType.IDENTIFIER)) {
                if (!tokens.skip("d", "D", "s", "S", "F", "f", "B", "b")) {
                    tokens.error(ErrorType.INVALID,"NBT number suffix");
                }
            }
            return t == TokenType.INT ? Integer.parseInt(v) : Double.parseDouble(v);
        } else if (tokens.isNext(TokenType.STRING)) {
            return  tokens.nextValue();
        } else if (tokens.isNext("{")) {
            return parseNBT(tokens);
        } else if (tokens.skip("[")) {
            List<Object> arr = new ArrayList<>();
            while (!tokens.isNext("]")) {
                arr.add(parseNBTValue(tokens));
                if (!tokens.skip(",") && !tokens.isNext("]")) {
                    tokens.error(ErrorType.EXPECTED,"] or , after NBT array value");
                    break;
                }
            }
            tokens.skip();
            return arr;
        }
        tokens.error(ErrorType.INVALID,"NBT value");
        return new NBT();
    }

}
