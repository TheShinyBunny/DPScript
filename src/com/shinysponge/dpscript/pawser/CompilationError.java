package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.tokenizew.CodePos;

public class CompilationError extends RuntimeException {

    private final CodePos pos;

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public CompilationError(String message, CodePos pos) {
        super(message);
        this.pos = pos;
    }

    public CodePos getPos() {
        return pos;
    }
}
