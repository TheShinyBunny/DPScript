package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.project.CompilationContext;
import com.shinysponge.dpscript.tokenizew.CodePos;

import java.util.function.Predicate;

public class GlobalLaterCheck {

    private String name;
    private String typeName;
    private CodePos pos;
    private Predicate<CompilationContext> check;

    public GlobalLaterCheck(String name, String typeName, CodePos pos, Predicate<CompilationContext> check) {
        this.name = name;
        this.typeName = typeName;
        this.pos = pos;
        this.check = check;
    }

    public void check(CompilationContext ctx) {
        if (!check.test(ctx)) {
            ctx.addError(new CompilationError("Compilation error at " + pos + ": Unknown " + typeName + " '" + name + "'",pos));
        }
    }
}
