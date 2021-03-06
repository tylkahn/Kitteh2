/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.lex;
import compiler.parse.Line;
import compiler.parse.Transform;
import java.util.ArrayList;
import java.util.Optional;

/**
 *
 * @author leijurv
 */
public class LexLuthor implements Transform<ArrayList<Object>> {
    @Override
    public void apply(ArrayList<Object> lines) {
        //bad non multithreaded implementation provided for reference
        /*for (Object o : lines) {
         if (o instanceof Line) {
         ((Line) o).lex();
         }
         }*/
        Optional<RuntimeException> e = lines.parallelStream().filter(Line.class::isInstance).map(Line.class::cast).map(line -> {
            try {
                line.lex();
            } catch (Exception ex) {
                return new RuntimeException("Exception while lexing line " + line.num(), ex);
            }
            return null;
        }).filter(ex -> ex != null).findFirst();//get the first non-null exception
        if (e.isPresent()) {
            throw e.get();//and throw it
        }
        //this makes it mimic the behavior of a non parallel lexer
        //to guarantee that the first and only the first line with an error gets an error thrown
    }
}
