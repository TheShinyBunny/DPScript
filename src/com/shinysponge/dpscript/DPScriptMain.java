package com.shinysponge.dpscript;

import com.shinysponge.dpscript.project.Datapack;

import java.io.File;

public class DPScriptMain {

    public static void main(String[] args) throws Exception {
        Datapack.from(new File("oneplayersleep")).compile();
    }

}
