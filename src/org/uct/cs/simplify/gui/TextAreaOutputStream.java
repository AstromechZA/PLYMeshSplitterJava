package org.uct.cs.simplify.gui;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOutputStream extends OutputStream
{
    private final JTextArea textArea;

    public TextAreaOutputStream(JTextArea textArea)
    {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException
    {
        this.textArea.append(String.valueOf((char) b));
        this.textArea.setCaretPosition(this.textArea.getDocument().getLength());
    }
}
