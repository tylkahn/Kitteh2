/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compiler.tac;

/**
 *
 * @author leijurv
 */
public class TACJump extends TACStatement {
    int jumpTo;
    public TACJump(int jumpTo) {
        this.jumpTo = jumpTo;
    }
    @Override
    protected void onContextKnown() {
    }
    @Override
    public String toString0() {
        return "jmp " + jumpTo;
    }
}
