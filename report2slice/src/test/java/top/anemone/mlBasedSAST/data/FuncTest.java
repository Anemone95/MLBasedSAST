package top.anemone.mlBasedSAST.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class FuncTest {

    @Test
    public void hashCodeTest1() {
        Func func1=new Func("AAA","BBB","CCC");
        Func func2=new Func("AAA","BBB","CCC");
        assertEquals(func1.hashCode(), func2.hashCode());
    }

    @Test
    public void hashCodeTest2() {
        Func func1=new Func("AAB","BBB","CCC");
        Func func2=new Func("AAA","BBB","CCC");
        assertNotEquals(func1.hashCode(), func2.hashCode());
    }

    @Test
    public void equalsTest() {
        Func func1=new Func("AAA","BBB","CCC");
        Func func2=new Func("AAA","BBB","CCCC");
        func2.setSig(func2.getSig().substring(1));
        func1.equals(func2);
        assertEquals(func1, func2);
    }
}