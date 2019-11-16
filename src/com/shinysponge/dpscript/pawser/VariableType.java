package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.oop.ClassInstance;
import com.shinysponge.dpscript.oop.LazyValue;
import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.pawser.score.LazyScoreValue;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum VariableType {
    SELECTOR(Selector.class, LazyValue.toLazyConsumer(SelectorParser::parseSelectorCommand)),
    INTEGER(Integer.class, (i,n)-> Collections.singletonList(SelectorParser.parseScoreOperators(Parser.getScoreAccess(n)))),
    ENUM(Enum.class),
    STRING(String.class),
    SCORE(LazyScoreValue.class),
    OBJECT(ClassInstance.class);

    private Class<?> valueType;
    private BiFunction<LazyValue<?>, String, List<String>> accessParser;

    <T> VariableType(Class<T> valueType, Function<LazyValue<T>, List<String>> accessParser) {
        this.valueType = valueType;
        this.accessParser = (l,n)->accessParser.apply((LazyValue<T>) l);
    }

    <T> VariableType(Class<T> valueType, BiFunction<LazyValue<T>, String, List<String>> accessParser) {
        this.valueType = valueType;
        this.accessParser = (l,n)->accessParser.apply((LazyValue<T>)l,n);
    }

    VariableType(Class<?> valueType) {
        this.valueType = valueType;
    }

    public BiFunction<LazyValue<?>, String, List<String>> getAccessParser() {
        return accessParser;
    }

    public static VariableType get(Object value) {
        for (VariableType t : values()) {
            if (t.valueType.isInstance(value)) {
                return t;
            }
        }
        return null;
    }
}
