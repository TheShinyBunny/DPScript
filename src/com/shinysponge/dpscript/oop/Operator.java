package com.shinysponge.dpscript.oop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Operator<T,R> {
    private static final List<Operator<?,?>> OPERATOR_LIST = new ArrayList<>();

    public static final Operator<Number,Number> MULTIPLY = new Operator<>(1, "*",DPClass.NUMBER,DPClass.NUMBER,(l, r)->l.doubleValue() + r.doubleValue());
    public static final Operator<Number,Number> DIVIDE = new Operator<>(1, "/",DPClass.NUMBER,DPClass.NUMBER,(l, r)->l.doubleValue() / r.doubleValue());
    public static final Operator<Number,Number> ADD = new Operator<>(2, "+",DPClass.NUMBER,DPClass.NUMBER, (l,r)->l.doubleValue() + r.doubleValue());
    public static final Operator<String,String> ADD_STRINGS = new Operator<>(3, "+",DPClass.STRING,DPClass.STRING, (l, r)->l + r);
    public static final Operator<Number,Number> SUBTRACT = new Operator<>(2, "-",DPClass.NUMBER,DPClass.NUMBER,(l, r)->l.doubleValue() - r.doubleValue());
    public static final Operator<Number,Boolean> EQUAL_NUMBER = new Operator<>(4, "==",DPClass.NUMBER,DPClass.BOOLEAN, (l,r)->l.doubleValue() == r.doubleValue());
    public static final Operator<Number,Boolean> NOT_EQUAL_NUMBER = new Operator<>(4, "!=",DPClass.NUMBER,DPClass.BOOLEAN, (l, r)->!l.equals(r));
    public static final Operator<Number,Boolean> LESS_THAN = new Operator<>(5, "<",DPClass.NUMBER,DPClass.BOOLEAN, (l, r)->l.doubleValue() < r.doubleValue());
    public static final Operator<Number,Boolean> GREATER_THAN = new Operator<>(5, ">",DPClass.NUMBER,DPClass.BOOLEAN, (l, r)->l.doubleValue() > r.doubleValue());
    public static final Operator<Number,Boolean> LESS_THAN_EQUAL = new Operator<>(5, "<=",DPClass.NUMBER,DPClass.BOOLEAN, (l, r)->l.doubleValue() <= r.doubleValue());
    public static final Operator<Number,Boolean> GREATER_THAN_EQUAL = new Operator<>(5, ">=",DPClass.NUMBER,DPClass.BOOLEAN, (l, r)->l.doubleValue() >= r.doubleValue());
    public static final Operator<Boolean,Boolean> OR = new Operator<>(6, "||",DPClass.BOOLEAN,DPClass.BOOLEAN,(l, r)->l || r);
    public static final Operator<Boolean,Boolean> AND = new Operator<>(7, "&&",DPClass.BOOLEAN,DPClass.BOOLEAN,(l, r)->l && r);

    private final int priority;
    private final String op;
    private final PrimitiveClass<T> sourceType;
    private final PrimitiveClass<R> resultType;
    private final BiFunction<T, T, R> operation;

    public Operator(int priority, String op, PrimitiveClass<T> sourceType, PrimitiveClass<R> resultType, BiFunction<T, T, R> operation) {
        this.priority = priority;
        this.op = op;
        this.sourceType = sourceType;
        this.resultType = resultType;
        this.operation = operation;
        OPERATOR_LIST.add(this);
    }

    public static Operator<?, ?> get(String op, AbstractClass type) {
        for (Operator<?,?> o : OPERATOR_LIST) {
            if (o.op.equalsIgnoreCase(op) && (type == null || o.sourceType.isSuperOrSameAs(type))) return o;
        }
        return null;
    }

    public String getOp() {
        return op;
    }

    public BiFunction<T, T, R> getOperation() {
        return operation;
    }

    public PrimitiveClass<R> getResultType() {
        return resultType;
    }

    public PrimitiveClass<T> getSourceType() {
        return sourceType;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return sourceType.getName() + op + sourceType.getName() + " = " + resultType.getName();
    }

    public AbstractClass getResultTypeFor(Object first, Object second) {
        if (this.resultType == DPClass.NUMBER) {
            if (LazyValue.typeOf(first) == DPClass.DOUBLE || LazyValue.typeOf(second) == DPClass.DOUBLE) {
                return DPClass.DOUBLE;
            }
            return DPClass.INT;
        }
        return resultType;
    }
}
