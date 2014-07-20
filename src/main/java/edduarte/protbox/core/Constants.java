package edduarte.protbox.core;

import edduarte.protbox.ui.Main;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Stores common elements or default values used throughout the application.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public final class Constants {

    public static boolean verbose = false;

    public static final String OS = System.getProperty("os.name").toLowerCase();

    public static final String INSTALL_DIR = new File(
            Constants.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath())
            .getAbsoluteFile()
            .getParentFile()
            .getAbsolutePath()
            .replaceAll("%20", " ");

    private static final Map<String, BufferedImage> ASSETS = new HashMap<String, BufferedImage>();

    public static BufferedImage getAsset(String resourceFileName) {
        BufferedImage result = ASSETS.get(resourceFileName);

        if (result == null) {
            try {
                InputStream stream = Main.class.getResourceAsStream("/" + resourceFileName);
                result = ImageIO.read(stream);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Asset file " + resourceFileName + " not detected or corrupted!\n Please reinstall the application.",
                        "Nonexistent or corrupted asset file",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }

        return result;
    }

    private static final FileDeleteStrategy deleter = FileDeleteStrategy.FORCE;

    public static String generateUniqueDirID() {
        String newID = "dir"+UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
        if(new File(INSTALL_DIR, newID).exists())
            return generateUniqueDirID();
        else
            return newID;
    }

    public static void delete(File fileToDelete){

        if(fileToDelete==null)
            return;

        else if(!fileToDelete.exists())
            return;

        try{
            deleter.delete(fileToDelete);
        }catch (IOException ex){
            fileToDelete.deleteOnExit();
        }
    }


    /**
     * Moves all contents from the first specified directory to the second specified
     * directory, overriding if it already exists!
     */
    public static void moveContentsFromDirToDir(File fromDir, File toDir) throws IOException {
        try{
            File[] list = fromDir.listFiles();
            for(File f : list){
                File destination = new File(toDir, f.getName());
                if(f.isDirectory()){
                    destination.mkdir();
                    moveContentsFromDirToDir(f, destination);
                } else {
                    FileUtils.writeByteArrayToFile(destination, FileUtils.readFileToByteArray(f));
                }
                Constants.delete(f);
            }
        }catch (NullPointerException ex) {
            throw new IOException("Specified directory is not a folder.", ex);
        }
    }

    /**
     * Get the current date and time
     * @return the current date and time
     */
    public static Date getToday() {
        Calendar c = new GregorianCalendar();
        return c.getTime();
    }
}
