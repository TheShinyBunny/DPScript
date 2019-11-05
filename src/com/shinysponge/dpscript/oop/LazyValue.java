package com.shinysponge.dpscript.oop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface LazyValue<T> {

    LazyValue<?> NULL = LazyValue.of(()->null,null);

    static LazyValue<?> combine(Object first, Operator op, Object second) {
        return LazyValue.of(()->op.getOperation().apply(valueOf(first),valueOf(second)),op.getResultTypeFor(first,second));
    }

    static <T> LazyValue<T> of(Supplier<T> value, AbstractClass type) {
        return new LazyValue<T>() {
            @Override
            public T eval() {
                return value.get();
            }

            @Override
            public AbstractClass getType() {
                return type == null ? DPClass.OBJECT : type;
            }
        };
    }

    static Object valueOf(Object obj) {
        if (obj instanceof LazyValue) {
            return valueOf(((LazyValue) obj).eval());
        }
        return obj;
    }

    static <T> LazyValue<T> literal(T literal) {
        return LazyValue.of(()->literal,PrimitiveClass.of(literal));
    }

    static LazyValue<List<?>> lazyList(List<LazyValue<?>> list) {
        return LazyValue.of(()->{
            List values = new ArrayList<>();
            for (LazyValue<?> v : list) {
                values.add(LazyValue.valueOf(v));
            }
            return values;
        },null);
    }

    static AbstractClass typeOf(Object first) {
        if (first instanceof LazyValue) {
            return ((LazyValue) first).getType();
        }
        return PrimitiveClass.of(first);
    }

    T eval();

    AbstractClass getType();

    default <R> LazyValue<R> map(Function<T,R> func) {
        AbstractClass type = getType();
        LazyValue<T> prev = this;
        return new LazyValue<R>() {
            @Override
            public R eval() {
                return func.apply(prev.eval());
            }

            @Override
            public AbstractClass getType() {
                return type;
            }
        };
    }

}
