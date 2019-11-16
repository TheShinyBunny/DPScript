package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.entities.NBT;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.pawser.Variable;
import com.shinysponge.dpscript.pawser.VariableType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassInstance {

    private DPClass type;
    private Map<String,Object> variables;

    public ClassInstance(DPClass type, List<LazyValue<?>> constructorParams) {
        this.type = type;
        this.variables = new HashMap<>();
        for (int i = 0; i < constructorParams.size(); i++) {
            DPParameter p = type.getConstructor().get(i);
            variables.put(p.getName(),LazyValue.valueOf(constructorParams.get(i)));
        }
        Parser.currentInstance = this;
        Parser.getContext().enterBlock();
        for (Map.Entry<String,Object> e : variables.entrySet()) {
            Parser.getContext().putVariable(e.getKey(),new Variable(VariableType.OBJECT,e.getValue()));
        }
        if (type.getSuperClass() != null) {
            for (DPField f : type.getAllFields()) {
                Object o = type.getInitFields().get(f.getKey());
                if (o != null) {
                    variables.put(f.getKey(), LazyValue.valueOf(o));
                }
            }
        }
        if (type.getSuperClass() instanceof DPClass && type.getSuperCall() != null) {
            ClassInstance superInstance = new ClassInstance((DPClass) type.getSuperClass(),type.getSuperCall());
            variables.putAll(superInstance.variables);
        }
        Parser.getContext().exitBlock();
        Parser.currentInstance = null;
    }

    public void set(String field, Object value) {
        variables.put(field,value);
    }

    public Object get(String field) {
        return variables.get(field);
    }

    public DPClass getType() {
        return type;
    }

    public NBT toNBT() {
        NBT nbt = new NBT();
        for (Map.Entry<String,Object> v : variables.entrySet()) {
            nbt.put(v.getKey(),LazyValue.valueOf(v.getValue()));
        }
        return nbt;
    }

    @Override
    public String toString() {
        return type.getName() + variables;
    }
}
