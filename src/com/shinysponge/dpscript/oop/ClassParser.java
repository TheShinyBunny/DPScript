package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.pawser.*;
import com.shinysponge.dpscript.pawser.score.LazyScoreValue;
import com.shinysponge.dpscript.project.MCFunction;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassParser {

    public static DPClass parseClass(TokenIterator tokens) {
        String name = tokens.expect(TokenType.IDENTIFIER,"class name");
        if (Parser.getContext().hasClass(name)) {
            Parser.compilationError(ErrorType.DUPLICATE,"class named " + name);
        }
        List<DPParameter> params = new ArrayList<>();
        if (tokens.isNext("(")) {
            AtomicBoolean optional = new AtomicBoolean(false);
            params = Parser.readList('(',')', t->{
                DPParameter p = parseParameter(t,optional.get());
                if (p.isOptional()) {
                    optional.set(true);
                }
                return p;
            });
        }
        System.out.println("params: " + params);
        DPClass cls;
        Parser.getContext().enterBlock();
        for (DPParameter p : params) {
            Parser.getContext().putVariable(p.getName(),new Variable(VariableType.OBJECT,LazyValue.of(()->Parser.currentInstance.get(p.getName()),p.getType())));
        }
        if (tokens.skip("extends")) {
            String extendName = tokens.expect(TokenType.IDENTIFIER,"class name");
            AbstractClass superClass = findClass(extendName,true);
            if (superClass != null) {
                List<LazyValue<?>> superCall = superClass.getConstructor().parseCall("super constructor call");
                System.out.println("super call: " + superCall);
                cls = superClass.createSubclass(name,superCall,params.toArray(new DPParameter[0]));
            } else {
                return null;
            }
        } else {
            cls = new DPClass(name,null,params.toArray(new DPParameter[0]));
        }
        tokens.expect('{');
        tokens.skip(TokenType.LINE_END);
        parseBody(tokens,cls);
        System.out.println("init fields: " + cls.getInitFields());
        Parser.getContext().exitBlock();
        return cls;
    }

    private static void parseBody(TokenIterator tokens, DPClass cls) {
        while (!tokens.isNext("}")) {
            tokens.skip(TokenType.LINE_END);
            if (tokens.isNext("}")) break;
            parseProperty(tokens,cls);
            tokens.skip(",");
        }
        tokens.expect("}");
    }

    private static void parseProperty(TokenIterator tokens, DPClass cls) {
        if (tokens.skip("function")) {
            parseFunction(tokens,cls);
            return;
        }
        String key = tokens.expect(TokenType.IDENTIFIER,"class property key");
        DPField field = cls.findField(key);
        if (field == null) {
            tokens.error(ErrorType.UNKNOWN,"field key " + key);
            return;
        }
        tokens.expect(":","=");
        cls.setInitField(field.getKey(),field.parse(tokens,key));
    }

    private static void parseFunction(TokenIterator tokens, DPClass cls) {
        String name = tokens.expect(TokenType.IDENTIFIER,"function name");
        MCFunction function = cls.findFunction(name);
        tokens.skipAll("(",")");
        List<String> block = Parser.parseStatement(ScopeType.NORMAL);
        function.addAll(block);
        cls.addFunction(function);
    }

    public static List<LazyValue<?>> parseFunctionCall(ParameterList params) {
        AtomicInteger i = new AtomicInteger(0);
        List<LazyValue<?>> values = Parser.readList('(',')',t->readParameterValue(i.getAndIncrement(),params));
        int requiredCount = params.requiredCount();
        if (i.get() < requiredCount) {
            Parser.compilationError(ErrorType.MISSING,(requiredCount - i.get()) + " missing parameters");
        }
        System.out.println("before pack: " + values);
        if (i.get() >= params.size() && params.hasVarargs()) {
            LazyValue<List<?>> varargs = LazyValue.lazyList(values.subList(params.size()-1,values.size()));
            values = new ArrayList<>(values.subList(0,params.size()-1));
            values.add(varargs);
        }
        System.out.println("after pack: " + values);
        return values;
    }

    private static LazyValue<?> readParameterValue(int index, ParameterList parameters) {
        if (index >= parameters.size() && !parameters.hasVarargs()) {
            Parser.compilationError(null,"Trailing parameter");
            return LazyValue.NULL;
        }
        DPParameter param = parameters.get(index);
        AbstractClass type = param.getType();
        Object value = parseExpression(Parser.tokens,type);
        return LazyValue.of(()->value,type);
    }

    private static DPParameter parseParameter(TokenIterator tokens, boolean hasToBeOptional) {
        String typeName = tokens.expect(TokenType.IDENTIFIER,"parameter type or name");
        AbstractClass type = findClass(typeName,false);
        String name = typeName;
        if (tokens.isNext(TokenType.IDENTIFIER)) {
            if (type == null) {
                Parser.compilationError(ErrorType.UNKNOWN,"class " + typeName);
            }
            name = tokens.nextValue();
        } else {
            type = null;
        }
        boolean optional = false;
        Object defaultValue = null;
        boolean varargs = false;
        if (tokens.skip("?")) {
            optional = true;
        } else if (tokens.skip("=")) {
            optional = true;
            defaultValue = readSingleValue(tokens,type);
            if (defaultValue == null) {
                Parser.compilationError(ErrorType.INVALID,"object of " + (type == null ? "any type" : "type " + type.getName()));
            }
        } else if (tokens.skipAll(".",".",".")) {
            varargs = true;
        } if (hasToBeOptional) {
            Parser.compilationError(null,"Cannot have required parameters after optional parameters!");
        }
        DPParameter p = new DPParameter(name,type,optional,defaultValue);
        if (varargs) {
            p.setVarargs();
        }
        return p;
    }

    public static LazyValue<?> readSingleValue(TokenIterator tokens, AbstractClass type) {
        if (tokens.skip("(")) {
            LazyValue<?> exp = parseExpression(tokens,type);
            tokens.expect(')');
            return exp;
        }
        if (tokens.isNext(TokenType.IDENTIFIER) && !tokens.isNext("true","false")) {
            System.out.println("reading var single value");
            String name = tokens.nextValue();
            Variable v = Parser.getContext().getVariable(name);
            if (v == null) {
                Parser.compilationError(ErrorType.UNKNOWN,"variable " + name);
                return LazyValue.of(type == null ? ()->null : type::dummyInstance,type);
            }
            return v.getLazyValue();
        }
        if (type != null) {
            return LazyValue.literal(type.parseLiteral(tokens));
        }
        if (tokens.isNext(TokenType.STRING)) {
            return LazyValue.literal(tokens.nextValue());
        }
        if (tokens.isNext(TokenType.INT)) {
            return LazyValue.literal(tokens.readLiteralInt());
        }
        if (tokens.isNext(TokenType.DOUBLE)) {
            System.out.println("reading literal double");
            return LazyValue.literal(tokens.readLiteralDouble());
        }
        if (tokens.isNext("true","false")) {
            return LazyValue.literal(tokens.readLiteralBoolean());
        }
        System.out.println("null single value!");
        return LazyValue.NULL;
    }

    public static LazyValue<?> parseExpression(TokenIterator tokens, AbstractClass type) {
        List<Object> expression = new ArrayList<>();
        LazyValue<?> lastValue = readSingleValue(tokens,null);
        expression.add(lastValue);
        while (tokens.isNext(TokenType.OPERATOR)) {
            String opcode = tokens.nextValue();
            System.out.println("operator " + opcode);
            Operator<?,?> op = Operator.get(opcode,type);
            if (op == null) {
                Parser.compilationError(ErrorType.UNKNOWN,"operator " + opcode);
                return LazyValue.NULL;
            }
            expression.add(op);
            if (!op.getSourceType().isInstance(lastValue)) {
                Parser.compilationError(null,"operator " + op.getOp() + " cannot be applied to " + lastValue.getType());
            }
            LazyValue<?> prev = lastValue;
            lastValue = readSingleValue(tokens,null);
            if (!op.getSourceType().isInstance(lastValue)) {
                Parser.compilationError(null,"operator " + op.getOp() + " cannot be applied to " + prev.getType() + ", " + lastValue.getType());
            }
            System.out.println("token after operation: " + tokens.peek());
            expression.add(lastValue);
        }
        System.out.println(expression);
        int priority = 1;
        while (expression.size() > 1) {
            for (int i = 0; i < expression.size(); i++) {
                Object obj = expression.get(i);
                if (obj instanceof Operator && ((Operator) obj).getPriority() == priority) {
                    System.out.println("operator " + obj + " at index " + i);
                    Object first = expression.get(i-1);
                    Object second = expression.get(i+1);
                    LazyValue<?> result = LazyValue.combine(first,(Operator)obj,second);
                    expression.set(i,result);
                    System.out.println("after set: " + expression);
                    expression.remove(i+1);
                    System.out.println("after removing before: "+ expression);
                    expression.remove(i-1);
                    System.out.println("after merging: " + expression);
                    i -= 2;
                }
            }
            priority++;
            if (priority > 7) {
                break;
            }
        }
        if (expression.size() > 1) {
            System.out.println(expression);
            Parser.compilationError(null,"cannot combine expression!");
        } else if (expression.isEmpty()) {
            Parser.compilationError(null,"expression is empty");
        } else if (!(expression.get(0) instanceof LazyValue<?>)) {
            Parser.compilationError(null,"invalid expression result");
        }
        LazyValue<?> result = (LazyValue<?>) expression.get(0);
        /*if (type != null && !type.isSuperOrSameAs(result.getType())) {
            Parser.compilationError(null,"Expected operation of result type " + type + ", but got " + result.getType());
        }*/
        System.out.println("token after expression: " + tokens.peek());
        return result;
    }


    public static AbstractClass findClass(String name, boolean shouldThrow) {
        AbstractClass cls = Parser.getContext().classes.get(name);
        if (cls == null && shouldThrow) {
            Parser.compilationError(ErrorType.UNKNOWN,"class " + name);
        }
        return cls;
    }

}
