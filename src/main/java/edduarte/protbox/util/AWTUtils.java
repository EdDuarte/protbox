package edduarte.protbox.util;

import java.awt.*;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class AWTUtils {

    /**
     * Centers the specified component based on the user main screen dimensions and resolution.
     * @param component the component to be moved to the center of the screen
     */
    public static void setComponentLocationOnCenter(Component component){
        int screenID = getScreenID(component);
        Dimension dim = getScreenDimension(screenID);
        final int x = (dim.width - component.getWidth()) / 2;
        final int y = (dim.height - component.getHeight()) / 2;
        component.setLocation(x, y);
    }

    private static int getScreenID(Component component) {
        int scrID = 1;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        for (int i = 0; i < gd.length; i++) {
            GraphicsConfiguration gc = gd[i].getDefaultConfiguration();
            Rectangle r = gc.getBounds();
            if (r.contains(component.getLocation())) {
                scrID = i+1;
            }
        }
        return scrID;
    }

    private static Dimension getScreenDimension(int scrID) {
        Dimension d = new Dimension(0, 0);
        if (scrID > 0) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            DisplayMode mode = ge.getScreenDevices()[scrID - 1].getDisplayMode();
            d.setSize(mode.getWidth(), mode.getHeight());
        }
        return d;
    }
}
