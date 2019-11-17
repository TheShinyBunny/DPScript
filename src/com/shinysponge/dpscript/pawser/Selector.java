package com.shinysponge.dpscript.pawser;

import com.shinybunny.utils.MapBuilder;
import com.shinysponge.dpscript.EmptyJoinCollector;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Selector {

    private char target;
    private Map<String,String> params;

    public Selector(char target, Map<String, String> params) {
        this.target = target;
        this.params = params.entrySet().stream().filter(e->e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isSingle() {
        return target == 'p' || target == 's' || target == 'r' || Integer.parseInt(params.getOrDefault("limit","0")) == 1;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public char getTarget() {
        return target;
    }

    public Selector toSingle() {
        return new Selector(target, MapBuilder.concat(params).and("limit","1"));
    }

    public static String toMultiParams(String key, List<String> values) {
        if (values.isEmpty()) return null;
        String result = "";
        for (String s : values) {
            if (!result.isEmpty()) {
                result += "," + key + "=";
            }
            result += s;
        }
        return result;
    }

    public Selector set(String key, String value){
        if (value == null) return this;
        getParams().put(key,value);
        return this;
    }

    @Override
    public String toString() {
        return "@" + target + params.entrySet().stream().map(e->e.getKey() + "=" + e.getValue()).collect(new EmptyJoinCollector(",","[","]"));
    }

    public boolean targetsPlayers() {
        if (getTarget() == 'p' || getTarget() == 'a' || getTarget() == 's') return true;
        Map<String,String> params = getParams();
        return "player".equalsIgnoreCase(params.get("type"));
    }

    public void ensurePlayer(String explanation) {
        if (!targetsPlayers()) {
            Parser.compilationError(null,"selector " + this + " can target non-players, but " + explanation);
        }
    }
}
