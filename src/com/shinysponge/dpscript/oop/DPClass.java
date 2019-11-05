package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.project.MCFunction;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.*;

public class DPClass extends AbstractClass {

    public static final DPClass OBJECT = new DPClass();
    public static final PrimitiveClass<Number> NUMBER = new PrimitiveClass<>(Number.class, t->0);
    public static final PrimitiveClass<Double> DOUBLE = new PrimitiveClass<>(double.class, TokenIterator::readLiteralDouble, NUMBER);
    public static final PrimitiveClass<Integer> INT = new PrimitiveClass<>(int.class, TokenIterator::readLiteralInt, NUMBER);
    public static final PrimitiveClass<String> STRING = new PrimitiveClass<>(String.class, t->t.expect(TokenType.STRING,"string literal"));
    public static final PrimitiveClass<Boolean> BOOLEAN = new PrimitiveClass<>(boolean.class, TokenIterator::readLiteralBoolean);

    private String name;
    private AbstractClass superClass;
    private ParameterList constructorParams;
    private List<LazyValue<?>> superCall = new ArrayList<>();
    private List<DPField> fields = new ArrayList<>();
    private List<MCFunction> methods = new ArrayList<>();
    private Map<String,LazyValue> initFields = new HashMap<>();

    private DPClass() {
        this.name = "object";
        this.constructorParams = new ParameterList();
    }

    public DPClass(String name) {
        this(name,null);
    }

    public DPClass(String name, AbstractClass superClass, DPParameter... constructorParams) {
        this.name = name;
        this.superClass = superClass == null ? OBJECT : superClass;
        this.constructorParams = new ParameterList(Arrays.asList(constructorParams));
    }

    public DPClass addField(DPField field) {
        fields.add(field);
        return this;
    }


    public void addFields(DPField... fields) {
        this.fields.addAll(Arrays.asList(fields));
    }

    public void setInitField(String key, LazyValue value) {
        this.initFields.put(key,value);
    }

    public Map<String, LazyValue> getInitFields() {
        return initFields;
    }

    @Override
    public AbstractClass getSuperClass() {
        return superClass;
    }

    @Override
    public List<DPField> getFields() {
        return fields;
    }

    @Override
    public List<MCFunction> getFunctions() {
        return methods;
    }

    @Override
    public Object parseLiteral(TokenIterator tokens) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ParameterList getConstructor() {
        return constructorParams;
    }

    @Override
    public boolean isInstance(Object obj) {
        if (this == OBJECT) return true;
        if (obj instanceof LazyValue) {
            return isSuperOrSameAs(((LazyValue) obj).getType());
        }
        if (obj instanceof ClassInstance) {
            return isSuperOrSameAs(((ClassInstance) obj).getType());
        }
        return false;
    }

    public DPClass setSuperCall(List<LazyValue<?>> superCall) {
        this.superCall = superCall;
        return this;
    }

    public void addFunction(MCFunction function) {
        this.methods.add(function);
    }

    public DPClass createSubclass(String name, List<LazyValue<?>> superCall, DPParameter... params) {
        DPClass cls = new DPClass(name,this,params);
        cls.setSuperCall(superCall);
        return this;
    }

    public ClassInstance parseNewInstanceCreation() {
        List<LazyValue<?>> values = constructorParams.parseCall("constructor call");
        return new ClassInstance(this,values);
    }

    public List<LazyValue<?>> getSuperCall() {
        return superCall;
    }

    @Override
    public ClassInstance dummyInstance() {
        return new ClassInstance(this,new ArrayList<>());
    }
}
