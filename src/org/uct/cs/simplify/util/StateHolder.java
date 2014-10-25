package org.uct.cs.simplify.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class StateHolder
{
    private static int currentState = 0;
    private static int lastStateId = 0;
    private static Stack<Integer> stateStack = new Stack<>();
    private static Map<String, Integer> seenStatesSI = new HashMap<>();
    private static Map<Integer, String> seenStatesIS = new HashMap<>();

    static
    {
        currentState = 0;
        seenStatesSI.put("default", 0);
        seenStatesIS.put(0, "default");
    }

    public static int getIdForState(String s)
    {
        s = s.toLowerCase().trim();
        if (seenStatesSI.containsKey(s))
        {
            return seenStatesSI.get(s);
        }
        else
        {
            lastStateId += 1;
            seenStatesSI.put(s, lastStateId);
            seenStatesIS.put(lastStateId, s);
            return lastStateId;
        }
    }

    public static void setState(String s)
    {
        currentState = getIdForState(s);
    }

    protected static void pushState(String s)
    {
        stateStack.push(currentState);
        currentState = getIdForState(s);
    }

    protected static void popState()
    {
        if (!stateStack.empty())
        {
            currentState = stateStack.pop();
        }
    }

    public static int getCurrentStateI()
    {
        return currentState;
    }

    public static String getCurrentStateS()
    {
        return seenStatesIS.get(currentState);
    }

    public static void reset()
    {
        stateStack.clear();
        currentState = 0;
        seenStatesSI.put("default", 0);
        seenStatesIS.put(0, "default");
    }

    public static String getPrintableIDMapping()
    {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> entry : seenStatesIS.entrySet())
        {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }


    public static class StateWrap implements AutoCloseable
    {
        public StateWrap(String state)
        {
            StateHolder.pushState(state);
        }

        @Override
        public void close()
        {
            StateHolder.popState();
        }
    }
}
