package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.project.MCFunction;
import com.shinysponge.dpscript.tokenizew.TokenIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PrimitiveClass<T> extends AbstractClass {
    private final Class<T> clazz;
    private Function<TokenIterator, T> literalParser;
    private PrimitiveClass<?> superClass;

    public PrimitiveClass(Class<T> clazz, Function<TokenIterator,T> literalParser) {
        this(clazz,literalParser,null);
    }

    public PrimitiveClass(Class<T> clazz, Function<TokenIterator, T> literalParser, PrimitiveClass<?> superClass) {
        this.clazz = clazz;
        this.literalParser = literalParser;
        this.superClass = superClass;
    }

    public static AbstractClass of(Object literal) {
        if (literal instanceof String) return DPClass.STRING;
        if (literal instanceof Integer) return DPClass.INT;
        if (literal instanceof Boolean) return DPClass.BOOLEAN;
        if (literal instanceof Double) return DPClass.DOUBLE;
        return DPClass.OBJECT;
    }

    @Override
    public AbstractClass getSuperClass() {
        return superClass;
    }

    @Override
    public List<DPField> getFields() {
        return new ArrayList<>();
    }

    @Override
    public List<MCFunction> getFunctions() {
        return new ArrayList<>();
    }

    @Override
    public Object parseLiteral(TokenIterator tokens) {
        return this.literalParser.apply(tokens);
    }

    @Override
    public String getName() {
        return clazz.getSimpleName();
    }

    @Override
    public DPClass createSubclass(String name, List<LazyValue<?>> superCall, DPParameter... params) {
        return null;
    }

    @Override
    public boolean isInstance(Object obj) {
        if (obj instanceof LazyValue) {
            return isSuperOrSameAs(((LazyValue) obj).getType());
        }
        return obj != null && clazz.isAssignableFrom(obj.getClass());
    }

    @Override
    public Object dummyInstance() {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
