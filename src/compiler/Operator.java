/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler;
import compiler.type.Type;
import compiler.type.TypeBoolean;
import compiler.type.TypeNumerical;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author leijurv
 */
public enum Operator {//extends Token maybe? might make things easier... idk
    PLUS("+", 50), MINUS("-", 50), MULTIPLY("*", 100), DIVIDE("/", 100), MOD("%", 1000), EQUAL("==", 10), NOT_EQUAL("!=", 10), GREATER(">", 10), LESS("<", 10), GREATER_OR_EQUAL(">=", 10), LESS_OR_EQUAL("<=", 10), OR("||", 4), AND("&&", 5);
    public static final ArrayList<List<Operator>> ORDER = orderOfOperations();//sorry this can't be the first line
    String str;
    int precedence;
    private Operator(String str, int precedence) {
        this.str = str;
        this.precedence = precedence;
    }
    @Override
    public String toString() {
        return str;
    }
    public static ArrayList<List<Operator>> orderOfOperations() {
        //Having it just be an array would put equal things next to each other, but not at the same place
        //For example, + might be sorted before - even though they have the same precedence
        //so, a-b+c might be parsed as a-(b+c)
        //having it be a 2d array fixes that
        Map<Integer, List<Operator>> precToOp = Stream.of(values()).collect(Collectors.groupingBy(op -> op.precedence));
        return Stream.of(values()).map(op -> op.precedence).distinct().sorted(Comparator.comparingInt(prec -> -prec)).map(prec -> precToOp.get(prec)).collect(Collectors.toCollection(ArrayList::new));
        //ArrayList<Operator> ops = new ArrayList<>(Arrays.asList(values()));
        //reverse order, so that the most important comes first (%) and least important comes last (&&, ||)
        //return ops;
    }
    public Type onApplication(Type a, Type b) {
        switch (this) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MOD:
                if (!a.equals(b)) {
                    throw new IllegalStateException("can't do " + this + " on " + a + " and " + b);
                }
                if (!(a instanceof TypeNumerical)) {
                    throw new IllegalStateException("can't do " + this + " on " + a + " and " + b);
                }
                return a;
            case EQUAL:
            case GREATER:
            case LESS:
            case GREATER_OR_EQUAL:
            case LESS_OR_EQUAL:
            case NOT_EQUAL:
                if (!(a instanceof TypeNumerical) || !(b instanceof TypeNumerical)) {
                    throw new IllegalStateException("can't do " + this + " on " + a + " and " + b);
                }
                return new TypeBoolean();
            case OR:
            case AND:
                if (!(a instanceof TypeBoolean) || !(b instanceof TypeBoolean)) {
                    throw new IllegalStateException("can't do " + this + " on " + a + " and " + b);
                }
                return new TypeBoolean();
            //dont add a default
        }
        throw new IllegalStateException("This could only happen if someone added a new operator but didn't implement calculating the type it returns. Operator in question: " + this);
    }
}
