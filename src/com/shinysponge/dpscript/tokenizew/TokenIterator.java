package com.shinysponge.dpscript.tokenizew;

import com.shinysponge.dpscript.oop.ClassParser;
import com.shinysponge.dpscript.oop.DPClass;
import com.shinysponge.dpscript.oop.LazyValue;
import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.sun.xml.internal.bind.v2.model.core.ID;

import java.util.Iterator;
import java.util.List;

/**
 * This class iterates through a list of {@link Token Tokens}.
 * This is used for going through the code and expecting, checking, validating and acting according the code.
 */
public class TokenIterator implements Iterator<Token> {

    private final ErrorConsumer errorConsumer;
    private List<Token> data;
    private int pos;
    private Token lastToken;

    public TokenIterator(List<Token> data, ErrorConsumer errorConsumer) {
        this.data = data;
        this.pos = 0;
        this.errorConsumer = errorConsumer;
        for (Token t : data) {
            if (t.getType() == TokenType.LINE_END) {
                System.out.println();
            } else {
                System.out.print(t);
            }
        }
        System.out.println();
    }

    public static TokenIterator from(String str) {
        return new TokenIterator(Tokenizer.tokenize(Parser.getContext().getFile(),str),Parser::compilationError);
    }

    /**
     * Returns {@code true} if the iteration has more tokens.
     * (In other words, returns {@code true} if {@link #next} would
     * return a real token rather than {@link Token#EOF})
     *
     * @return {@code true} if the iteration has more tokens
     */
    @Override
    public boolean hasNext() {
        return pos < data.size();
    }

    /**
     * Returns the next token in the iteration.
     *
     * @return the next token in the iteration
     */
    @Override
    public Token next() {
        if (pos >= data.size()) return Token.EOF;
        return lastToken = data.get(pos++);
    }

    /**
     * Returns the next token in the iterator, without skipping it. Calls {@link #peek(int) peek(0)}.
     */
    public Token peek() {
        return peek(0);
    }

    /**
     * Returns the token {@code i} positions forward. Providing 0 will return the next token.
     * Does not skip to the next token.
     * @param i The number of steps in the token list to get.
     * @return The token at index <code>pos + i</code>
     */
    public Token peek(int i) {
        if (pos + i >= data.size()) return Token.EOF;
        return data.get(pos + i);
    }

    /**
     * Returns the next token's string value. Calls {@link #next() next()}{@link Token#getValue() .getValue()}.
     */
    public String nextValue() {
        return next().getValue();
    }


    /**
     * Returns true if the next token's type is one of the given {@link TokenType}s. Does not skip to the next token.
     * @param tokenType The types to compare to
     */
    public boolean isNext(TokenType... tokenType) {
        if (!hasNext()) return false;
        TokenType t = peek().getType();
        for (TokenType t2 : tokenType) {
            if (t == t2) return true;
        }
        return false;
    }

    /**
     * Returns true if the next token value is one of the given strings. Does not skip to the next token.
     * @param values The values to compare to
     */
    public boolean isNext(String... values) {
        String s = peek().getValue();
        for (String v : values) {
            if (v.equals(s)) return true;
        }
        return false;
    }

    /**
     * Skips to the next token. Similar to {@link #next()}, but without checking bounds or returning the token.
     */
    public void skip() {
        pos++;
    }

    /**
     * Expect the next token to be the specified character. If it is, skips it. Otherwise, will add a compilation error.
     * @param c The character to expect
     */
    public void expect(char c) {
        expect(c + "");
    }

    /**
     * Expect the next token to be one of the specified strings. If it is, skips it and returns the matching value. Otherwise, will add a compilation error.
     * @param s The strings to compare
     * @return The matching string.
     */
    public String expect(String... s) {
        if (!isNext(s)) {
            error(ErrorType.EXPECTED, s.length == 1 ? s[0] : "one of (" + String.join(",", s) + ")");
        }
        return nextValue();
    }

    /**
     * Expect the given token type to be the next token. If it is, skips and returns the value. Otherwise, adds an compilation error, and returns the default value of that type.
     * @param type The type to check
     * @param desc The description about the use of that token.
     * @return The value of the token with that type.
     */
    public String expect(TokenType type, String desc) {
        if(!isNext(type)) {
            skip();
            if (desc != null) {
                error(ErrorType.EXPECTED, desc);
                return type.getDefault().toString();
            }
        }
        return nextValue();
    }

    /**
     * Skips to the next token if the next token is of the specified type.
     * @param type The token type.
     * @return true if it was that type and skipped it.
     */
    public boolean skip(TokenType type) {
        if (isNext(type)) {
            skip();
            return true;
        }
        return false;
    }

    /**
     * Skips all tokens until the next {@link TokenType#LINE_END}.
     */
    public void nextLine() {
        while (hasNext() && !isNext(TokenType.LINE_END))
            skip();
        skip(TokenType.LINE_END);
    }

    /**
     * Skips if the next token's value is one of the specified strings.
     * @param s The values to check
     * @return true if it had one of those values and skipped to the next token.
     */
    public boolean skip(String... s) {
        if (isNext(s)) {
            skip();
            return true;
        }
        return false;
    }

    public void error(ErrorType type, String message) {
        errorConsumer.onError(type,message);
    }

    /**
     * Returns the previous token's value. Will only save the last token if it was skipped with {@link #next()}.
     */
    public String previous() {
        return lastToken.getValue();
    }

    /**
     * Skips all tokens with the specified values in order.
     * @param tokens The tokens to have in that order in the code.
     * @return True if it was all skipped successfully.
     */
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

    public LazyValue<Integer> readInt() {
        return ClassParser.parseExpression(this, DPClass.INT).map(i->(Integer) i);
    }

    public LazyValue<Double> readDouble() {
        return ClassParser.parseExpression(this, DPClass.DOUBLE).map(d->(Double) d);
    }

    public LazyValue<Boolean> readBoolean() {
        return ClassParser.parseExpression(this, DPClass.BOOLEAN).map(b->(Boolean)b);
    }

    public double readLiteralDouble() {
        return Double.parseDouble(expect(TokenType.DOUBLE,"double"));
    }

    /**
     * Reverts the token iterator to the previous token
     */
    public void pushBack() {
        pos--;
    }

    public void suggestHere(List<String> suggestions) {
        Parser.getContext().suggest(peek(),suggestions.toArray(new String[0]));
    }

    public int readLiteralInt() {
        return Integer.parseInt(expect(TokenType.INT,"integer"));
    }

    public boolean readLiteralBoolean() {
        return Boolean.parseBoolean(expect("true","false"));
    }

    @FunctionalInterface
    public interface ErrorConsumer {

        void onError(ErrorType type, String description);

    }
}
