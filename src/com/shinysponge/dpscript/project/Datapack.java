package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.AbstractFile;
import com.shinybunny.utils.fs.Folder;
import com.shinysponge.dpscript.pawser.Parser;

import java.util.ArrayList;
import java.util.List;

public class Datapack {

    private final Folder root;
    private List<ProjectEntry> components;

    public Datapack(Folder root) {
        this.root = root;
        this.components = new ArrayList<>();
    }

    public CompilationResults compile() {
        CompilationContext ctx = new CompilationContext(this);
        Parser.init(ctx);
        String path = "";
        for (AbstractFile f : root.children()) {
            ProjectEntry.from(f,path).compile(ctx);
        }
        ctx.runChecks();
        ctx.logResults();
        return ctx.getResults();
    }

    public String getName() {
        return root.getName();
    }

    public static Datapack from(Folder f) {
        return new Datapack(f);
    }

    public String getDescription() {
        return "Auto generated Datapack using DPScript";
    }
}
