package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonTextParser {

    private static Map<String,TextComponentProperty> propertyMap = new HashMap<String, TextComponentProperty>() {{
        put("text",(p)->"\"" + p.tokens.next(TokenType.STRING) + "\"");
        put("selector",(p)-> {
            if (p.tokens.isNext(TokenType.STRING)) {
                return "\"" + SelectorParser.parseStringSelector(p.tokens.nextValue()) + "\"";
            }
            return p.selectors.parseSelector();
        });
    }};

    public static String readTextComponent(Parser parser) {
        TokenIterator tokens = parser.tokens;
        if (tokens.isNext(TokenType.STRING)) {
            return  "\"" + tokens.nextValue() + "\"";
        } else if (tokens.skip("{")) {
            Map<String,String> props = new HashMap<>();
            while (!tokens.isNext("}")) {
                String key = tokens.next(TokenType.STRING);
                TextComponentProperty prop = propertyMap.get(key);
                if (prop == null) {
                    throw new RuntimeException("Unknown JSON text property '" + key + "'");
                }
                tokens.expect(':');
                String value = prop.parse(parser);
                props.put(key,value);
                if (!tokens.skip(",") && !tokens.isNext("}")) {
                    throw new RuntimeException("Expected } or , after a JSON property");
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
                    throw new RuntimeException("Expected ] or , after a JSON array value");
                }
            }
            tokens.skip();
            arr += "]";
            return arr;
        }
        throw new RuntimeException("Invalid JSON text value!");
    }

    public interface TextComponentProperty {

        String parse(Parser parser);

    }

}
