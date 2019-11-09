package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.AbstractFile;
import com.shinybunny.utils.fs.Folder;

public abstract class ProjectEntry {

    private static final ProjectEntry DUMMY = new ProjectEntry(null,"") {
        @Override
        public void compile(CompilationContext ctx) {}
    };
    protected final String path;
    protected AbstractFile file;

    public ProjectEntry(AbstractFile file, String path) {
        this.file = file;
        this.path = path;
    }

    public static ProjectEntry from(AbstractFile f, String path) {
        if (f instanceof Folder) return new DPFolder(f.asFolder(),path);
        else if (f.getName().substring(f.getName().lastIndexOf('.')+1).equalsIgnoreCase("dps")) return new DPScript(f.asFile(),path);
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
