package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

public class NBTDataParser {

    public static String parse(String selector) {
        TokenIterator tokens = Parser.tokens;
        if (tokens.skip("=")) {
            String nbt = Parser.parseNBT();
            return "data merge " + selector + " " + nbt;
        }
        tokens.expect('[');
        String path = tokens.expect(TokenType.STRING,"NBT path");
        tokens.expect(']');
        if (tokens.skip("=")) {
            if (tokens.isNext("byte","short","int","long","float","double")) {
                String type = tokens.expect(TokenType.IDENTIFIER,null);
                tokens.expect('(');
                String cmd = Parser.readExecuteRunCommand();
                tokens.expect(')');
                String result = "result";
                if (tokens.skip(".")) {
                    result = tokens.expect("result","success");
                }
                double scale = 1;
                if (tokens.skip("*")) {
                    scale = Double.parseDouble(tokens.nextValue());
                }
                return "execute store " + result + " " + selector + " " + path + " " + type + " " + scale + " " + cmd;
            } else {
                String source = Parser.parseNBTSource();
                return "data modify " + selector + " " + path + " set " + source;
            }
        } else if (tokens.skip(".")) {
            String methodLabel = tokens.expect(TokenType.IDENTIFIER,"NBT data method");
            String method = null;
            tokens.expect('(');
            if ("remove".equals(methodLabel) || "delete".equals(methodLabel)) {
                tokens.expect(')');
                return "data remove " + selector + " " + path;
            }
            if ("insert".equals(methodLabel)) {
                method = "insert " + tokens.expect(TokenType.INT,"insertion index");
                tokens.expect(',');
            }
            String source = Parser.parseNBTSource();
            tokens.expect(')');
            switch (methodLabel) {
                case "push":
                case "add":
                case "append":
                    method = "append";
                    break;
                case "merge":
                    method = "merge";
                    break;
                case "prepend":
                case "unshift":
                    method = "prepend";
                    break;
                    default:
                        Parser.compilationError(ErrorType.UNKNOWN,"NBT data method " + method);
            }
            return "data modify " + selector + " " + path + " " + method + " " + source;
        } else {
            double scale = 1;
            if (tokens.skip("*")) {
                if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) {
                    scale = Double.parseDouble(tokens.nextValue());
                }
            }
            return "data get " + selector + " " + path + " " + scale;
        }
    }
}
