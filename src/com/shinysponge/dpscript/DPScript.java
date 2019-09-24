package com.shinysponge.dpscript;

import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.Token;
import com.shinysponge.dpscript.tokenizew.Tokenizer;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class DPScript {

    public static void main(String[] args) throws Exception {
        File f = new File("script.dps");
        String[] lines = Files.readAllLines(f.toPath()).stream()
                .map(s -> s + " ")
                .filter(s -> !s.startsWith("#"))
                .toArray(String[]::new);

        String code = String.join("\n", lines);
        System.out.println(code);

        List<Token> tokens = Tokenizer.tokenize(code);
        System.out.println(tokens);
        Parser.pawse(tokens);
    }

}
