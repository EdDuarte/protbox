/*
 * Copyright 2014 University of Aveiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edduarte.protbox.ui.windows;

import edduarte.protbox.core.Constants;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.listeners.OnKeyReleased;
import edduarte.protbox.utils.listeners.OnMouseClick;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class InsertPasswordWindow extends JFrame {
    private JPasswordField field;

    private Consumer<String> consumer;


    private InsertPasswordWindow(Consumer<String> consumer) {
        super("Insert the saved directories password - Protbox");
        this.setIconImage(Constants.getAsset("box.png"));
        this.setLayout(null);
        this.consumer = consumer;

        JLabel info = new JLabel();
        info.setText("Insert a password for your saved directories:");
        info.setFont(Constants.FONT);
        info.setBounds(20, 5, 250, 30);
        add(info);

        field = new JPasswordField(6);
        field.setDocument(new LimitedFieldDocument(6));
        field.setFont(Constants.FONT.deriveFont(16f));
        field.setBounds(20, 34, 80, 30);
        field.addKeyListener((OnKeyReleased) e -> {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
                okAction();
        });
        add(field);

        JLabel ok = new JLabel(new ImageIcon(Constants.getAsset("ok.png")));
        ok.setLayout(null);
        ok.setBounds(100, 30, 142, 39);
        ok.setBackground(Color.black);
        ok.addMouseListener((OnMouseClick) e -> okAction());
        add(ok);

        JLabel cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(200, 30, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener((OnMouseClick) e -> {
            if (JOptionPane.showConfirmDialog(
                    InsertPasswordWindow.this, "You will need to insert a password to be used on saved " +
                            "directories in order to use this application!\n" +
                            "Are you sure you want to cancel and quit the application?\n\n",
                    "Confirm quit application",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                System.exit(1);
            }
        });
        add(cancel);

        setSize(328, 80);
        setUndecorated(true);
        getContentPane().setBackground(Color.white);
        setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        field.selectAll();
        setVisible(true);
    }


    public static void showPrompt(Consumer<String> consumer) {
        new InsertPasswordWindow(consumer);
    }


    private void okAction() {
        char[] input = field.getPassword();
        if (input.length != 6) {
            JOptionPane.showMessageDialog(InsertPasswordWindow.this,
                    "The password must have exactly 6 characters! Try again!",
                    "Invalid password!",
                    JOptionPane.ERROR_MESSAGE);

        } else {
            consumer.accept(new String(input));
            dispose();
        }

        Arrays.fill(input, '0');
        field.selectAll();
    }


    /**
     * A JTextField document that limits the number of inserted characters to the specified limit.
     */
    private class LimitedFieldDocument extends PlainDocument implements Document {
        private int limit;


        LimitedFieldDocument(int limit) {
            super();
            this.limit = limit;
        }


        @Override
        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if (str == null) return;

            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
        }
    }
}
