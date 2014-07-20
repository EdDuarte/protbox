package edduarte.protbox.ui;

import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.LoggerFactory;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.synchronization.Sync;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */

public class TrayApplet extends JDialog {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(TrayApplet.class);

    TrayIcon trayIcon;
    JPanel instanceList;
    private JLabel statusText;
    private static TrayApplet instance;

    public static TrayApplet getInstance() {
        if(instance==null) {
            instance = new TrayApplet();
        } else {
            instance.toFront();
        }
        return instance;
    }

    public void status(TrayStatus status, String extraInfo) {
        trayIcon.setImage(status.trayIcon);
        statusText.setIcon(new ImageIcon(status.dialogIcon));

        String msg = status.msg.replaceAll("<EXTRA>", extraInfo);
        trayIcon.setToolTip("Protbox 1.031\n" + msg);
        statusText.setText(msg);
    }

    public void baloon(String caption, String msg, TrayIcon.MessageType type){
        trayIcon.displayMessage(caption, msg, type);
    }

    public static enum TrayStatus {
        OKAY(Constants.getAsset("box.png"), Constants.getAsset("done.png"), "Up to date and secure"),
        LOADING(Constants.getAsset("vfl3Wt7C.gif"), Constants.getAsset("sync.png"), "<EXTRA>..."),
        UPDATING(Constants.getAsset("vfl3Wt7C.gif"), Constants.getAsset("sync.png"), "Updating (<EXTRA>)...");
//        ERROR(Constants.getAsset("box.png"), Constants.getAsset("done.png"), "Error!");

        private BufferedImage trayIcon;
        private BufferedImage dialogIcon;
        private String msg;

        TrayStatus(BufferedImage trayIcon, BufferedImage dialogIcon, String message) {
            this.trayIcon = trayIcon;
            this.dialogIcon = dialogIcon;
            this.msg = message;
        }
    }
    

    private TrayApplet() {
        super();
        this.setLayout(null);

        statusText = new JLabel("");

        final Integer clickInterval = (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");


        this.trayIcon = new TrayIcon(TrayStatus.OKAY.trayIcon);
        this.status(TrayStatus.LOADING, "Loading");
        this.trayIcon.setImageAutoSize(true);
        this.trayIcon.addMouseListener(new TrayClickListener(this, clickInterval.intValue()));


        statusText.setLayout(null);
        statusText.setBounds(10, 5, 240, 30);
        statusText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusText.setForeground(Color.DARK_GRAY);
        this.add(statusText);




        JLabel close = new JLabel("Exit Application");
        close.setLayout(null);
        close.setBounds(195, 219, 100, 30);
        close.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        close.setForeground(Color.gray);
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Sync.stop();
                System.exit(1);
            }
        });
        this.add(close);




        instanceList = new JPanel();
        instanceList.setLayout(new VerticalLayout());
        instanceList.setMinimumSize(new Dimension(310, 200));
        instanceList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instanceList.setForeground(Color.black);
        instanceList.setBackground(Color.white);
        instanceList.setOpaque(true);
        JScrollPane scrollList = new JScrollPane(instanceList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollList.setOpaque(true);
        scrollList.setBackground(Color.white);
        scrollList.setBorder(null);
        scrollList.setBounds(0, 41, 310, 174);
        this.add(scrollList);




        JLabel addFolder = new JLabel("Monitor folder...");
        addFolder.setLayout(null);
        addFolder.setBounds(30, 219, 150, 30);
        addFolder.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addFolder.setForeground(Color.gray);
        addFolder.setIcon(new ImageIcon(Constants.getAsset("add.png")));
        addFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                NewDirectory.getInstance(false);
            }
        });
        this.add(addFolder);




        JLabel separator = new JLabel(new ImageIcon(Constants.getAsset("separator.png")));
        separator.setLayout(null);
        separator.setBounds(0, 40, 310, 2);
        this.add(separator);

        JLabel lowerPanelBg = new JLabel(new ImageIcon(Constants.getAsset("lower_side.png")));
        lowerPanelBg.setLayout(null);
        lowerPanelBg.setBounds(0, 215, 310, 41);
        this.add(lowerPanelBg);



        this.addWindowFocusListener(new WindowFocusListener() {
            private boolean gained = false;

            @Override
            public void windowGainedFocus(WindowEvent e) {
                gained = true;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (gained) {
                    dispose();
                }
            }
        });

        this.setVisible(false);
        this.setSize(312, 257);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));

        if(Constants.verbose) logger.info("All swing elements were built");
    }
}

