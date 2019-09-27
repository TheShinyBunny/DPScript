package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonTextParser {

    private static Map<String,TextComponentProperty> propertyMap = new HashMap<String, TextComponentProperty>() {{
        put("text",(p)->"\"" + p.tokens.next(TokenType.STRING,"text value") + "\"");
        put("selector",(p)-> {
            if (p.tokens.isNext(TokenType.STRING)) {
                return "\"" + SelectorParser.parseStringSelector(p,p.tokens.nextValue()) + "\"";
            }
            p.tokens.expect('@');
            return "\"" + p.selectors.parseSelector() + "\"";
        });
        put("color",(p)->{
           return "\"" + p.tokens.expect("red","green","blue","yellow","block","purple") + "\"";
        });
        put("runs",(p)->{
            return "{\"action\":\"run_command\",\"value\":\"" + p.tokens.next(TokenType.STRING,"command to run") + "\"}";
        });
        put("hover",(p)->{
            if (p.tokens.isNext(TokenType.STRING)) {
                return "{\"action\":\"show_text\",\"value\":\"" + p.tokens.nextValue() + "\"}";
            }
            return readTextComponent(p);
        });
    }};

    public static String readTextComponent(Parser parser) {
        TokenIterator tokens = parser.tokens;
        if (tokens.isNext(TokenType.STRING)) {
            return  "\"" + tokens.nextValue() + "\"";
        } else if (tokens.skip("{")) {
            Map<String,String> props = new HashMap<>();
            while (!tokens.isNext("}")) {
                String key = tokens.next(TokenType.STRING,"JSON key");
                TextComponentProperty prop = propertyMap.get(key);
                if (prop == null) {
                    parser.compilationError(ErrorType.UNKNOWN,"JSON text property '" + key + "'");
                    break;
                }
                tokens.expect(':');
                String value = prop.parse(parser);
                props.put(key,value);
                if (!tokens.skip(",") && !tokens.isNext("}")) {
                    parser.compilationError(ErrorType.EXPECTED,"} or , after a JSON property");
                }
            }
            tokens.skip();
            return props.entrySet().stream().map(e->"\"" + e.getKey() + "\":" + e.getValue()).collect(Collectors.joining(",","{","}"));
        } else if (tokens.skip("[")) {
            String arr = "[";
            while (!tokens.isNext("]")) {
                arr += readTextComponent(parser);
                if (tokens.skip(",")) {
                    arr += ",";
                } else if (!tokens.isNext("]")) {
                    parser.compilationError(ErrorType.EXPECTED,"] or , after a JSON array value");
                }
            }
            tokens.skip();
            arr += "]";
            return arr;
        }
        parser.compilationError(ErrorType.INVALID,"JSON component");
        return "{}";
    }

    public interface TextComponentProperty {

        String parse(Parser parser);

    }

}
