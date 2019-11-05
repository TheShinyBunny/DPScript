package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.entities.Key;
import com.shinysponge.dpscript.entities.NBT;

public class Item {

    @Key("id")
    private String id;
    @Key("tag")
    private NBT tag;
    @Key("Count")
    private int count;

    public Item(String id, NBT tag, int count) {
        this.id = id;
        this.tag = tag;
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public String getId() {
        return id;
    }

    public NBT getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return id + tag + (count == 1 ? "" : " " + count);
    }

    public NBT toNBT() {
        return new NBT(this);
    }
}
