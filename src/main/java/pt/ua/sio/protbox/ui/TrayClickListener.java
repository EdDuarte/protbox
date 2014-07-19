package pt.ua.sio.protbox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class TrayClickListener extends MouseAdapter implements ActionListener {
    JDialog frame;
    int delay;
    boolean wasDoubleClick;

    public TrayClickListener(JDialog frame, int delay) {
        this.frame = frame;
        this.delay = delay;
        wasDoubleClick = false;
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
//        if (e.getClickCount() == 2) {
//            // DOUBLE CLICK ACTION
//            try{
//                properties.openProtboxFolder();
//            } catch (IOException ex){
//                System.err.println(ex);
//            }
//            wasDoubleClick = true;
//        } else {
            Timer timer = new Timer(delay, new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if (wasDoubleClick) {
                        wasDoubleClick = false; // reset flag
                    } else {
                        // SINGLE CLICK ACTION
                        Point pos = e.getLocationOnScreen();
                        Rectangle screen = getScreenBoundsAt(pos);

                        if (pos.x + frame.getWidth() > screen.x + screen.width) {
                            pos.x = screen.x + screen.width - frame.getWidth();
                        }
                        if (pos.x < screen.x) {
                            pos.x = screen.x;
                        }

                        if (pos.y + frame.getHeight() > screen.y + screen.height) {
                            pos.y = screen.y + screen.height - frame.getHeight();
                        }
                        if (pos.y < screen.y) {
                            pos.y = screen.y;
                        }

                        frame.setLocation(pos);
                        frame.setVisible(true);
//                        frame.setEsetExtendedState(JFrame.NORMAL);
                    }
                }
            });
            timer.setRepeats(false);
            timer.start();
//        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Do nothing, the action performed events are done within the Timer
    }

    public static Rectangle getScreenBoundsAt(Point pos) {
        GraphicsDevice gd = getGraphicsDeviceAt(pos);
        Rectangle bounds = null;

        if (gd != null) {
            bounds = gd.getDefaultConfiguration().getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());

            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= (insets.left + insets.right);
            bounds.height -= (insets.top + insets.bottom);
        }
        return bounds;
    }

    public static GraphicsDevice getGraphicsDeviceAt(Point pos) {
        GraphicsDevice device = null;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice lstGDs[] = ge.getScreenDevices();

        java.util.List<GraphicsDevice> lstDevices = new ArrayList<GraphicsDevice>(lstGDs.length);

        for (GraphicsDevice gd : lstGDs) {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();

            if (screenBounds.contains(pos)) {
                lstDevices.add(gd);
            }
        }

        if (lstDevices.size() == 1) {
            device = lstDevices.get(0);
        }
        return device;
    }
}