package com.shinysponge.dpscript.project;

import com.shinybunny.utils.StringUtils;
import com.shinybunny.utils.fs.Files;
import com.shinysponge.dpscript.pawser.Parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MCFunction implements Taggable {

    private Namespace namespace;
    private String name;
    private List<String> commands;

    public MCFunction(Namespace namespace, String name) {
        this.namespace = namespace;
        this.name = name;
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

    public void saveIn(File folder) {
        File f = new File(folder, StringUtils.toLowerCaseUnderscore(name) + ".mcfunction");
        Files.write(f,String.join("\n",commands));
    }

    public String getName() {
        return name;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return namespace.getName() + ":" + name;
    }
}
