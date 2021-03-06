/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.parse;
import compiler.Context;
import compiler.command.Command;
import compiler.command.CommandDefineFunction;
import compiler.lex.LexLuthor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author leijurv
 */
public class Processor {
    /**
     * So this does the entire lexing and parsing stuff. It goes from your list
     * of strings (the lines of code, with comments and such removed), to just
     * your AST, in the form of an ArrayList of commands
     *
     * @param tempO
     * @param context
     * @return
     */
    public static ArrayList<Command> parse(List<Object> tempO, Context context) {
        long a = System.currentTimeMillis();
        ArrayList<Object> o = new ArrayList<>(tempO.size());
        o.addAll(tempO);
        tempO.clear();//idk
        //System.out.println("Processing " + o);
        new StringFinder().apply(o);//This makes a lot of things easier. Without this we can't do things like if(line.contains("{")) because the { might be in a string and therefore wouldn't actually begin a block
        new BlockFinder().apply(o);
        long b = System.currentTimeMillis();
        new LexLuthor().apply(o);
        long c = System.currentTimeMillis();
        //System.out.println("Done processing, beginning parsing " + o);
        ArrayList<Command> res = new Parser().parse(o, context);
        long d = System.currentTimeMillis();
        System.out.println("benchmark " + (b - a) + " " + (c - b) + " " + (d - c) + " -- total " + (d - a));
        return res;
    }
    /**
     * Top level parsing, only parses the function headers and returns a list of
     * CommandDefineFunctions. Should be assembled into a FunctionsContext then
     * parsedRekursively
     *
     * @param obj
     * @param context
     * @return
     */
    public static List<CommandDefineFunction> initialParse(List<Line> obj, Context context) {
        return parse(new ArrayList<>(obj), context).stream().map(CommandDefineFunction.class::cast).collect(Collectors.toList());
    }
}
