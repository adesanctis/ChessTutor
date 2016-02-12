/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nullprogram.chess.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 *
 * @author Administrator
 */
public class UciPlayerSelector extends JPanel {
    
    /** Version for object serialization. */
    private static final long serialVersionUID = 1L;

    /** JList labels for the user. */
    private static final String[] LABELS_AI = {
        "Level 08",
        "Level 09",
        "Level 10",
        "Level 11",
        "Level 12",
        "Level 13",
        "Level 14",
        "Level 15",
        "Level 16",
        "Level 17",
        "Level 18",
        "Level 19",
        "Level 20",
    };

    /** Configuration names corresponding to LABELS_AI. */
    private static final String[] NAMES_AI = LABELS_AI;

    /** The default AI selection in the JList. */
    private static final int DEFAULT_AI = 5;

    /** Selection for a human player. */
    private final JRadioButton human = new JRadioButton("Human");;

    /** Selection for a computer player. */
    private final JRadioButton computer = new JRadioButton("Computer");

    /** AI selector. */
    private final JList ai = new JList(LABELS_AI);

    /** Vertical padding around this panel. */
    static final int V_PADDING = 15;

    /** Horizontal padding around this panel. */
    static final int H_PADDING = 10;

    /**
     * Creates a player selector panel.
     *
     * @param title title for this selector
     * @param humanSet select the human player by default
     */
    public UciPlayerSelector(final String title, final boolean humanSet) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(title);
        add(label);

        /* Set up widgets. */
        ButtonGroup group = new ButtonGroup();
        group.add(human);
        group.add(computer);
        human.setSelected(humanSet);
        computer.setSelected(!humanSet);
        ai.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ai.setSelectedIndex(DEFAULT_AI);
        ai.setEnabled(!humanSet);

        /* Set up widget alignment. */
        human.setAlignmentX(Component.LEFT_ALIGNMENT);
        computer.setAlignmentX(Component.LEFT_ALIGNMENT);
        ai.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Set up list enable/disable. */
        human.addActionListener(new ActionListener() {
            public final void actionPerformed(final ActionEvent e) {
                ai.setEnabled(!human.isSelected());
            }
        });
        computer.addActionListener(new ActionListener() {
            public final void actionPerformed(final ActionEvent e) {
                ai.setEnabled(computer.isSelected());
            }
        });

        add(human);
        add(computer);
        add(ai);
        setBorder(BorderFactory.createEmptyBorder(H_PADDING, V_PADDING,
                  H_PADDING, V_PADDING));
    }

    /**
     * Get the player selected by this dialog.
     *
     * @return the player type
     */
    public final String getPlayer() {
        if (human.isSelected()) {
            return "human";
        } else {
            int i = ai.getSelectedIndex();
            if (i < 0) {
                return "default";
            } else {
                return NAMES_AI[i];
            }
        }
    }
}
