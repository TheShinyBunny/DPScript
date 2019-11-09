package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.AbstractFile;
import com.shinybunny.utils.fs.Folder;

public class DPFolder extends ProjectEntry {

    public DPFolder(Folder file, String path) {
        super(file,path);
    }

    @Override
    public void compile(CompilationContext ctx) {
        String prev = ctx.getPath();
        ctx.setPath(path);
        for (AbstractFile f : file.asFolder().children()) {
            ProjectEntry.from(f,path + file.getName() + "/").compile(ctx);
        }
        ctx.setPath(prev);
    }
}
