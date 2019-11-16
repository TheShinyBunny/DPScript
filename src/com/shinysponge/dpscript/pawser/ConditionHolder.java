package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.score.EntryScore;

public interface ConditionHolder<T extends ConditionOption> {

    String compareAndRun(T option, EntryScore value, String command);

    T parseOption();
}
