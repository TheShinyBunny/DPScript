package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.tokenizew.TokenIterator;

public interface DPField {

    String getKey();

    LazyValue<?> parse(TokenIterator tokens, String key);

    String[] getAliases();

    default boolean useKeyAsAlias() {
        return true;
    }

    default boolean isKey(String key) {
        if (useKeyAsAlias() && getKey().equalsIgnoreCase(key)) return true;
        for (String s : getAliases()) {
            if (s.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

}
