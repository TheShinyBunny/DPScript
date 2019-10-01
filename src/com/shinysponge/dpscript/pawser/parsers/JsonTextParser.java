package com.shinysponge.dpscript.pawser.parsers;

import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.*;
import java.util.stream.Collectors;

public class JsonTextParser {

    private static final Map<String, JsonProperty> translateProps = new HashMap<String, JsonProperty>(){{
       put("key",(ctx)->ctx.nextString("translation key"));
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

    private static final Map<String, JsonProperty> hoverEventProps = new HashMap<String, JsonProperty>() {{
       put("action",ctx -> JsonValue.str(ctx.tokens.expect("show_text","show_item","show_entity")));
       put("value", ctx -> ctx.nextString("hover event value"));
    }};

    private static final Map<String, JsonProperty> clickEventProps = new HashMap<String, JsonProperty>() {{
        put("action",ctx -> JsonValue.str(ctx.tokens.expect("open_url","open_file","run_command","suggest_command","change_page")));
        put("value", ctx -> ctx.nextString("hover event value"));
    }};

    private static final Map<String, JsonProperty> propertyMap = new HashMap<String, JsonProperty>() {{
        put("text",(ctx)->ctx.nextString("plain text"));
        put("selector",(ctx)-> {
            if (ctx.tokens.isNext(TokenType.STRING)) {
                return JsonValue.str(SelectorParser.parseStringSelector(ctx.tokens.nextValue()));
            }
            ctx.tokens.expect('@');
            return JsonValue.str(SelectorParser.parseSelector());
        });
        put("color",(ctx)->{
           return JsonValue.str(ctx.tokens.expect("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "reset"));
        });
        put("runs",(ctx)->{
            return new JsonValue("clickEvent","{\"action\":\"run_command\",\"value\":\"/" + ctx.tokens.expect(TokenType.STRING,"command to run") + "\"}");
        });
        put("hover",(ctx)->{
            if (ctx.tokens.isNext(TokenType.STRING)) {
                return new JsonValue("hoverEvent","{\"action\":\"show_text\",\"value\":\"" + ctx.tokens.nextValue() + "\"}");
            }
            return readJson(ctx.withProps(hoverEventProps));
        });
        put("click", (ctx -> readJson(ctx.withProps(clickEventProps))));
        put("translate",(ctx)->{
            JsonValue value = readJson(ctx.withProps(translateProps));
            if (value.elements != null && value.elements.containsKey("args")) {
                ctx.parent.put("with", value.getElement("args"));
            }
            return value.require(ctx,"key");
        });
        put("bold", Context::nextBoolean);
        put("italic", Context::nextBoolean);
        put("underlined",Context::nextBoolean);
        put("strikethrough",Context::nextBoolean);
        put("obfuscated",Context::nextBoolean);
        put("insertion",(ctx)->ctx.nextString("insertion text"));
    }};


    public static String readTextComponent() {
        return readJson(new Context(new HashMap<>(),propertyMap)).toString();
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
        public final Map<String,JsonValue> parent;
        public final Map<String,JsonProperty> props;
        public final TokenIterator tokens;

        public Context(Map<String, JsonValue> parent, Map<String, JsonProperty> props) {
            this.parent = parent;
            this.tokens = Parser.tokens;
            this.props = props;
        }

        public void compilationError(ErrorType type, String msg) {
            Parser.compilationError(type, msg);
        }

        public Context withProps(Map<String, JsonProperty> props) {
            return new Context(parent,props);
        }

        public Context withParent(Map<String, JsonValue> parent) {
            return new Context(parent,props);
        }

        public JsonValue nextBoolean() {
            return new JsonValue(null,Boolean.parseBoolean(tokens.expect("true","false")));
        }

        public JsonValue nextString(String desc) {
            return JsonValue.str(tokens.expect(TokenType.STRING,desc));
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
