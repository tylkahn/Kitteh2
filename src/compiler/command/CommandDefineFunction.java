/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.command;
import compiler.Context;
import compiler.FunctionsContext;
import compiler.Keyword;
import compiler.X86Emitter;
import compiler.parse.Processor;
import compiler.tac.IREmitter;
import compiler.tac.TACStatement;
import compiler.tac.optimize.TACOptimizer;
import compiler.type.Type;
import compiler.type.TypeInt32;
import compiler.type.TypePointer;
import compiler.type.TypeVoid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import javafx.util.Pair;

/**
 *
 * @author leijurv
 */
public class CommandDefineFunction extends Command {//dont extend commandblock because we only get the contents later because of header first parsing
    ArrayList<Pair<String, Type>> arguments;
    Type returnType;
    String name;
    ArrayList<Command> contents;
    ArrayList<Object> rawContents;
    FunctionHeader header;
    public CommandDefineFunction(Context context, Type returnType, ArrayList<Pair<String, Type>> arguments, String functionName, ArrayList<Object> rawContents) {
        super(context);
        this.arguments = arguments;
        this.name = functionName;
        this.returnType = returnType;
        this.rawContents = rawContents;
        this.header = new FunctionHeader(name, returnType, arguments.stream().map(arg -> arg.getValue()).collect(Collectors.toCollection(ArrayList::new)));
    }
    public FunctionHeader getHeader() {
        return header;
    }
    public void parse(FunctionsContext gc) {
        context.setCurrFunc(this);
        context.gc = gc;
        contents = Processor.parse(rawContents, context);
        context.gc = null;
    }
    @Override
    protected void generateTAC0(IREmitter emit) {
        throw new UnsupportedOperationException("Not supported yet, you poo."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected int calculateTACLength() {
        throw new UnsupportedOperationException("Not supported yet, you poo."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public void staticValues() {
        for (Command com : contents) {
            com.staticValues();
        }
    }
    public void generateX86(StringBuilder resp) {
        Context.VarInfo.printFull = true;
        IREmitter emit = new IREmitter();
        for (Command com : contents) {
            com.generateTAC(emit);
        }
        ArrayList<TACStatement> result = TACOptimizer.optimize(emit);
        System.out.println("TAC FOR " + name);
        for (int i = 0; i < result.size(); i++) {
            System.out.println(i + ":     " + result.get(i));
        }
        System.out.println();
        Context.VarInfo.printFull = false;
        System.out.println("TAC FOR " + name);
        for (int i = 0; i < result.size(); i++) {
            System.out.println(i + ":     " + result.get(i));
        }
        System.out.println();
        Context.VarInfo.printFull = true;
        X86Emitter emitter = new X86Emitter(name);
        for (int i = 0; i < result.size(); i++) {
            emitter.addStatement(emitter.lineToLabel(i) + ":");
            emitter.addStatement("#   " + result.get(i));
            result.get(i).printx86(emitter);
            emitter.addStatement("");//nice blank line makes it more readable =)
        }
        resp.append("	.globl	_").append(name).append("\n	.align	4, 0x90\n");
        resp.append("_").append(name).append(":\n");
        resp.append(FUNC_HEADER).append('\n');
        resp.append(emitter.toX86()).append('\n');
        resp.append(FUNC_FOOTER).append('\n');
    }
    static final String FUNC_HEADER = "	.cfi_startproc\n"
            + "	pushq	%rbp\n"
            + "	.cfi_def_cfa_offset 16\n"
            + "	.cfi_offset %rbp, -16\n"
            + "	movq	%rsp, %rbp\n"
            + "	.cfi_def_cfa_register %rbp";
    static final String FUNC_FOOTER = "	.cfi_endproc";

    public static class FunctionHeader {
        private FunctionHeader(String name, Type returnType, ArrayList<Type> arguments) {
            this.name = name;
            this.returnType = returnType;
            this.arguments = arguments;
        }
        public final String name;
        final Type returnType;
        final ArrayList<Type> arguments;
        public Type getReturnType() {
            return returnType;
        }
        public ArrayList<Type> inputs() {
            return arguments;
        }
    }
    public static FunctionHeader PRINTINT = new FunctionHeader("KEYWORD" + Keyword.PRINT.toString(), new TypeVoid(), new ArrayList<>(Arrays.asList(new Type[]{new TypeInt32()})));
    public static FunctionHeader MALLOC = new FunctionHeader("malloc", new TypePointer<>(new TypeInt32()), new ArrayList<>(Arrays.asList(new Type[]{new TypeInt32()})));
}
