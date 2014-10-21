package org.uct.cs.simplify.util;

public class NumberSummary
{
    private final OrderedArrayList<Double> data;

    public double min;
    public double p25;
    public double p50;
    public double p75;
    public double p90;
    public double max;
    public double mean;

    public int count;
    public double total;

    public NumberSummary(int initialCapacity)
    {
        data = new OrderedArrayList<>(initialCapacity);
    }

    public NumberSummary(int initialCapacity, double initialvalue)
    {
        this(initialCapacity);
        add(initialvalue);
    }

    public void add(double v)
    {
        data.put(v);
        count++;

        min = data.get(0);
        max = data.get(count - 1);
        total += v;
        mean = total / count;

        if (count % 2 == 1)
        {
            int middle = (count - 1) / 2;
            p50 = data.get(middle);
        }
        else
        {
            int middle = count / 2;
            p50 = (data.get(middle - 1) + data.get(middle)) / 2;
        }

        p25 = data.get((int) (count * 0.25));
        p75 = data.get((int) (count * 0.75));
        p90 = data.get((int) (count * 0.9));
    }

    public double calculateStdDev()
    {
        double total = 0;
        for (Double d : data)
        {
            total += Math.pow(Math.abs(d - mean), 2);
        }
        return Math.sqrt(total / data.size());
    }

}
