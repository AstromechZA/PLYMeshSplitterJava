package org.uct.cs.simplify.gui.cropper;

import javafx.geometry.Point2D;
import org.uct.cs.simplify.blueprint.BluePrintGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class CroppingDisplay extends JPanel
{
    private static final int NEARNESS_DISTANCE = 10;

    private final int size;
    private final ArrayList<Point2D> criticalPoints = new ArrayList<>();
    private ArrayList<Point2D> actualHullPoints = new ArrayList<>();
    private WorkingMode currentMode = WorkingMode.NONE;
    private BufferedImage image;
    private BluePrintGenerator.BlueprintGeneratorResult blueprint;

    public CroppingDisplay(int size)
    {
        this.size = size;
        this.bindClick();
    }

    public ArrayList<Point2D> getHull()
    {
        return this.actualHullPoints;
    }

    public WorkingMode getMode()
    {
        return this.currentMode;
    }

    public void setMode(WorkingMode m)
    {
        this.currentMode = m;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        if (this.image != null)
        {
            g2.drawImage(this.image, 0, 0, null);
        }

        if (!this.criticalPoints.isEmpty())
        {
            g2.setColor(Color.magenta);
            for (Point2D point : criticalPoints)
            {
                drawCross(g2, point, 2);
            }

            if (!actualHullPoints.isEmpty())
            {
                // first join with lines
                Point2D last = actualHullPoints.get(actualHullPoints.size() - 1);
                int[] xpoints = new int[ actualHullPoints.size() ];
                int[] ypoints = new int[ actualHullPoints.size() ];
                int index = 0;
                for (Point2D point : actualHullPoints)
                {
                    g2.drawLine((int) last.getX(), (int) last.getY(), (int) point.getX(), (int) point.getY());
                    last = point;
                    xpoints[ index ] = (int) point.getX();
                    ypoints[ index ] = (int) point.getY();
                    index++;
                }

                g2.setColor(new Color(50, 50, 50, 100));
                g2.fillPolygon(xpoints, ypoints, xpoints.length);

                g2.setColor(Color.yellow);
                for (Point2D point : actualHullPoints) drawCross(g2, point, 2);
            }
        }

        // draw colour border indicating mode
        g2.setColor(this.currentMode.colour);
        g2.drawRect(0, 0, this.size - 1, this.size - 1);
        if (this.currentMode != WorkingMode.NONE)
        {
            g2.fillRect(0, 0, 120, 20);
            g2.setColor(Color.white);
            g2.drawString(this.currentMode.text, 5, 14);
        }
    }

    public void tryAddCriticalPoint(int x, int y)
    {
        if (!criticalPoints.isEmpty())
        {
            Point2D a = new Point2D(x, y);
            for (Point2D point : criticalPoints)
            {
                if (point.distance(a) < NEARNESS_DISTANCE)
                {
                    return;
                }
            }
        }
        addCriticalPoint(x, y);
    }

    public void addCriticalPoint(int x, int y)
    {
        if (x < 0 || y < 0 || x > this.size || y > this.size) throw new RuntimeException("point out of range");
        this.criticalPoints.add(new Point2D(x, y));
        this.actualHullPoints = ConvexHullCalculator.quickHull(this.criticalPoints);
    }

    public void tryRemoveCriticalPointNear(int x, int y)
    {
        if (!criticalPoints.isEmpty())
        {
            Point2D a = new Point2D(x, y);
            Point2D pointToRemove = null;
            for (Point2D point : criticalPoints)
            {
                if (point.distance(a) < NEARNESS_DISTANCE)
                {
                    pointToRemove = point;
                    break;
                }
            }
            if (pointToRemove != null)
            {
                criticalPoints.remove(pointToRemove);
                this.actualHullPoints = ConvexHullCalculator.quickHull(this.criticalPoints);
            }
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(this.size, this.size);
    }

    private void bindClick()
    {
        this.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1)
                {
                    if (currentMode == WorkingMode.EDIT_MODE)
                    {
                        tryAddCriticalPoint(e.getX(), e.getY());
                    }
                }
                else if (e.getButton() == MouseEvent.BUTTON3)
                {
                    if (currentMode == WorkingMode.EDIT_MODE)
                    {
                        tryRemoveCriticalPointNear(e.getX(), e.getY());
                    }
                }

                CroppingDisplay.this.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e)
            {

            }

            @Override
            public void mouseReleased(MouseEvent e)
            {

            }

            @Override
            public void mouseEntered(MouseEvent e)
            {

            }

            @Override
            public void mouseExited(MouseEvent e)
            {

            }
        });
    }

    private void drawCross(Graphics2D g2, Point2D p, int size)
    {
        drawCross(g2, (int) p.getX(), (int) p.getY(), size);
    }

    private void drawCross(Graphics2D g2, int x, int y, int size)
    {
        g2.drawLine(x, y - size, x, y + size);
        g2.drawLine(x - size, y, x + size, y);
    }

    public void setBlueprint(BluePrintGenerator.BlueprintGeneratorResult r)
    {
        this.image = r.output;
        this.blueprint = r;
        this.repaint();
    }

    public Point2D getWorldPointFromBlueprint(int x, int y)
    {
        return this.blueprint.getWorldPointFromImage(x, y);
    }

    public void reset()
    {
        this.image = null;
        this.currentMode = WorkingMode.NONE;
        this.criticalPoints.clear();
        this.actualHullPoints.clear();
    }

    public BluePrintGenerator.BlueprintGeneratorResult getBluePrint()
    {
        return blueprint;
    }

    public enum WorkingMode
    {
        NONE(Color.black, ""),
        EDIT_MODE(Color.blue, "EDIT MODE");

        public final Color colour;
        public final String text;

        WorkingMode(Color c, String s)
        {
            this.colour = c;
            this.text = s;
        }

    }
}
