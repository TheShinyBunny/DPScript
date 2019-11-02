package com.shinysponge.dpscript.project;

import com.shinybunny.utils.Array;
import com.shinybunny.utils.fs.File;
import com.shinysponge.dpscript.pawser.Parser;

public class DPScript extends ProjectEntry {

    public DPScript(File file) {
        super(file);
    }

    @Override
    public void compile(CompilationContext ctx) {
        Parser.parse(this);
    }


    public Array<String> getCode() {
        return file.asFile().lines().map(s -> s.trim().startsWith("#") || s.trim().startsWith("//") ? "" : s);
    }

    @Override
    public File getFile() {
        return (File) super.getFile();
    }
}
