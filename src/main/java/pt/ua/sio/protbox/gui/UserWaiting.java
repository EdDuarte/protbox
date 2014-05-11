package pt.ua.sio.protbox.gui;

import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.util.AWTUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class UserWaiting extends JFrame {

    private final javax.swing.Timer displayTimer;

    public static UserWaiting getInstance() {
        return new UserWaiting();
    }
    
    private UserWaiting() {
        super();
        this.setTitle("Waiting for user's response...");
        this.setIconImage(Constants.ASSETS.get("box.png"));
        this.setLayout(null);

        JLabel title = new JLabel("Waiting for user's response...");
        title.setBounds(10, 1, 250, 50);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.add(title);
        final JLabel timer = new JLabel();
        timer.setBounds(10, 22, 250, 50);
        timer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.add(timer);

        this.setSize(270, 70);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        this.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);

        ActionListener listener = new ActionListener() {
            private int i = 0;
            private int min = 2;

            @Override
            public void actionPerformed(ActionEvent event) {
                timer.setText("("+min+" min "+i+" seconds left until timeout)");
                if(i == 0){
                    i = 60;
                    min --;
                }
                i--;
            }
        };

        displayTimer = new javax.swing.Timer(1000, listener);
        displayTimer.setInitialDelay(1);
        displayTimer.start();
    }

    @Override
    public void dispose() {
        displayTimer.stop();
        super.dispose();
    }
}
