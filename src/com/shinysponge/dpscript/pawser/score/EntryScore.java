package com.shinysponge.dpscript.pawser.score;

public class EntryScore extends Score {

    private String objective;
    private String entry;

    public EntryScore(String objective, String entry) {
        this.objective = objective;
        this.entry = entry;
    }

    @Override
    public String toString() {
        return entry + " " + objective;
    }

    @Override
    public String toAssignCommand(EntryScore dest) {
        return "scoreboard players operation " + dest + " = " + this;
    }
}
