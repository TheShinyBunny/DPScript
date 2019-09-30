package com.shinysponge.dpscript.project;

import com.shinysponge.dpscript.pawser.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DPScript extends ProjectEntry {

    public DPScript(File file) {
        super(file);
    }

    @Override
    public void compile(CompilationContext ctx) {
        ctx.setFile(this);
        Parser.parse(ctx);
    }


    public List<String> getCode() {
        try {
            return Files.readAllLines(file.toPath()).stream()
                    .map(s -> s.trim().startsWith("#") || s.trim().startsWith("//") ? "" : s)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
