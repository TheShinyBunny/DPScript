package com.shinysponge.dpscript.project;

import java.io.File;

public class DPFolder extends ProjectEntry {
    public DPFolder(File file) {
        super(file);
    }

    @Override
    public void compile(CompilationContext ctx) {
        ctx.pushDirectory(this);
        for (File f : file.listFiles()) {
            ProjectEntry.from(f).compile(ctx);
        }
        ctx.popDirectory();
    }
}
