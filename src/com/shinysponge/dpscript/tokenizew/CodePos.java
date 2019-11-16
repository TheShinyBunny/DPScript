package com.shinysponge.dpscript.tokenizew;

import java.io.File;

public class CodePos implements Comparable<CodePos> {

    public static final CodePos END = new CodePos(null,-1,-1,-1);
    private File file;
    private int pos;
    private int line;
    private int column;

    public CodePos(File file, int pos, int line, int column) {
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

    public File getFile() {
        return file;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return this.line == -1 ? "end" : file.getName() + "(line " + line + ", column " + column + ")";
    }

    @Override
    public int compareTo(CodePos o) {
        return Integer.compare(this.pos,o.pos);
    }
}
