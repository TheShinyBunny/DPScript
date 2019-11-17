package com.shinysponge.dpscript.project;

import com.shinybunny.utils.json.Json;
import com.shinybunny.utils.json.JsonSerializable;

public interface Taggable extends JsonSerializable {

    String getName();

    Namespace getNamespace();

    @Override
    default Json toJson() {
        return Json.of(getNamespace() + ":" + getName());
    }
}
