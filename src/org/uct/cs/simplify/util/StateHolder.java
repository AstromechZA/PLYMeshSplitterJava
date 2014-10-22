package org.uct.cs.simplify.util;

import java.util.Stack;

public class StateHolder
{
    public static String state = "default";
    private static Stack<String> stateStack = new Stack<>();

    protected static void setState(String s)
    {
        stateStack.push(state);
        state = s;
    }

    protected static void popState()
    {
        if (!stateStack.empty())
        {
            state = stateStack.pop();
        }
    }

    public static class StateWrap implements AutoCloseable
    {
        public StateWrap(String state)
        {
            StateHolder.setState(state);
        }

        @Override
        public void close()
        {
            StateHolder.popState();
        }
    }
}
