/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.command;
import compiler.Context;
import compiler.expression.Expression;
import compiler.expression.ExpressionConst;
import compiler.tac.IREmitter;
import compiler.tac.TempVarUsage;

/**
 *
 * @author leijurv
 */
public class CommandSetVar extends Command {
    Expression val;
    String var;
    public CommandSetVar(String var, Expression val, Context context) {
        super(context);
        this.val = val;
        this.var = var;
    }
    @Override
    public void generateTAC(Context context, IREmitter emit) {
        val.generateTAC(context, emit, new TempVarUsage(), var);//this one, at least, is easy
    }
    @Override
    protected int calculateTACLength() {
        return val.getTACLength();
    }
    @Override
    public void staticValues() {
        val = val.insertKnownValues(context);
        val = val.calculateConstants();
        if (val instanceof ExpressionConst) {
            context.setKnownValue(var, (ExpressionConst) val);
        } else {
            context.clearKnownValue(var);//we are setting it to something dynamic, so it's changed now
        }
    }
}
