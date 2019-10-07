package com.shinysponge.dpscript.project;

import com.shinysponge.dpscript.pawser.CompilationError;
import com.shinysponge.dpscript.pawser.Parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Datapack {

    private final File root;
    private List<ProjectEntry> components;

    public Datapack(File root) {
        this.root = root;
        this.components = new ArrayList<>();
    }

    public void compile() {
        CompilationContext ctx = new CompilationContext(this);
        Parser.init(ctx);
        for (File f : root.listFiles()) {
            ProjectEntry.from(f).compile(ctx);
        }
        ctx.runChecks();
        ctx.logResults();
    }

    public String getName() {
        return root.getName();
    }

    public static Datapack from(File f) {
        return new Datapack(f);
    }
}
