package com.shinysponge.dpscript;

import com.shinybunny.utils.fs.Folder;
import com.shinybunny.utils.json.JsonFile;
import com.shinysponge.dpscript.project.CompilationResults;
import com.shinysponge.dpscript.project.Datapack;

public class DPScriptMain {

    public static void main(String[] args) {
        compile(Folder.of("class_test"));//.saveFunctions(Folder.of("output/oneplayersleep"));
    }

    public static CompilationResults compile(Folder src) {
        return Datapack.from(src).compile();
    }

    public static void generateMCMeta(Folder in, String description) {
        JsonFile mcmeta = JsonFile.of(in,"pack.mcmeta").set("pack.description",description).set("pack.pack_format",1);
        System.out.println(mcmeta.getContent());
    }
}
