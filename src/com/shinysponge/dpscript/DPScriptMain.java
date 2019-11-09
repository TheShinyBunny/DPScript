package com.shinysponge.dpscript;

import com.shinybunny.utils.fs.File;
import com.shinybunny.utils.fs.Folder;
import com.shinybunny.utils.json.*;
import com.shinysponge.dpscript.pawser.CompilationError;
import com.shinysponge.dpscript.project.CompilationResults;
import com.shinysponge.dpscript.project.Datapack;

public class DPScriptMain {

    public static void main(String[] args) {
        String dir = args.length == 0 ? "class_test" : args[0];
        CompilationResults results = compile(Folder.of(dir));//.saveFunctions(Folder.of("output/oneplayersleep"));
        Json json = new Json();
        JsonArray arr = new JsonArray();
        for (CompilationError err : results.getErrors()) {
            arr.add(err.toJson());
        }
        json.set("errors",arr);
        String str = json.prettyPrint(4);
        System.out.println(str);
        File.of("compilerOutput.json").setContent(str);
    }

    public static CompilationResults compile(Folder src) {
        return Datapack.from(src).compile();
    }

    public static void generateMCMeta(Folder in, String description) {
        JsonFile mcmeta = JsonFile.of(in,"pack.mcmeta").set("pack.description",description).set("pack.pack_format",1);
        System.out.println(mcmeta.getContent());
    }
}
