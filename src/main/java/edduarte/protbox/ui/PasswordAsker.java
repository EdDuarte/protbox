package edduarte.protbox.ui;

import edduarte.protbox.core.Constants;
import edduarte.protbox.util.AWTUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class PasswordAsker extends JFrame {
    private JLabel info, ok, cancel;
    private JPasswordField field;

    String result;

    public static PasswordAsker getInstance() {
        return new PasswordAsker();
    }

    private PasswordAsker() {
        super("Insert the saved directories password - Protbox");
        this.setIconImage(Constants.getAsset("box.png"));
        this.setLayout(null);
//        this.result = new Result();
        this.result = null;

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (JOptionPane.showConfirmDialog(
                        PasswordAsker.this, "You will need to insert a password to be used on saved directories in order to use this application!\n" +
                        "Are you sure you want to cancel and quit the application?\n\n",
                        "Confirm quit application",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    System.exit(1);
                }
            }
        };


        info = new JLabel();
        info.setText("Insert a password for your saved directories:");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setBounds(20, 5, 250, 30);
        add(info);

        field = new JPasswordField(6);
        field.setDocument(new LimitedFieldDocument(6));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBounds(20, 34, 80, 30);
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    okAction();
            }
        });
        add(field);

        ok = new JLabel(new ImageIcon(Constants.getAsset("ok.png")));
        ok.setLayout(null);
        ok.setBounds(100, 30, 142, 39);
        ok.setBackground(Color.black);
        ok.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                okAction();
            }
        });
        add(ok);

        cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(200, 30, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener(ma);
        add(cancel);

        this.setSize(328, 80);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        field.selectAll();
        this.setVisible(true);
    }

    private void okAction(){
        char[] input = field.getPassword();
        if (input.length != 6) {
            JOptionPane.showMessageDialog(PasswordAsker.this,
                    "The password must have exactly 6 characters! Try again!",
                    "Invalid password!",
                    JOptionPane.ERROR_MESSAGE);

        } else {
            result = new String(input);
            dispose();
        }

        Arrays.fill(input, '0');
        field.selectAll();
    }
}
