package com.shinysponge.dpscript.pawser.conditions;

import com.shinysponge.dpscript.pawser.Parser;
import java.util.List;

public abstract class Condition {

    public abstract List<String> toCommands(Parser parser, String function);
}
