package com.shinysponge.dpscript.pawser.selector;

import com.shinybunny.utils.MapBuilder;

import java.util.Map;
import java.util.stream.Collectors;

public class SimpleSelector implements Selector {

    private char target;
    private Map<String,String> params;

    public SimpleSelector(char target, Map<String, String> params) {
        this.target = target;
        this.params = params.entrySet().stream().filter(e->e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean isSingle() {
        return target == 'p' || target == 's' || target == 'r' || Integer.parseInt(params.getOrDefault("limit","0")) == 1;
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public char getTarget() {
        return target;
    }

    @Override
    public Selector toSingle() {
        return new SimpleSelector(target, MapBuilder.concat(params).and("limit","1"));
    }

    @Override
    public String toString() {
        return Selector.toString(this);
    }
}
