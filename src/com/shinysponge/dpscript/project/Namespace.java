package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.Files;

import java.io.File;
import java.util.*;

public class Namespace {

    private final String name;
    private File dir;
    private Map<String,MCFunction> functions = new HashMap<>();

    public Namespace(String name) {
        this.name = name;
    }

    public Namespace(File dir) {
        this.dir = dir;
        this.name = dir.getName();
    }

    public void addFunction(MCFunction function) {
        functions.put(function.getName(),function);
    }

    public String getName() {
        return name;
    }

    public boolean isReal() {
        return dir != null;
    }

    public void addTag(String type, String id, List<? extends Taggable> objects) {

    }

    public void saveIn(File data) {
        File f = Files.create(data,name);
        File funcs = Files.create(f,"functions");
        for (MCFunction mcf : functions.values()) {
            mcf.saveIn(funcs);
        }
    }

    public MCFunction getFunction(String name) {
        return functions.get(name);
    }

    public Collection<MCFunction> getFunctions() {
        return functions.values();
    }
}
