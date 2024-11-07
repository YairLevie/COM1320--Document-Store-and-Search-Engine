package edu.yu.cs.com1320.project.stage3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.impl.StackImpl;

import static org.junit.jupiter.api.Assertions.*;

public class StackImplTest {
    StackImpl<Integer> stack;
    
    @BeforeEach
    public void setup(){
        stack = new StackImpl<>();
    }

    @Test
    public void testPushAndSize(){
        for (int i = 0; i<10; i++){
            stack.push(i);
        }
        assertEquals(10, stack.size());
    }
    @Test
    public void testPeek(){
        for (int i = 0; i<10; i++){
            stack.push(i);
        }
        assertEquals(9, stack.peek());
    }
    @Test
    public void testPop(){
        for (int i = 0; i<10; i++){
            stack.push(i);
        }
        assertEquals(9, stack.pop());
        assertEquals(8, stack.pop());
        assertEquals(7, stack.pop());
        assertEquals(6, stack.peek());
        assertEquals(7, stack.size());
    }
    @Test
    public void testNullPop(){
       assertNull (stack.pop());
       stack.push(1);
       assertEquals(1, stack.pop());
       assertNull(stack.pop());
    }
}

