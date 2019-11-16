package com.shinysponge.dpscript.project;

import com.shinybunny.utils.DummyEntry;
import com.shinybunny.utils.json.Json;
import com.shinybunny.utils.json.JsonArray;
import com.shinysponge.dpscript.pawser.CompilationError;
import com.shinysponge.dpscript.tokenizew.CodePos;
import com.shinysponge.dpscript.tokenizew.Token;

import java.io.File;
import java.util.*;

public class CompilationResults {
    private final Datapack project;
    private final List<CompilationError> errors;
    private Map<Token, String[]> suggestions;

    public CompilationResults(Datapack project, List<CompilationError> errors, Map<Token, String[]> suggestions) {
        this.project = project;
        this.errors = errors;
        this.suggestions = suggestions;
    }

    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    public Datapack getProject() {
        return project;
    }

    public List<CompilationError> getErrors() {
        return errors;
    }

    public JsonArray getSuggestions() {
        JsonArray json = new JsonArray();
        for (Map.Entry<Token,String[]> s : suggestions.entrySet()) {
            System.out.println("added suggestions: " + Arrays.toString(s.getValue()));
            CodePos pos = s.getKey().getPos();
            Json suggestion = new Json().set("file",pos.getFile().getPath()).set("line",pos.getLine()).set("column",pos.getColumn()).set("length",s.getKey().getValue().length()).set("values",s.getValue());
            json.add(suggestion);
        }
        return json;
    }

    public String[] getSuggestions(File file, int pos) {
        return suggestions.entrySet().stream().filter(e->e.getKey().isPositionInside(file,pos)).findFirst().orElse(new DummyEntry<>(null,new String[0])).getValue();
    }

    public String[] getEndOfFileSuggestions(File file) {
        return suggestions.entrySet().stream().filter(e->e.getKey().getPos().getLine() == -1 && e.getKey().getPos().getFile().equals(file)).findFirst().orElse(new DummyEntry<>(null,new String[0])).getValue();
    }

    public Datapack getDatapack() {
        return project;
    }
}
