package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;

public class ChipFrame extends JFrame {

    private ChipPanel panel;

    public ChipFrame(Chip c) {
        setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(new Dimension(640 + getInsets().left + getInsets().right, 320 + getInsets().top + getInsets().bottom));
        panel = new ChipPanel(c);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Chip 8 Emulator");
        pack();
        setVisible(true);
    }

}
