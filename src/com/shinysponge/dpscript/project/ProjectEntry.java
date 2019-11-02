package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.AbstractFile;
import com.shinybunny.utils.fs.Folder;

public abstract class ProjectEntry {

    private static final ProjectEntry DUMMY = new ProjectEntry(null) {
        @Override
        public void compile(CompilationContext ctx) {}
    };
    protected AbstractFile file;

    public ProjectEntry(AbstractFile file) {
        this.file = file;
    }

    public static ProjectEntry from(AbstractFile f) {
        if (f instanceof Folder) return new DPFolder(f.asFolder());
        else if (f.getName().substring(f.getName().lastIndexOf('.')+1).equalsIgnoreCase("dps")) return new DPScript(f.asFile());
        return ProjectEntry.DUMMY;
    }

    public abstract void compile(CompilationContext ctx);

    public String getName() {
        return file.getName();
    }

    public AbstractFile getFile() {
        return file;
    }
}
