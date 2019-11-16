package com.shinysponge.dpscript.pawser.score;

import com.shinysponge.dpscript.pawser.*;
import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.util.ArrayList;
import java.util.List;

public class LazyScoreValue implements Conditionable {

    private Score literal;
    private LazyScoreValue first;
    private ObjectiveOperators op;
    private LazyScoreValue second;

    public LazyScoreValue(Score literal) {
        this.literal = literal;
    }

    public LazyScoreValue(LazyScoreValue first, ObjectiveOperators op, LazyScoreValue second) {
        this.first = first;
        this.op = op;
        this.second = second;
    }

    public static LazyScoreValue parseExpression() {
        TokenIterator tokens = Parser.tokens;
        List<Object> expression = new ArrayList<>();
        LazyScoreValue lastValue = readSingleValue(tokens);
        expression.add(lastValue);
        while (tokens.isNext(TokenType.OPERATOR)) {
            String opcode = tokens.nextValue();
            ObjectiveOperators op = ObjectiveOperators.getNormal(opcode);
            if (op == null) {
                Parser.compilationError(null,"Invalid use of operator " + opcode);
                return null;
            }
            expression.add(op);
            lastValue = readSingleValue(tokens);
            expression.add(lastValue);
        }
        int priority = 1;
        while (expression.size() > 1) {
            for (int i = 0; i < expression.size(); i++) {
                Object obj = expression.get(i);
                if (obj instanceof ObjectiveOperators && ((ObjectiveOperators) obj).getPriority() == priority) {
                    Object first = expression.get(i-1);
                    Object second = expression.get(i+1);
                    LazyScoreValue result = new LazyScoreValue((LazyScoreValue)first,(ObjectiveOperators)obj,(LazyScoreValue)second);
                    expression.set(i,result);
                    expression.remove(i+1);
                    expression.remove(i-1);
                    i -= 2;
                }
            }
            priority++;
            if (priority > 7) {
                break;
            }
        }
        if (expression.size() > 1) {
            System.out.println(expression);
            Parser.compilationError(null,"cannot combine expression!");
        } else if (expression.isEmpty()) {
            Parser.compilationError(null,"expression is empty");
        } else if (!(expression.get(0) instanceof LazyScoreValue)) {
            Parser.compilationError(null,"invalid expression result");
        }
        LazyScoreValue result = (LazyScoreValue) expression.get(0);
        System.out.println("token after expression: " + tokens.peek());
        return result;
    }

    private static LazyScoreValue readSingleValue(TokenIterator tokens) {
        System.out.println("reading single score value: " + tokens.peek());
        if (tokens.skip("(")) {
            LazyScoreValue score = parseExpression();
            tokens.expect(")");
            return score;
        }
        if (tokens.isNext(TokenType.INT)) {
            return new LazyScoreValue(Score.of(tokens.readLiteralInt()));
        }
        if (tokens.isNext(TokenType.IDENTIFIER)) {
            Variable var = Parser.getContext().getVariable(tokens.peek().getValue());
            if (var != null && var.getType() == VariableType.SCORE) {
                return var.get(LazyScoreValue.class);
            }
        }
        Selector selector = SelectorParser.parseAnySelector(false);
        if (selector == null) return null;
        tokens.expect(".");
        String obj = tokens.expect(TokenType.IDENTIFIER,"objective name");
        if (!Parser.hasObjective(obj)) {
            Parser.compilationError(ErrorType.UNKNOWN,"objective " + obj);
        }
        return new LazyScoreValue(new EntryScore(obj,selector.toString()));
    }

    public LazyScoreValue getFirst() {
        return first;
    }

    public LazyScoreValue getSecond() {
        return second;
    }

    public ObjectiveOperators getOp() {
        return op;
    }

    @Override
    public void storeValue(List<String> cmds, EntryScore dest, int tempCount) {
        if (literal != null) {
            cmds.add(literal.toAssignCommand(dest));
        } else {
            cmds.addAll(op.toCommands(dest, first, second, tempCount));
        }
    }

    @Override
    public ConditionHolder getHolder() {
        return ScoreHolder.INSTANCE;
    }
}
