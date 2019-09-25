package com.shinysponge.dpscript.tokenizew;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TokenIterator implements Iterator<Token> {

    private List<Token> data;
    private int pos;
    private Token lastToken;

    public TokenIterator(List<Token> data) {
        this.data = data;
        this.pos = 0;
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return pos < data.size();
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public Token next() {
        return lastToken = data.get(pos++);
    }

    public Token peek() {
        return peek(0);
    }

    public Token peek(int i) {
        return data.get(pos + i);
    }

    public String nextValue() {
        return next().getValue();
    }

    public boolean isNext(TokenType... tokenType) {
        if (!hasNext()) return false;
        TokenType t = peek().getType();
        for (TokenType t2 : tokenType) {
            if (t == t2) return true;
        }
        return false;
    }

    public boolean isNext(String... values) {
        String s = peek().getValue();
        for (String v : values) {
            if (v.equals(s)) return true;
        }
        return false;
    }

    public void skip() {
        pos++;
    }

    public void expect(char c) {
        expect("" + c);
    }

    public String expect(String... s) {
        if (!isNext(s))
            throw new RuntimeException("Expected " + Arrays.toString(s) + ", found " + peek());
        return nextValue();
    }

    public Token expect(TokenType type) {
        if(!isNext(type)) {
            throw new RuntimeException("Expected " + type + ", found " + peek());
        }
        return next();
    }

    public void skip(TokenType type) {
        if (isNext(type))
            skip();
    }

    public void nextLine() {
        skip(TokenType.LINE_END);
    }

    public boolean skip(String... s) {
        if (isNext(s)) {
            skip();
            return true;
        }
        return false;
    }

    public String next(TokenType type) {
        if (isNext(type))
            return nextValue();
        throw new RuntimeException("Expected " + type);
    }

    public String previous() {
        return lastToken.getValue();
    }

    public boolean skipAll(String... tokens) {
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (!peek(i).getValue().equals(t)) return false;
        }
        for (String s : tokens) {
            skip();
        }
        return true;
    }

    /**
     * Reverts the token iterator to the previous token
     */
    public void pushBack() {
        pos--;
    }
}
