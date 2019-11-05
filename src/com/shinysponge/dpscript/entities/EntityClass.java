package com.shinysponge.dpscript.entities;

import com.shinybunny.utils.MapBuilder;
import com.shinysponge.dpscript.oop.*;
import com.shinysponge.dpscript.pawser.selector.Selector;
import com.shinysponge.dpscript.pawser.selector.SimpleSelector;

import java.util.*;

public class EntityClass extends DPClass {

    private Entities type;
    private List<LazyValue<?>> tags;

    public EntityClass(String name, Entities type, List<LazyValue<?>> tags, DPParameter... params) {
        super(name,type.getTypeClass(),params);
        this.type = type;
        this.tags = tags;
    }

    public EntityClass(String name, AbstractClass superClass, Entities type) {
        super(name, superClass,new DPParameter("Tags",DPClass.STRING,true,new ArrayList<>()).setVarargs());
        this.type = type;
    }

    public Selector createSelector(ClassInstance instance) {
        if (this == instance.getType()) {
            System.out.println(instance);
            return new SimpleSelector('e', MapBuilder.of("type",this.type.getId()).and("tag",Selector.toMultiParams("tag", (List<String>) instance.get("Tags"))));
        }
        return null;
    }

    public Entities getType() {
        return type;
    }

    public List<LazyValue<?>> getTags() {
        return tags;
    }

    @Override
    public DPClass createSubclass(String name, List<LazyValue<?>> superCall, DPParameter... params) {
        return new EntityClass(name,this.type,superCall,params).setSuperCall(superCall);
    }
}
