package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.project.MCFunction;
import com.shinysponge.dpscript.tokenizew.TokenIterator;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractClass {

    public DPField findField(String key) {
        for (DPField f : getFields()) {
            if (f.isKey(key)) {
                return f;
            }
        }
        if (getSuperClass() != null) {
            return getSuperClass().findField(key);
        }
        return null;
    }

    public abstract AbstractClass getSuperClass();

    public abstract List<DPField> getFields();

    public abstract List<MCFunction> getFunctions();

    public abstract Object parseLiteral(TokenIterator tokens);

    public abstract String getName();

    public abstract DPClass createSubclass(String name, List<LazyValue<?>> superCall, DPParameter... params);

    public ParameterList getConstructor() {
        return new ParameterList();
    }

    public MCFunction findFunction(String name) {
        for (MCFunction f : getFunctions()) {
            if (f.getId().equals(name)) {
                return f;
            }
        }
        if (getSuperClass() != null) {
            return getSuperClass().findFunction(name);
        }
        return null;
    }

    public abstract boolean isInstance(Object obj);

    public boolean isSuperOrSameAs(AbstractClass cls) {
        if (this == cls) return true;
        if (cls.getSuperClass() == null) return false;
        return isSuperOrSameAs(cls.getSuperClass());
    }

    public abstract Object dummyInstance();

    @Override
    public String toString() {
        return getName();
    }

    public List<DPField> getAllFields() {
        List<DPField> fields = new ArrayList<>(getFields());
        if (getSuperClass() != null) {
            fields.addAll(getSuperClass().getAllFields());
        }
        return fields;
    }
}
