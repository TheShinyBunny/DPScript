package com.shinysponge.dpscript.tokenizew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class Tokenizer {

    public static final char[] SYMBOLS = "{}().,;[]=;~^@:#".toCharArray();
    public static final String[] OPERATORS = new String[]{"+", "-", "++", "--", "*", "/", "%", "<", ">", ">=", "<=", "==", "><", "!=", "!", "&&", "||"}; // , "&", "|", "^", "~"


    public static List<Token> tokenize(String str) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        boolean inString = false;
        String temp = "";
        boolean signedNumber = false;
        boolean wasSignedN = false;
        while (pos < str.length()) {
            char c = str.charAt(pos++);
            if (inString) {
                if ((c == '\'' || c == '"') && (temp.isEmpty() || temp.charAt(temp.length()-1) != '\\')) {
                    tokens.add(new Token(TokenType.STRING,temp));
                    temp = "";
                    inString = false;
                } else {
                    temp += c;
                }
                continue;
            }
            if (wasSignedN) {
                wasSignedN = false;
                signedNumber = false;
            }
            if (signedNumber) {
                wasSignedN = true;
            }
            switch (c) {
                case '"': case '\'':
                    inString = true;
                    break;
                case ' ':
                    break;
                case '\n':
                    tokens.add(new Token(TokenType.LINE_END,"\n"));
                    break;
                default: {
                    if (signedNumber || Character.isDigit(c)) {
                        String numStr = "" + c;
                        for (; pos < str.length() - 1 && TokenType.INT.test(numStr + str.charAt(pos)); pos++) {
                            numStr += str.charAt(pos);
                        }
                        if (pos < numStr.length() - 1) {
                            String doubleStr = numStr;
                            for (; pos < str.length() - 1 && TokenType.DOUBLE.test(doubleStr + str.charAt(pos)); pos++) {
                                doubleStr += str.charAt(pos);
                            }
                            if (doubleStr.equals(numStr)) {
                                tokens.add(new Token(TokenType.INT,numStr));
                            } else {
                                tokens.add(new Token(TokenType.DOUBLE,doubleStr));
                            }
                        } else {
                            tokens.add(new Token(TokenType.INT,numStr));
                        }
                        signedNumber = false;
                    } else if (isOperator(c + "") && (pos >= str.length() || !isOperator(c + "" + str.charAt(pos) + ""))) {
                        if ((c == '+' || c == '-') && Character.isDigit(str.charAt(pos))) {
                            pos--;
                            signedNumber = true;
                            break;
                        } else {
                            tokens.add(new Token(TokenType.OPERATOR, c + ""));
                        }
                    } else if (pos < str.length() - 1 && isOperator(c + "" + str.charAt(pos) + "")) {
                        tokens.add(new Token(TokenType.OPERATOR,c + "" + str.charAt(pos++) + ""));
                    } else if (isSymbol(c)){
                        tokens.add(new Token(TokenType.SYMBOL,c + ""));
                    } else if (TokenType.IDENTIFIER.test(c + "")){
                        String id = c + "";
                        for (; pos < str.length() && TokenType.IDENTIFIER.test(id + str.charAt(pos)); pos++) {
                            id += str.charAt(pos);
                        }
                        tokens.add(new Token(TokenType.IDENTIFIER,id));
                    }
                    break;
                }
            }
        }
        return tokens;
    }

    public static boolean isOperator(String s) {
        for (String o : OPERATORS) {
            if (o.equals(s)) return true;
        }
        return false;
    }

    public static boolean isSymbol(char c) {
        for (char s : SYMBOLS) {
            if (s == c) return true;
        }
        return false;
    }

}
