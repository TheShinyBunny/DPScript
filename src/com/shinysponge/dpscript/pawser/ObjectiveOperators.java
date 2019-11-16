package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.score.EntryScore;
import com.shinysponge.dpscript.pawser.score.LazyScoreValue;
import com.shinysponge.dpscript.pawser.score.Score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ObjectiveOperators {
    EQUALS("set","="),
    PLUS("add","+="),
    MINUS("remove","-="),
    MULTIPLY("*="),
    DIVIDE("/="),
    MODULUS("%="),
    LESS_THAN(null,"<","<="),
    GREATER_THAN(null,">",">="),
    SWAP("><"),
    INCREMENT("add","++",true),
    DECREMENT("remove","--",true);


    private final String literalCommand;
    private final String operator;
    private final String operationOperator;
    private boolean unary;

    private static Map<String,ObjectiveOperators> normals = new HashMap<>();
    private static Map<ObjectiveOperators, Integer> priorityTable = new HashMap<>();

    static {
        normals.put("+",PLUS);
        normals.put("-",MINUS);
        normals.put("*",MULTIPLY);
        normals.put("/",DIVIDE);
        normals.put("%",MODULUS);

        priorityTable.put(MULTIPLY,1);
        priorityTable.put(DIVIDE,1);
        priorityTable.put(MODULUS,1);
        priorityTable.put(PLUS,2);
        priorityTable.put(MINUS,2);
    }



    ObjectiveOperators(String literalCommand, String operator) {
        this(literalCommand,operator,operator);
    }

    ObjectiveOperators(String literalCommand, String operator, boolean unary) {
        this(literalCommand, operator);
        this.unary = unary;
    }

    ObjectiveOperators(String literalCommand, String operator, String operationOperator) {
        this.literalCommand = literalCommand;
        this.operator = operator;
        this.operationOperator = operationOperator;
    }

    ObjectiveOperators(String operator) {
        this(null,operator,operator);
    }

    public static ObjectiveOperators getNormal(String opcode) {
        return normals.get(opcode);
    }

    public String getLiteralCommand() {
        return literalCommand;
    }

    public String getOperationOperator() {
        return operationOperator;
    }

    public String getOperator() {
        return operator;
    }

    public boolean isUnary() {
        return unary;
    }

    public List<String> toCommands(EntryScore dest, LazyScoreValue first, LazyScoreValue second, int temps) {
        List<String> list = new ArrayList<>();
        EntryScore temp1 = Score.global("_exprTemp" + temps);
        EntryScore temp2 = Score.global("_exprTemp" + (temps + 1));
        first.storeValue(list,temp1,temps + 2);
        second.storeValue(list,temp2,temps + 2);
        list.add("scoreboard players operation " + temp1 + " " + operationOperator + " " + temp2);
        list.add("scoreboard players operation " + dest + " = " + temp1);
        return list;
    }

    public int getPriority() {
        return priorityTable.getOrDefault(this,0);
    }
}
