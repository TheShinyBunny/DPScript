package com.shinysponge.dpscript.project;

import java.io.File;

public abstract class ProjectEntry {

    private static final ProjectEntry DUMMY = new ProjectEntry(null) {
        @Override
        public void compile(CompilationContext ctx) {}
    };
    protected File file;

    public ProjectEntry(File file) {
        this.file = file;
    }

    public static ProjectEntry from(File f) {
        if (f.isDirectory()) return new DPFolder(f);
        else if (f.getName().substring(f.getName().lastIndexOf('.')+1).equalsIgnoreCase("dps")) return new DPScript(f);
        return ProjectEntry.DUMMY;
    }

    public abstract void compile(CompilationContext ctx);

    public String getName() {
        return file.getName();
    }
}
