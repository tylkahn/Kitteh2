/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.tac;
import compiler.Context.VarInfo;
import compiler.Operator;
import compiler.type.TypeInt64;
import compiler.type.TypeNumerical;
import compiler.type.TypePointer;
import compiler.x86.X86Comparison;
import compiler.x86.X86Const;
import compiler.x86.X86Emitter;
import compiler.x86.X86Param;
import compiler.x86.X86Register;
import compiler.x86.X86TypedRegister;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalChannelGroupException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author leijurv
 */
public class TACStandard extends TACStatement {
    public final Operator op;
    public TACStandard(String resultName, String firstName, String secondName, Operator op) {
        super(firstName, secondName, resultName);
        try {
            Integer.parseInt(firstName);
            Integer.parseInt(secondName);
            throw new IllegalChannelGroupException();
        } catch (NumberFormatException e) {
        }
        this.op = op;
    }
    @Override
    public List<String> requiredVariables() {
        return Arrays.asList(paramNames[0], paramNames[1]);
    }
    @Override
    public List<String> modifiedVariables() {
        return Arrays.asList(paramNames[2]);
    }
    @Override
    public String toString0() {
        return params[2] + " = " + params[0] + " " + op + " " + params[1];
    }
    @Override
    public void onContextKnown() {
        if (!params[2].getType().equals(op.onApplication(params[0].getType(), params[1].getType()))) {
            throw new IllegalThreadStateException();
        }
        if (!params[0].getType().equals(params[1].getType())) {
            if (params[0].getType() instanceof TypePointer) {
                return;
            }
            throw new IllegalStateException(this + "");
        }
    }
    @Override
    public void printx86(X86Emitter emit) {
        String firstName = paramNames[0];//i literally can't be bothered
        String secondName = paramNames[1];
        X86Param first = params[0];
        X86Param second = params[1];
        X86Param result = params[2];
        TypeNumerical type;
        if (firstName.startsWith(X86Register.REGISTER_PREFIX)) {
            type = X86Register.typeFromRegister(firstName);
        } /*else if (first == null) {
            if (second == null) {
                throw new RuntimeException("that optimization related exception again " + this);
                //type = (TypeNumerical) result.getType(); //this is a workaround for when i'm lazy
            } else {
                type = (TypeNumerical) second.getType();
            }
        }*/ else {
            type = (TypeNumerical) first.getType();
        }
        X86TypedRegister a = X86Register.A.getRegister(type);
        X86TypedRegister c = X86Register.C.getRegister(type);
        X86TypedRegister d = X86Register.D.getRegister(type);
        String mov = "mov" + type.x86typesuffix() + " ";
        TACConst.move(a, first, emit);
        if (type instanceof TypePointer && (second instanceof VarInfo || second instanceof X86Const)) {//if second is null that means it's a const in secondName, and if that's the case we don't need to do special cases
            //pointer arithmetic, oh boy pls no
            //what are we adding to the pointer
            if (!(second.getType() instanceof TypeNumerical)) {
                throw new ClosedSelectorException();
            }
            if (!second.getType().getClass().toString().contains("TypeInt")) {//look bud i'm not perfect
                throw new IllegalStateException(this + " " + second.getType().toString() + " " + second.getClass());
            }
            //we put the pointer in A
            //and the integer in C
            if (second instanceof X86Const) {
                second = new X86Const(((X86Const) second).getName(), new TypeInt64());//its probably a const int that we are trying to add to an 8 byte pointer
                //since its literally a const number, just change the type
            }
            if (second.getType().getSizeBytes() == first.getType().getSizeBytes() || second instanceof X86Const) {
                TACConst.move(X86Register.C.getRegister(type), second, emit);
            } else {
                emit.addStatement("movs" + ((TypeNumerical) second.getType()).x86typesuffix() + "q " + second.x86() + "," + X86Register.C.getRegister(new TypeInt64()));
            }
        } else {
            try {
                TACConst.move(X86Register.C.getRegister(type), second, emit);
            } catch (Exception e) {
                throw new UnsupportedOperationException(this + " " + type + " " + firstName + " " + secondName, e);
            }
        }
        switch (op) {
            case PLUS:
                emit.addStatement("add" + type.x86typesuffix() + " " + c + ", " + a);
                emit.addStatement(mov + a + ", " + result.x86());
                break;
            case MINUS:
                emit.addStatement("sub" + type.x86typesuffix() + " " + c + ", " + a);
                emit.addStatement(mov + a + ", " + result.x86());
                break;
            case MOD:
                emit.addStatement("xor" + type.x86typesuffix() + " " + d + ", " + d);
                emit.addStatement("idiv" + type.x86typesuffix() + " " + c);
                emit.addStatement(mov + d + ", " + result.x86());
                break;
            case DIVIDE:
                emit.addStatement("xor" + type.x86typesuffix() + " " + d + ", " + d);
                emit.addStatement("idiv" + type.x86typesuffix() + " " + c);
                emit.addStatement(mov + a + ", " + result.x86());
                break;
            case MULTIPLY:
                emit.addStatement("imul" + type.x86typesuffix() + " " + c + ", " + a);
                emit.addStatement(mov + a + ", " + result.x86());
                break;
            case LESS:
            case GREATER:
            case EQUAL:
            case NOT_EQUAL:
            case GREATER_OR_EQUAL:
            case LESS_OR_EQUAL:
                emit.addStatement("cmp" + type.x86typesuffix() + " " + c + ", " + a);
                emit.addStatement(X86Comparison.tox86set(op) + " %cl");
                emit.addStatement("movb %cl, " + result.x86());
                break;
            default:
                throw new IllegalStateException(op + "");
        }
    }
}
