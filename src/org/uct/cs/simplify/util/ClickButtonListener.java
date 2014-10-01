package org.uct.cs.simplify.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public abstract class ClickButtonListener implements MouseListener
{
    public abstract void mouseClicked(MouseEvent e);

    public void mousePressed(MouseEvent e) { }

    public void mouseReleased(MouseEvent e) { }

    public void mouseEntered(MouseEvent e) { }

    public void mouseExited(MouseEvent e) { }
}
