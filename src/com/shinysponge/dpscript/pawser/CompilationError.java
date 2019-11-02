package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.tokenizew.CodePos;
import com.shinysponge.dpscript.tokenizew.Token;

public class CompilationError extends RuntimeException {

    private final CodePos pos;
    private Token token;

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

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public CompilationError(String message, CodePos pos, Token token) {
        super(message);
        this.pos = pos;
        this.token = token;
    }

    public CodePos getPos() {
        return pos;
    }

    public Token getToken() {
        return token;
    }
}
