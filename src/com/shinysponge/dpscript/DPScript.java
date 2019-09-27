package com.shinysponge.dpscript;

import com.shinysponge.dpscript.pawser.Parser;

import java.io.File;

public class DPScript {

    public static void main(String[] args) throws Exception {
        File f = new File("up_trigger.dps");
        Parser.pawse(f);
    }

}
