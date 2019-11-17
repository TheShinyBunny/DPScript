package com.shinysponge.dpscript.advancements;

import com.shinybunny.utils.fs.Files;
import com.shinybunny.utils.json.Json;
import com.shinysponge.dpscript.pawser.parsers.JsonTextParser;
import com.shinysponge.dpscript.project.DatapackItem;
import com.shinysponge.dpscript.project.Namespace;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.io.File;

public class Advancement implements DatapackItem {
    private final Namespace namespace;
    private String id;
    private JsonTextParser.JsonValue title;
    private JsonTextParser.JsonValue description;
    private FrameType frame;

    public Advancement(Namespace namespace, String id) {
        this.namespace = namespace;
        this.id = id;
        this.frame = FrameType.TASK;
    }

    public void parseDeclaration(TokenIterator tokens) {
        tokens.expect('{');
        tokens.nextLine();
        while (tokens.hasNext() && !tokens.skip("}")) {
            if (tokens.skip(TokenType.LINE_END)) continue;
            parseDisplayProps(tokens);
            tokens.nextLine();
        }
    }

    private void parseDisplayProps(TokenIterator tokens) {
        System.out.println("next prop: " + tokens.peek());
        if (tokens.skip("title")) {
            tokens.skip("=",":");
            this.title = JsonTextParser.readJson(new JsonTextParser.Context(tokens));
        } else if (tokens.skip("desc","description","subtitle","info")) {
            tokens.skip("=",":");
            this.description = JsonTextParser.readJson(new JsonTextParser.Context(tokens));
        }
    }

    public Advancement frame(FrameType frame) {
        this.frame = frame;
        return this;
    }

    public Json toJson() {
        Json json = new Json();
        if (title != null) {
            json.set("display.title", title);
        }
        if (description != null) {
            json.set("display.description", description);
        }
        json.set("display.frame", frame);
        return json;
    }

    @Override
    public void saveIn(File dir) {
        Files.write(Files.create(dir,id + ".json"),toJson().prettyPrint(4));
    }

    @Override
    public String getDirectory() {
        return "advancements";
    }
}
