package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.Files;

import java.io.File;
import java.util.*;

public class Namespace {

    private final String name;
    private File dir;
    private Map<String,MCFunction> functions = new HashMap<>();
    private List<DatapackItem> items = new ArrayList<>();

    public Namespace(String name) {
        this.name = name;
    }

    public Namespace(File dir) {
        this.dir = dir;
        this.name = dir.getName().toLowerCase();
    }

    public void addFunction(MCFunction function) {
        System.out.println("adding function to namespace " + name);
        functions.put(function.getName(),function);
        items.add(function);
    }

    public String getName() {
        return name;
    }

    public boolean isReal() {
        return dir != null;
    }

    public void addTag(String type, String id, List<? extends Taggable> objects) {
        items.add(new Tag(id,type,objects));
    }

    public void saveIn(File data) {
        File f = Files.create(data,name);
        for (DatapackItem item : items) {
            File dir = Files.create(f,item.getDirectory());
            item.saveIn(dir);
        }
    }

    public MCFunction getFunction(String name) {
        return functions.get(name);
    }

    public Collection<MCFunction> getFunctions() {
        return functions.values();
    }

    @Override
    public String toString() {
        return name;
    }

    public void addItem(DatapackItem item) {
        items.add(item);
    }
}
