package edduarte.protbox.core;

import edduarte.protbox.Main;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Stores common elements or default values used throughout the application.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class Constants {


    public static boolean verbose = false;


    public static final char SPECIAL_FILE_FIRST_CHAR = '_';


    public static final String SPECIAL_FILE_ASK_PREFIX = SPECIAL_FILE_FIRST_CHAR + "ask";


    public static final String SPECIAL_FILE_INVALID_PREFIX = SPECIAL_FILE_FIRST_CHAR + "invalid";


    public static final String SPECIAL_FILE_KEY_PREFIX = SPECIAL_FILE_FIRST_CHAR + "";


    public static final String FONT = "Helvetica";


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


    private static final FileDeleteStrategy deleter = FileDeleteStrategy.FORCE;


    private static final Map<String, BufferedImage> ASSETS = new HashMap<>();


    public static BufferedImage getAsset(String resourceFileName) {
        BufferedImage result = ASSETS.get(resourceFileName);

        if (result == null) {
            try {
                InputStream stream = Main.class.getResourceAsStream("assets" + File.separator + resourceFileName);
                result = ImageIO.read(stream);
                ASSETS.put(resourceFileName, result);

            } catch (IOException | IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null,
                        "Asset file " + resourceFileName + " not detected or corrupted!\nPlease reinstall the application.",
                        "Nonexistent or corrupted asset file",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
            }
        }

        return result;
    }


    public static void delete(File fileToDelete) {

        if (fileToDelete == null) {
            return;
        }

        else if (!fileToDelete.exists()) {
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
