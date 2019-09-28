package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.*;
import java.util.stream.Collectors;

public class JsonTextParser {

    private static Map<String, JsonProperty> translateProps = new HashMap<String, JsonProperty>(){{
       put("key",(ctx)->JsonValue.str(ctx.tokens.next(TokenType.STRING,"translation key")));
       put("args",(ctx)->{
           JsonValue value = readJson(ctx);
           if (value.value != null) {
               return new JsonValue(Collections.singletonList(value));
           } else if (value.children == null || !value.children.stream().allMatch(v->v.value != null)) {
               ctx.compilationError(ErrorType.INVALID,"format arguments, must be a string or a list of strings");
           }
           return value;
       });
    }};

    private static Map<String, JsonProperty> propertyMap = new HashMap<String, JsonProperty>() {{
        put("text",(ctx)->JsonValue.str(ctx.tokens.next(TokenType.STRING,"text value")));
        put("selector",(ctx)-> {
            if (ctx.tokens.isNext(TokenType.STRING)) {
                return JsonValue.str(SelectorParser.parseStringSelector(ctx.parser,ctx.tokens.nextValue()));
            }
            ctx.tokens.expect('@');
            return JsonValue.str(ctx.parser.selectors.parseSelector());
        });
        put("color",(ctx)->{
           return JsonValue.str(ctx.tokens.expect("red","green","blue","yellow","block","purple"));
        });
        put("runs",(ctx)->{
            return new JsonValue("clickEvent","{\"action\":\"run_command\",\"value\":\"" + ctx.tokens.next(TokenType.STRING,"command to run") + "\"}");
        });
        put("hover",(ctx)->{
            if (ctx.tokens.isNext(TokenType.STRING)) {
                return new JsonValue("hoverEvent","{\"action\":\"show_text\",\"value\":\"" + ctx.tokens.nextValue() + "\"}");
            }
            return new JsonValue("hoverEvent","{\"action\":\"show_text\",\"value\":\"" + readTextComponent(ctx.parser) + "\"}");
        });
        put("translate",(ctx)->{
            JsonValue value = readJson(ctx.withProps(translateProps));
            if (value.elements != null && value.elements.containsKey("args")) {
                ctx.parent.put("with", value.getElement("args"));
            }
            return value.require(ctx,"key");
        });
    }};

    public static String readTextComponent(Parser parser) {
        return readJson(new Context(parser,null,propertyMap)).toString();
    }

    public static JsonValue readJson(Context ctx) {
        TokenIterator tokens = ctx.tokens;
        if (tokens.isNext(TokenType.STRING)) {
            return  JsonValue.str(tokens.nextValue());
        } else if (tokens.skip("{")) {
            Map<String,JsonValue> map = new HashMap<>();
            Context subCtx = ctx.withParent(map);
            while (!tokens.isNext("}")) {
                if (!tokens.isNext(TokenType.STRING,TokenType.IDENTIFIER)) {
                    ctx.compilationError(ErrorType.EXPECTED,"JSON component key");
                }
                String key = tokens.nextValue();
                JsonProperty prop = ctx.props.get(key);
                if (prop == null) {
                    ctx.compilationError(ErrorType.UNKNOWN,"JSON text property '" + key + "'");
                    break;
                }
                tokens.expect(':');
                JsonValue value = prop.parse(subCtx);
                if (value.key == null) {
                    map.put(key, value);
                } else {
                    map.put(value.key,value);
                }
                if (!tokens.skip(",") && !tokens.isNext("}")) {
                    ctx.compilationError(ErrorType.EXPECTED,"} or , after a JSON property");
                }
            }
            tokens.skip();
            return new JsonValue(map);
        } else if (tokens.skip("[")) {
            List<JsonValue> values = new ArrayList<>();
            while (!tokens.isNext("]")) {
                values.add(readJson(ctx));
                if (!tokens.skip(",") && !tokens.isNext("]")) {
                    ctx.compilationError(ErrorType.EXPECTED,"] or , after a JSON array value");
                }
            }
            tokens.skip();
            return new JsonValue(values);
        }
        ctx.compilationError(ErrorType.INVALID,"JSON component");
        return JsonValue.NULL;
    }

    public interface JsonProperty {

        JsonValue parse(Context ctx);

    }

    public static class Context {
        public final Parser parser;
        public final Map<String,JsonValue> parent;
        public final Map<String,JsonProperty> props;
        public final TokenIterator tokens;

        public Context(Parser parser, Map<String, JsonValue> parent, Map<String, JsonProperty> props) {
            this.parser = parser;
            this.parent = parent;
            this.tokens = parser.tokens;
            this.props = props;
        }

        public void compilationError(ErrorType type, String msg) {
            parser.compilationError(type, msg);
        }

        public Context withProps(Map<String, JsonProperty> props) {
            return new Context(parser,parent,props);
        }

        public Context withParent(Map<String, JsonValue> parent) {
            return new Context(parser,parent,props);
        }
    }

    /**
     * Represents a JSON value that could be a JSON Object (when {@link #elements} is not null),
     * a JSON Array (when {@link #children} is not null),
     * or a JSON primitive value (when {@link #value} is not null).<br/>
     * The {@link #key} property is used to bind this element to a different key in the parent JSON object.
     */
    public static class JsonValue {

        public static final JsonValue NULL = new JsonValue(null,null);

        private Map<String,JsonValue> elements;
        private List<JsonValue> children;
        private String key;
        private Object value;

        public JsonValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public JsonValue(Map<String, JsonValue> elements) {
            this.elements = elements;
        }

        public JsonValue(List<JsonValue> children) {
            this.children = children;
        }

        public static JsonValue str(String str) {
            return new JsonValue(null,"\"" + str + "\"");
        }

        @Override
        public String toString() {
            if (elements != null) {
                return elements.entrySet().stream().map(e->"\"" + e.getKey() + "\":" + e.getValue()).collect(Collectors.joining(",","{","}"));
            }
            if (children != null) {
                return children.stream().map(JsonValue::toString).collect(Collectors.joining(",","[","]"));
            }
            return String.valueOf(value);
        }

        public JsonValue require(Context ctx, String key) {
            if (elements == null) {
                ctx.compilationError(ErrorType.EXPECTED, "a JSON Object");
                return NULL;
            }
            if (!elements.containsKey(key)) {
                ctx.compilationError(ErrorType.MISSING,"key " + key + " in json object");
                return NULL;
            }
            return elements.get(key);
        }

        public JsonValue getElement(String key) {
            return elements == null ? NULL : elements.get(key);
        }
    }

}