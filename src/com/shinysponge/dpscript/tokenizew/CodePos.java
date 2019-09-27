package com.shinysponge.dpscript.tokenizew;

import java.io.File;

public class CodePos {

    public static final CodePos END = new CodePos(null,-1,-1);
    private File file;
    private int line;
    private int column;

    public CodePos(File file, int line, int column) {
        this.file = file;
        this.line = line;
        this.column = column;
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
        return this == END ? "end" : file.getName() + "(line " + line + ", column " + column + ")";
    }
}
