package org.uct.cs.simplify.gui.util;

import org.uct.cs.simplify.util.Useful;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOutputStream extends OutputStream
{
    private static final String RELATIVE_FONT_PATH = "font/DejaVuSansMono.ttf";
    private final JTextArea textArea;

    public TextAreaOutputStream(JTextArea textArea)
    {
        this.textArea = textArea;
        this.textArea.setBackground(new Color(7, 54, 66));
        this.textArea.setForeground(new Color(238, 232, 213));
        try
        {
            this.textArea.setFont(Font.createFont(Font.TRUETYPE_FONT, new File(Useful.getCodePath() + '/' + RELATIVE_FONT_PATH)).deriveFont(Font.PLAIN, 10));
        }
        catch (IOException | FontFormatException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void write(int b) throws IOException
    {
        this.textArea.append(String.valueOf((char) b));
        this.textArea.setCaretPosition(this.textArea.getDocument().getLength());
    }
}
