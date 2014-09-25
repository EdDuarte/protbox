package edduarte.protbox.ui.windows;

import edduarte.protbox.core.Constants;
import edduarte.protbox.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class WaitingForResponseWindow extends JFrame {

    private final javax.swing.Timer displayTimer;

    private WaitingForResponseWindow() {
        super();
        this.setTitle("Waiting for user's response...");
        this.setIconImage(Constants.getAsset("box.png"));
        this.setLayout(null);

        JLabel title = new JLabel("Waiting for user's response...");
        title.setBounds(10, 1, 250, 50);
        title.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
        this.add(title);
        final JLabel timer = new JLabel();
        timer.setBounds(10, 22, 250, 50);
        timer.setFont(new Font(Constants.FONT, Font.PLAIN, 13));
        this.add(timer);

        this.setSize(270, 70);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        this.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);

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

        displayTimer = new javax.swing.Timer(1000, listener);
        displayTimer.setInitialDelay(1);
        displayTimer.start();
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
