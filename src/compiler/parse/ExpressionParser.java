/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.parse;
import compiler.Context;
import compiler.Operator;
import compiler.expression.Expression;
import compiler.expression.ExpressionConst;
import compiler.expression.ExpressionConstNum;
import compiler.expression.ExpressionConstStr;
import compiler.expression.ExpressionFunctionCall;
import compiler.expression.ExpressionOperator;
import compiler.expression.ExpressionVariable;
import compiler.token.Token;
import compiler.token.TokenComma;
import compiler.token.TokenEndParen;
import compiler.token.TokenKeyword;
import compiler.token.TokenNum;
import compiler.token.TokenOperator;
import compiler.token.TokenStartParen;
import compiler.token.TokenString;
import compiler.token.TokenVariable;
import compiler.type.Type;
import compiler.type.TypeBoolean;
import compiler.type.TypeInt32;
import compiler.type.TypeNumerical;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author leijurv
 */
public class ExpressionParser {
    private static Expression parseImpl(ArrayList<Object> o, Optional<Type> desiredType, Context context) {//the comments are todos, in order that they should be inserted (I got the order from kittehv1, assuming I
        System.out.println("EXPARSE " + o + " " + desiredType);
        int currentlyInParentheses = 0;
        for (int i = 0; i < o.size(); i++) {
            Object ob = o.get(i);
            if (ob instanceof TokenStartParen) {
                currentlyInParentheses++;
            }
            if (ob instanceof TokenEndParen) {
                currentlyInParentheses--;
            }
            if (ob instanceof TokenNum && currentlyInParentheses == 0) {//at any parenthetical level except the top, desiredType may be different, may as well parse down there
                String val = ((TokenNum) ob).val;
                Number num;
                TypeNumerical toUse = (desiredType.isPresent() && desiredType.get() instanceof TypeNumerical && !(desiredType.get() instanceof TypeBoolean)) ? (TypeNumerical) desiredType.get() : new TypeInt32();
                if (val.contains(".")) {
                    System.out.println("Parsing " + val + " as double: " + toUse);
                    num = Double.parseDouble(val);
                } else {
                    System.out.println("Parsing " + val + " as non float: " + toUse);
                    num = Integer.parseInt(val);
                }
                o.set(i, new ExpressionConstNum(num, toUse));//TODO this is mal
            }
            if (ob instanceof TokenString) {
                o.set(i, new ExpressionConstStr(((TokenString) ob).val));
            }
            if (ob instanceof TokenVariable) {
                if (i != o.size() - 1 && o.get(i + 1) instanceof TokenStartParen) {
                    //this is a pattern like f(
                    //indicates function call
                    //let's just like not
                    continue;
                }
                String name = ((TokenVariable) ob).val;
                Expression ex = new ExpressionVariable(name, context);
                Type type = ex.getType();
                if (type == null) {
                    throw new IllegalStateException("Trying to use " + name + " before it's defined");
                }
                o.set(i, ex);
            }
            if (ob instanceof TokenKeyword) {
                TokenKeyword tk = (TokenKeyword) ob;
                ExpressionConst ec = tk.getKeyword().getConstVal();
                if (ec != null) {
                    o.set(i, (Expression) ec);
                }
            }
        }
        if (o.size() == 1) {
            if (!(o.get(0) instanceof Expression)) {
                throw new IllegalStateException();
            }
            return (Expression) o.get(0);
        }
        //variable definitions / settings (=, :=)
        for (int i = 0; i < o.size(); i++) {//recursively call parseImpl on the contents of parentheses
            if (o.get(i) instanceof TokenStartParen) {
                ArrayList<ArrayList<Object>> inParen = new ArrayList<>();
                ArrayList<Object> temp = new ArrayList<>();
                int numParens = 1;
                o.remove(i);
                for (; i < o.size();) {
                    Object b = o.remove(i);
                    if (b instanceof TokenEndParen) {
                        numParens--;
                        if (numParens == 0) {
                            if (!temp.isEmpty()) {
                                inParen.add(temp);
                            }
                            System.out.println("Doing replace " + o + " " + inParen);
                            if (i != 0 && (o.get(i - 1) instanceof TokenVariable || o.get(i - 1) instanceof TokenKeyword)) {
                                String funcName;
                                if (o.get(i - 1) instanceof TokenVariable) {
                                    funcName = ((TokenVariable) o.get(i - 1)).val;
                                } else {
                                    funcName = ((TokenKeyword) o.get(i - 1)).toString();//some functions that you call are also keywords
                                }
                                ArrayList<Type> desiredTypes = context.gc.getHeader(funcName).inputs();
                                System.out.println("Expecting inputs: " + desiredTypes);
                                //tfw parallel expression parsing
                                //tfw this is a GOOD idea /s
                                ArrayList<Expression> args = IntStream.range(0, inParen.size()).parallel().mapToObj(p -> parseImpl(inParen.get(p), Optional.of(desiredTypes.get(p)), context)).collect(Collectors.toCollection(ArrayList::new));
                                o.remove(i - 1);
                                o.add(i - 1, new ExpressionFunctionCall(context, funcName, args));
                            } else {
                                if (inParen.size() != 1) {
                                    throw new IllegalStateException("This has commas or is empty, but isn't a function call " + inParen);
                                }
                                o.add(i, parseImpl(inParen.get(0), Optional.empty(), context));
                            }
                            return parseImpl(o, desiredType, context);
                        }
                    }
                    if (b instanceof TokenComma && numParens == 1) {
                        inParen.add(temp);
                        temp = new ArrayList<>();
                    } else {
                        temp.add(b);
                    }
                    if (b instanceof TokenStartParen) {
                        numParens++;
                    }
                }
                throw new IllegalStateException("mismatched ( and )");
            }
        }
        //inline array definitions a={5,6,7}     TODO: DECIDE TO USE { LIKE C/JAVA OR [ LIKE PYTHON/JAVASCRIPT
        //only three items in o; assume the middle one must be an operator
        //function calls (a TokenVariable then an Expression / ArrayList<Expression>)
        //getting array item (like arr[ind])
        /*for (int i = 0; i < o.size(); i++) {
         //increment and decrement
         }*/
        for (List<Operator> op : Operator.ORDER) {//order of operations
            for (int i = 0; i < o.size(); i++) {
                if (o.get(i) instanceof TokenOperator && op.contains(((TokenOperator) o.get(i)).op)) {
                    if (i == 0 || i == o.size() - 1) {
                        throw new IllegalStateException("Operator on edge. 411 hangs up on you.");
                    }
                    Expression rightSide = (Expression) o.remove(i + 1);
                    TokenOperator tokOp = (TokenOperator) o.remove(i);
                    Expression leftSide = (Expression) o.remove(i - 1);
                    o.add(i - 1, new ExpressionOperator(leftSide, tokOp.op, rightSide));
                    return parseImpl(o, Optional.empty(), context);//not all subexpressions should be the same desired type. like you might want a boolean overall but you might have i+1==2, where you expect i to be TypeNumerical
                }
            }
        }
        throw new IllegalStateException("Unable to parse " + o);
    }
    private static Expression purse(ArrayList<Object> o, Optional<Type> desiredType, Context context) {
        Expression r = parseImpl(o, desiredType, context);
        try {
            r.getType();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Exception while getting type of " + o, e);
        }
        if (desiredType.isPresent()) {
            if (!desiredType.get().equals(r.getType())) {
                throw new IllegalStateException(o + " should have been type " + desiredType.get() + " but was actually type " + r.getType());
            }
        }
        return r;
    }
    public static Expression parse(List<Token> tokens, Optional<Type> desiredType, Context context) {
        return purse(new ArrayList<>(tokens), desiredType, context);//this both casts each item from Token to Object, as well as cloning the arraylist because we are going to BUTCHER it
    }
    public static Expression parse(ArrayList<Token> tokens, Optional<Type> desiredType, Context context) {
        return purse(new ArrayList<>(tokens), desiredType, context);//this both casts each item from Token to Object, as well as cloning the arraylist because we are going to BUTCHER it
    }
}
