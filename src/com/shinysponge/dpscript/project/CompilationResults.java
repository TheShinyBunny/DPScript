package com.shinysponge.dpscript.project;

import com.shinybunny.utils.DummyEntry;
import com.shinybunny.utils.fs.Folder;
import com.shinysponge.dpscript.DPScriptMain;
import com.shinysponge.dpscript.pawser.CompilationError;
import com.shinysponge.dpscript.tokenizew.CodePos;
import com.shinysponge.dpscript.tokenizew.Token;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CompilationResults {
    private final Datapack project;
    private final List<CompilationError> errors;
    private final Map<String, MCFunction> functions;
    private Map<Token, String[]> suggestions;

    public CompilationResults(Datapack project, List<CompilationError> errors, Map<String, MCFunction> functions, Map<Token, String[]> suggestions) {
        this.project = project;
        this.errors = errors;
        this.functions = functions;
        this.suggestions = suggestions;
    }

    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    public Datapack getProject() {
        return project;
    }

    public Map<String, MCFunction> getFunctions() {
        return functions;
    }

    public List<CompilationError> getErrors() {
        return errors;
    }

    public void saveFunctions(Folder dest) {
        if (!isSuccessful()) return;
        DPScriptMain.generateMCMeta(dest,project.getDescription());
        Folder funcs = dest.subFolder("data").subFolder(project.getName()).subFolder("functions");
        for (MCFunction f : functions.values()) {
            f.saveIn(funcs);
        }
    }

    public String[] getSuggestions(File file, int pos) {
        return suggestions.entrySet().stream().filter(e->e.getKey().isPositionInside(file,pos)).findFirst().orElse(new DummyEntry<>(null,new String[0])).getValue();
    }

    public String[] getEndOfFileSuggestions(File file) {
        return suggestions.entrySet().stream().filter(e->e.getKey().getPos().getLine() == -1 && e.getKey().getPos().getFile().getFile().sameAs(file)).findFirst().orElse(new DummyEntry<>(null,new String[0])).getValue();
    }
}
