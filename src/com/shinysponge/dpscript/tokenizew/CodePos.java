package com.shinysponge.dpscript.tokenizew;

import com.shinysponge.dpscript.project.DPScript;

public class CodePos implements Comparable<CodePos> {

    public static final CodePos END = new CodePos(null,-1,-1,-1);
    private DPScript file;
    private int pos;
    private int line;
    private int column;

    public CodePos(DPScript file, int pos, int line, int column) {
        this.pos = pos;
        this.file = file;
        this.line = line;
        this.column = column;
    }

    public int getPos() {
        return pos;
    }

    public int getLine() {
        return line;
    }

    public DPScript getFile() {
        return file;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return this == END ? "end" : file.getName() + "(line " + line + ", column " + column + ")";
    }

    @Override
    public int compareTo(CodePos o) {
        return Integer.compare(this.pos,o.pos);
    }
}
