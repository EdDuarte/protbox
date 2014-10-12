package edduarte.protbox.core;

import edduarte.protbox.Main;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Stores common elements or default values used throughout the application.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class Constants {


    public static final char SPECIAL_FILE_FIRST_CHAR = '_';
    public static final String SPECIAL_FILE_ASK_PREFIX = SPECIAL_FILE_FIRST_CHAR + "ask";
    public static final String SPECIAL_FILE_INVALID_PREFIX = SPECIAL_FILE_FIRST_CHAR + "invalid";
    public static final String SPECIAL_FILE_KEY_PREFIX = SPECIAL_FILE_FIRST_CHAR + "";
    public static final Font FONT;

    static {
        Font loadedFont = null;
        try {

            InputStream fontStream = Main.class.getResourceAsStream("HelveticaNeue.otf");

            // create the font to use
            loadedFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(12f);

            // register the font
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(loadedFont);

        } catch (IOException | FontFormatException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        FONT = loadedFont;
    }
    public static final String INSTALL_DIR = new File(
            Constants.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath())
            .getAbsoluteFile()
            .getParentFile()
            .getAbsolutePath()
            .replaceAll("%20", " ");
    public static final String PROVIDERS_DIR = new File(INSTALL_DIR, "providers").getAbsolutePath();
    public static final String REGISTRIES_DIR = new File(INSTALL_DIR, "registries").getAbsolutePath();
    /**
     * Returns the standard Date format to be used in this application.
     */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");
    private static final Map<String, BufferedImage> cachedAssets = new HashMap<>();
    private static final FileDeleteStrategy deleter = FileDeleteStrategy.FORCE;
    public static boolean verbose = false;

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static BufferedImage getAsset(String resourceFileName) {
        BufferedImage result = cachedAssets.get(resourceFileName);

        if (result == null) {
            try {
                InputStream stream = Main.class.getResourceAsStream("assets" + File.separator + resourceFileName);
                result = ImageIO.read(stream);
                cachedAssets.put(resourceFileName, result);

            } catch (IOException | IllegalArgumentException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Asset file " + resourceFileName + " not detected or corrupted!\n" +
                                "Please reinstall the application.", "Nonexistent or corrupted asset file",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        return result;
    }

    public static void delete(File fileToDelete) {

        if (fileToDelete == null) {
            return;
        }

        if (!fileToDelete.exists()) {
            return;
        }

        try {
            deleter.delete(fileToDelete);

        } catch (IOException ex) {
            fileToDelete.deleteOnExit();
        }
    }


    /**
     * Moves all contents from the first specified registry to the second specified
     * registry, overriding if it already exists!
     */
    public static void moveContentsFromDirToDir(File fromDir, File toDir) throws IOException {
        try {
            File[] list = fromDir.listFiles();
            if (list == null) {
                return;
            }
            for (File f : list) {
                File destination = new File(toDir, f.getName());
                if (f.isDirectory()) {
                    destination.mkdir();
                    moveContentsFromDirToDir(f, destination);
                } else {
                    FileUtils.writeByteArrayToFile(destination, FileUtils.readFileToByteArray(f));
                }
                Constants.delete(f);
            }
        } catch (NullPointerException ex) {
            throw new IOException("Specified registry is not a folder.", ex);
        }
    }


    /**
     * Get the current date and time
     *
     * @return the current date and time
     */
    public static Date getToday() {
        Calendar c = new GregorianCalendar();
        return c.getTime();
    }
}
