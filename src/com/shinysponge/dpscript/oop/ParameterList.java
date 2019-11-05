package com.shinysponge.dpscript.oop;

import com.shinysponge.dpscript.pawser.ErrorType;
import com.shinysponge.dpscript.pawser.Parser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParameterList implements Iterable<DPParameter> {

    private List<DPParameter> params;

    public ParameterList(List<DPParameter> params) {
        this.params = params;
    }

    public ParameterList() {
        this.params = new ArrayList<>();
    }

    public ParameterList add(DPParameter parameter) {
        this.params.add(parameter);
        return this;
    }

    public boolean hasVarargs() {
        return !isEmpty() && params.get(params.size() - 1).isVarargs();
    }

    public int size() {
        return params.size();
    }

    public int requiredCount() {
        int count = 0;
        for (DPParameter p : params) {
            if (p.isOptional()) break;
            count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public boolean hasRequired() {
        for (DPParameter p : params) {
            if (!p.isOptional()) {
                return true;
            }
        }
        return false;
    }

    public DPParameter get(int index) {
        if (hasVarargs()) {
            if (index >= size()-1) {
                return params.get(params.size()-1);
            }
        }
        return params.get(index);
    }

    @Override
    public Iterator<DPParameter> iterator() {
        return params.iterator();
    }

    public List<LazyValue<?>> parseCall(String description) {
        System.out.println(params);
        TokenIterator tokens = Parser.tokens;
        if (tokens.isNext("(")) {
            return ClassParser.parseFunctionCall(this);
        } else if (hasRequired()){
            Parser.compilationError(ErrorType.MISSING,description + " (it takes required parameters)");
        }
        return new ArrayList<>();
    }
}
