package com.shinysponge.dpscript.pawser;

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
}
