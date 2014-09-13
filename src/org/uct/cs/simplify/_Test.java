package org.uct.cs.simplify;

public class _Test
{
    public static void main(String[] args)
    {
        System.out.println(zeropad(Integer.toBinaryString(Float.floatToIntBits(123.14f))));
        System.out.println(zeropad(Integer.toBinaryString(Float.floatToIntBits(-123.14f))));
        System.out.println(zeropad(Integer.toBinaryString(Float.floatToIntBits(1f))));
        System.out.println(zeropad(Integer.toBinaryString(Float.floatToIntBits(2f))));
        System.out.println(zeropad(Integer.toBinaryString(Float.floatToIntBits(3f))));
        System.out.println(zeropad(Integer.toBinaryString(Float.floatToIntBits(899.123415f))));
    }

    public static String zeropad(String s)
    {
        return "00000000000000000000000000000000".substring(s.length()) + s;
    }


}
