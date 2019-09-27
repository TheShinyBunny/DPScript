package com.shinysponge.dpscript.tokenizew;

import com.shinysponge.dpscript.pawser.ErrorType;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TokenIterator implements Iterator<Token> {

    private final ErrorConsumer errorConsumer;
    private List<Token> data;
    private int pos;
    private Token lastToken;

    public TokenIterator(List<Token> data, ErrorConsumer errorConsumer) {
        this.data = data;
        this.pos = 0;
        this.errorConsumer = errorConsumer;
        System.out.println(data);
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
        if (pos >= data.size()) return Token.EOD;
        return lastToken = data.get(pos++);
    }

    public Token peek() {
        return peek(0);
    }

    public Token peek(int i) {
        if (pos + i >= data.size()) return Token.EOD;
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
        expect(c + "");
    }

    public String expect(String... s) {
        if (!isNext(s)) {
            skip();
            error(ErrorType.EXPECTED, s.length == 1 ? s[0] : "one of (" + String.join(",", s) + ")");
        }
        return nextValue();
    }

    public Token expect(TokenType type, String message) {
        if(!isNext(type)) {
            skip();
            if (message != null) {
                error(ErrorType.EXPECTED, message);
                return new Token(peek().getPos(),TokenType.DUMMY,type.getDefault().toString());
            }
        }
        return next();
    }

    public boolean skip(TokenType type) {
        if (isNext(type)) {
            skip();
            return true;
        }
        return false;
    }

    public void nextLine() {
        while (hasNext() && !isNext(TokenType.LINE_END))
            skip();
        skip(TokenType.LINE_END);
    }

    public boolean skip(String... s) {
        if (isNext(s)) {
            skip();
            return true;
        }
        return false;
    }

    public String next(TokenType type, String desc) {
        if (isNext(type))
            return nextValue();
        if (desc != null) {
            error(ErrorType.EXPECTED, desc);
        }
        skip();
        return type.getDefault().toString();
    }

    public void error(ErrorType type, String message) {
        errorConsumer.onError(type,message);
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

    @FunctionalInterface
    public interface ErrorConsumer {

        void onError(ErrorType type, String description);

    }
}
