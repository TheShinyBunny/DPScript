package com.shinysponge.dpscript.pawser.selector;

import com.shinysponge.dpscript.EmptyJoinCollector;

import java.util.List;
import java.util.Map;

public interface Selector {

    static String toMultiParams(String key, List<String> values) {
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

    static String toString(Selector selector) {
        return "@" + selector.getTarget() + selector.getParams().entrySet().stream().map(e->e.getKey() + "=" + e.getValue()).collect(new EmptyJoinCollector(",","[","]"));
    }

    boolean isSingle();

    Map<String,String> getParams();

    char getTarget();

    default Selector set(String key, String value){
        if (value == null) return this;
        getParams().put(key,value);
        return this;
    }

    Selector toSingle();

    default boolean targetsPlayers() {
        if (getTarget() == 'p' || getTarget() == 'a') return true;
        Map<String,String> params = getParams();
        return "player".equalsIgnoreCase(params.get("type"));
    }
}
