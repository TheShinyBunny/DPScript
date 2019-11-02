package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.AbstractFile;
import com.shinybunny.utils.fs.Folder;

public class DPFolder extends ProjectEntry {
    public DPFolder(Folder file) {
        super(file);
    }

    @Override
    public void compile(CompilationContext ctx) {
        for (AbstractFile f : file.asFolder().children()) {
            ProjectEntry.from(f).compile(ctx);
        }
    }
}
