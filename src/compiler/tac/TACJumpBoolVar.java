/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.tac;
import compiler.Context.VarInfo;
import compiler.X86Emitter;
import compiler.type.TypeBoolean;

/**
 *
 * @author leijurv
 */
public class TACJumpBoolVar extends TACJump {
    String varName;
    VarInfo var;
    boolean invert;
    public TACJumpBoolVar(String varName, int jumpTo, boolean invert) {
        super(jumpTo);
        this.varName = varName;
        this.invert = invert;
    }
    @Override
    protected void onContextKnown() {
        var = context.get(varName);
        if (!(var.getType() instanceof TypeBoolean)) {
            throw new IllegalStateException("There is laterally no way this could happen. But I guess it did. lolripyou");
        }
    }
    @Override
    public String toString0() {
        return "jump to " + jumpTo + " if " + (invert ? "not " : "") + var;
    }
    @Override
    public void printx86(X86Emitter emit) {
        emit.addStatement("cmpl $0, " + var.x86());
        if (invert) {
            emit.addStatement("je " + emit.lineToLabel(jumpTo));
        } else {
            emit.addStatement("jne " + emit.lineToLabel(jumpTo));
        }
    }
}