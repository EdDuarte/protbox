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

package com.edduarte.protbox.ui.windows;

import com.edduarte.protbox.core.Constants;
import com.edduarte.protbox.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class WaitingForResponseWindow extends JFrame {

    private final Timer displayTimer;


    private WaitingForResponseWindow() {
        super();
        this.setTitle("Waiting for response...");
        this.setIconImage(Constants.getAsset("box.png"));
        this.setLayout(null);

        JLabel title = new JLabel("Waiting for response...");
        title.setBounds(10, 1, 250, 50);
        title.setFont(Constants.FONT);
        this.add(title);
        final JLabel timer = new JLabel();
        timer.setBounds(10, 22, 250, 50);
        timer.setFont(Constants.FONT);
        this.add(timer);

        this.setSize(270, 70);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        this.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        ActionListener listener = new ActionListener() {
            private int i = 0;

            private int min = 2;


            @Override
            public void actionPerformed(ActionEvent event) {
                timer.setText("(" + min + " min " + i + " seconds left until timeout)");
                if (i == 0) {
                    i = 60;
                    min--;
                }
                i--;
            }
        };

        displayTimer = new Timer(1000, listener);
        displayTimer.setInitialDelay(1);
        displayTimer.start();

        this.setVisible(true);
    }


    public static WaitingForResponseWindow getInstance() {
        return new WaitingForResponseWindow();
    }


    @Override
    public void dispose() {
        displayTimer.stop();
        super.dispose();
    }
}
