package org.uct.cs.simplify.gui.cropper;

import org.uct.cs.simplify.blueprint.BluePrintGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class CroppingDisplay extends JPanel
{
    public static final Color CRITICAL_POINT_COLOUR = Color.magenta;
    public static final int CRITICAL_POINT_SIZE = 2;
    public static final Color HULL_POINT_COLOUR = Color.yellow;
    public static final int HULL_POINT_SIZE = 3;
    public static final Color INTERIOR_COLOUR = new Color(50, 50, 50, 100);
    public static final Color HULL_LINE_COLOUR = Color.black;
    public static final float ZOOM_INC = 1.1f;
    private static final int NEARNESS_DISTANCE = 10;
    private final ArrayList<Point2D> criticalPoints = new ArrayList<>();
    // navigation stuff
    double zoom = 1;
    double currentX = 0;
    double currentY = 0;
    AffineTransform transform = new AffineTransform();
    private ArrayList<Point2D> actualHullPoints = new ArrayList<>();
    private WorkingMode currentMode = WorkingMode.NONE;
    private BufferedImage image;
    private BluePrintGenerator.BlueprintGeneratorResult blueprint;

    public CroppingDisplay()
    {
        this.bindMouse();
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
    public Dimension getMinimumSize()
    {
        return new Dimension(400, 400);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(400, 400);
    }

    public AffineTransform getCurrentTransform()
    {
        AffineTransform o = new AffineTransform();
        o.translate(getWidth() / 2.0f, getHeight() / 2.0f);
        o.scale(zoom, zoom);
        o.translate(currentX, currentY);
        o.translate(-image.getWidth() / 2.0f, -image.getHeight() / 2.0f);
        return o;
    }

    public void updateTransform()
    {
        transform = getCurrentTransform();
    }


    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Dimension currentSize = this.getSize();

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(BluePrintGenerator.DEFAULT_BACKGROUND);
        g2.fillRect(0, 0, currentSize.width, currentSize.height);

        if (this.image != null)
        {
            g2.drawImage(this.image, transform, null);
        }

        if (!this.criticalPoints.isEmpty())
        {
            if (!actualHullPoints.isEmpty())
            {
                // first join with lines
                Point2D last = transform.transform(actualHullPoints.get(actualHullPoints.size() - 1), null);

                int[] xpoints = new int[ actualHullPoints.size() ];
                int[] ypoints = new int[ actualHullPoints.size() ];
                int index = 0;
                g2.setColor(HULL_LINE_COLOUR);
                for (Point2D point : actualHullPoints)
                {
                    Point2D currentPoint = transform.transform(point, null);
                    g2.drawLine((int) last.getX(), (int) last.getY(), (int) currentPoint.getX(), (int) currentPoint.getY());
                    last = currentPoint;
                    xpoints[ index ] = (int) currentPoint.getX();
                    ypoints[ index ] = (int) currentPoint.getY();
                    index++;
                }

                g2.setColor(INTERIOR_COLOUR);
                g2.fillPolygon(xpoints, ypoints, xpoints.length);
            }

            g2.setColor(CRITICAL_POINT_COLOUR);
            for (Point2D point : criticalPoints)
            {
                Point2D tP = transform.transform(point, null);
                drawCross(g2, tP, CRITICAL_POINT_SIZE);
            }

            if (!actualHullPoints.isEmpty())
            {
                g2.setColor(HULL_POINT_COLOUR);
                for (Point2D point : actualHullPoints)
                {
                    Point2D tP = transform.transform(point, null);
                    drawCross(g2, tP, HULL_POINT_SIZE);
                }
            }
        }

        // draw colour border indicating mode
        g2.setColor(this.currentMode.colour);
        g2.drawRect(0, 0, currentSize.width - 1, currentSize.height - 1);
        if (this.currentMode != WorkingMode.NONE)
        {
            g2.fillRect(0, 0, 120, 20);
            g2.setColor(Color.white);
            g2.drawString(this.currentMode.text, 5, 14);
        }
    }

    public void tryAddCriticalPoint(Point2D p) throws NoninvertibleTransformException
    {
        if (!criticalPoints.isEmpty())
        {
            for (Point2D point : criticalPoints)
            {
                if (transform.transform(point, null).distance(p) < NEARNESS_DISTANCE)
                {
                    return;
                }
            }
        }
        addCriticalPoint(transform.inverseTransform(p, null));
    }

    public void addCriticalPoint(Point2D p)
    {
        this.criticalPoints.add(p);
        this.actualHullPoints = ConvexHullCalculator.quickHull(this.criticalPoints);
    }

    public void tryRemoveCriticalPointNear(Point2D p)
    {
        if (!criticalPoints.isEmpty())
        {
            Point2D pointToRemove = null;
            for (Point2D point : criticalPoints)
            {
                if (transform.transform(point, null).distance(p) < NEARNESS_DISTANCE)
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

    private void bindMouse()
    {
        MouseAdapter handler = new MouseAdapter()
        {
            private int panning_lastOffsetX;
            private int panning_lastOffsetY;

            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    if (currentMode == WorkingMode.EDIT_MODE)
                    {
                        Point2D m = new Point2D.Double(e.getX(), e.getY());
                        if (e.getButton() == MouseEvent.BUTTON1)
                        {
                            tryAddCriticalPoint(m);
                        }
                        else if (e.getButton() == MouseEvent.BUTTON3)
                        {
                            tryRemoveCriticalPointNear(m);
                        }
                        CroppingDisplay.this.repaint();
                    }
                }
                catch (NoninvertibleTransformException e1)
                {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isMiddleMouseButton(e))
                {
                    System.out.println("tic1");
                    panning_lastOffsetX = e.getX();
                    panning_lastOffsetY = e.getY();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                if (SwingUtilities.isMiddleMouseButton(e))
                {
                    // new x and y are defined by current mouse location subtracted
                    // by previously processed mouse location
                    int newX = e.getX() - panning_lastOffsetX;
                    int newY = e.getY() - panning_lastOffsetY;

                    // increment last offset to last processed by drag event.
                    panning_lastOffsetX += newX;
                    panning_lastOffsetY += newY;

                    currentX += newX / zoom;
                    currentY += newY / zoom;
                    updateTransform();
                    CroppingDisplay.this.repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
                {
                    double minzoom = getHeight() / (float) image.getHeight();
                    if (getWidth() < getHeight()) minzoom = getWidth() / (float) image.getWidth();

                    double v = (e.getWheelRotation() > 0) ? 1.1 : 1 / 1.1;
                    zoom *= v;
                    zoom = Math.max(minzoom, zoom);
                    zoom = Math.min(5, zoom);
                    updateTransform();
                    CroppingDisplay.this.repaint();
                }
            }
        };

        this.addMouseListener(handler);
        this.addMouseMotionListener(handler);
        this.addMouseWheelListener(handler);
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
        updateTransform();
        this.repaint();
    }

    public Point2D.Double getWorldPointFromBlueprint(int x, int y)
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
