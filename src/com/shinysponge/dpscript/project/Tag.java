package com.shinysponge.dpscript.project;

import com.shinybunny.utils.json.JsonFile;

import java.io.File;
import java.util.List;

public class Tag implements DatapackItem {
    private final String id;
    private final String type;
    private final List<? extends Taggable> objects;

    public Tag(String id, String type, List<? extends Taggable> objects) {
        this.id = id;
        this.type = type;
        this.objects = objects;
    }

    @Override
    public void saveIn(File dir) {
        JsonFile file = JsonFile.of(dir,id + ".json");
        file.set("values",objects);
    }

    @Override
    public String getDirectory() {
        return "tags/" + type;
    }
}
