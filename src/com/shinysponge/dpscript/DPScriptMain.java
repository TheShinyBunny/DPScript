package com.shinysponge.dpscript;

import com.shinybunny.utils.fs.Files;
import com.shinybunny.utils.json.*;
import com.shinysponge.dpscript.pawser.CompilationError;
import com.shinysponge.dpscript.project.CompilationContext;
import com.shinysponge.dpscript.project.CompilationResults;
import com.shinysponge.dpscript.project.Datapack;

import java.io.File;

public class DPScriptMain {

    public static void main(String[] args) {
        String dir = args.length == 0 ? "class_test" : args[0];
        File dest = new File(dir);
        CompilationResults results = compile(dest);//.saveFunctions(Folder.of("output/oneplayersleep"));
        Json json = new Json();
        JsonArray arr = new JsonArray();
        for (CompilationError err : results.getErrors()) {
            arr.add(err.toJson());
        }
        json.set("errors",arr);
        String str = json.prettyPrint(4);
        Files.write("compilerOutput.json",str);
        if (results.isSuccessful()) {
            results.getDatapack().save(new File(dest,"ignore"));
        }
    }

    public static CompilationResults compile(File src) {
        Datapack dp = new Datapack(src);
        CompilationContext ctx = new CompilationContext(dp);
        return ctx.compile();
    }

    public static void generateMCMeta(File in, String description) {
        JsonFile.of(in,"pack.mcmeta").set("pack.description",description).set("pack.pack_format",1);
    }
}
