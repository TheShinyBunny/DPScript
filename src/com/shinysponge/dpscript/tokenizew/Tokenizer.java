package com.shinysponge.dpscript.tokenizew;

import com.shinysponge.dpscript.project.DPScript;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class Tokenizer {

    public static final char[] SYMBOLS = "{}().,;[]=;~^@:#".toCharArray();
    public static final String[] OPERATORS = new String[]{"+", "-", "++", "--", "*", "/", "%", "<", ">", ">=", "<=", "==", "><", "!=", "!", "&&", "||","+=","-="}; // , "&", "|", "^", "~"


    public static List<Token> tokenize(DPScript file, String str) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int column = 0;
        int line = 1;
        boolean inString = false;
        String temp = "";
        boolean signedNumber = false;
        boolean wasSignedN = false;
        boolean rawCommand = false;
        CodePos codePos = new CodePos(file,0,0);
        while (pos < str.length()) {
            int startPos = pos;
            int startLine = line;
            char c = str.charAt(pos++);
            if (rawCommand && c != '\n') {
                temp += c;
                continue;
            }
            if (inString) {
                if ((c == '\'' || c == '"') && (temp.isEmpty() || temp.charAt(temp.length()-1) != '\\')) {
                    tokens.add(new Token(codePos,TokenType.STRING,temp));
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
            if ((tokens.isEmpty() || tokens.get(tokens.size()-1).getType() == TokenType.LINE_END) && c == '/') {
                rawCommand = true;
                continue;
            }
            switch (c) {
                case '"': case '\'':
                    inString = true;
                    break;
                case ' ':
                    break;
                case '\n':
                    if (rawCommand) {
                        tokens.add(new Token(codePos,TokenType.RAW_COMMAND,temp));
                        rawCommand = false;
                        temp = "";
                    }
                    tokens.add(new Token(codePos,TokenType.LINE_END,"line end"));
                    line++;
                    break;
                default: {
                    if (signedNumber || Character.isDigit(c)) {
                        String numStr = "" + c;
                        for (; pos < str.length() - 1 && isInteger(numStr + str.charAt(pos)); pos++) {
                            numStr += str.charAt(pos);
                        }
                        if (pos < numStr.length() - 1) {
                            String doubleStr = numStr;
                            for (; pos < str.length() - 1 && isDouble(doubleStr + str.charAt(pos)); pos++) {
                                doubleStr += str.charAt(pos);
                            }
                            if (doubleStr.equals(numStr)) {
                                tokens.add(new Token(codePos,TokenType.INT,numStr));
                            } else {
                                tokens.add(new Token(codePos,TokenType.DOUBLE,doubleStr));
                            }
                        } else {
                            tokens.add(new Token(codePos,TokenType.INT,numStr));
                        }
                        signedNumber = false;
                    } else if (isOperator(c + "") && (pos >= str.length() || !isOperator(c + "" + str.charAt(pos) + ""))) {
                        if ((c == '+' || c == '-') && Character.isDigit(str.charAt(pos))) {
                            pos--;
                            signedNumber = true;
                            break;
                        } else {
                            tokens.add(new Token(codePos,TokenType.OPERATOR, c + ""));
                        }
                    } else if (pos < str.length() - 1 && isOperator(c + "" + str.charAt(pos) + "")) {
                        tokens.add(new Token(codePos,TokenType.OPERATOR,c + "" + str.charAt(pos++) + ""));
                    } else if (isSymbol(c)){
                        tokens.add(new Token(codePos,TokenType.SYMBOL,c + ""));
                    } else if (isIdentifier(c + "")){
                        String id = c + "";
                        for (; pos < str.length() && isIdentifier(id + str.charAt(pos)); pos++) {
                            id += str.charAt(pos);
                        }
                        tokens.add(new Token(codePos,TokenType.IDENTIFIER,id));
                    }
                    break;
                }
            }
            if (startLine == line) {
                column += pos - startPos;
            } else {
                column = 0;
            }
            codePos = new CodePos(file,line,column);
        }
        if (rawCommand) {
            tokens.add(new Token(codePos, TokenType.RAW_COMMAND, temp));
        }
        return tokens;
    }

    private static boolean isIdentifier(String str) {
        return str.matches("[a-zA-Z$_][a-zA-Z0-9$_]*");
    }

    private static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
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
