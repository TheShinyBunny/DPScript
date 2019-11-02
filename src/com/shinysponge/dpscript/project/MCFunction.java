package com.shinysponge.dpscript.project;

import com.shinybunny.utils.fs.File;
import com.shinybunny.utils.fs.Folder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MCFunction {

    private String namespace;
    private String name;
    private List<String> commands;
    private FunctionType type;

    public MCFunction(String namespace, String name, FunctionType type) {
        this.namespace = namespace;
        this.name = name;
        this.type = type;
        this.commands = new ArrayList<>();
    }

    public void add(String command) {
        commands.add(command);
    }

    public void addAll(List<String> commands) {
        this.commands.addAll(commands);
    }

    public void forEachCommand(Consumer<String> consumer) {
        commands.forEach(consumer);
    }

    public void saveIn(Folder folder) {
        File f = folder.child(name + ".mcfunction");
        f.setContent(String.join("\n",commands));
    }
}
