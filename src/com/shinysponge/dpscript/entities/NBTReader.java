package com.shinysponge.dpscript.entities;

import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NBTReader {

    public int readInt() {
        return Integer.parseInt(Parser.tokens.expect(TokenType.INT,"integer"));
    }

    public boolean readBoolean() {
        return Boolean.parseBoolean(Parser.tokens.expect("true","false"));
    }

    public <T> List<T> readNBTList(Supplier<T> valueParser) {
        List<T> list = new ArrayList<>();
        Parser.tokens.expect('[');
        while (Parser.tokens.hasNext() && !Parser.tokens.isNext("]")) {
            list.add(valueParser.get());
            if (!Parser.tokens.skip(",") && !Parser.tokens.isNext("]")) {
                Parser.compilationError(ErrorType.EXPECTED,", or ] to end NBT array");
            }
        }
        Parser.tokens.expect(']');
        return list;
    }


}
