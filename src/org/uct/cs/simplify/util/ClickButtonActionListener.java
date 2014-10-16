package org.uct.cs.simplify.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ClickButtonActionListener implements ActionListener
{
    public abstract void mouseClicked();

    @Override
    public void actionPerformed(ActionEvent e)
    {
        this.mouseClicked();
    }
}
