package pt.ua.sio.protbox.core;

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

    public static final Map<String, BufferedImage> ASSETS = getAssets(new File(INSTALL_DIR, "assets").getAbsolutePath());

    private static final FileDeleteStrategy deleter = FileDeleteStrategy.FORCE;

    public static String generateUniqueDirID() {
        String newID = "dir"+UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
        if(new File(INSTALL_DIR, newID).exists())
            return generateUniqueDirID();
        else
            return newID;
    }

    private static Map<String, BufferedImage> getAssets(String assetsFilePath) {
        Map<String, BufferedImage> map = new HashMap<>();

        try(ZipFile in = new ZipFile(assetsFilePath)){

            for(Enumeration<? extends ZipEntry> entries = in.entries(); entries.hasMoreElements();){
                ZipEntry next = entries.nextElement();
                InputStream stream = in.getInputStream(next);

                map.put(next.getName(), ImageIO.read(stream));
            }
        }catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Some program files were corrupted!\n" +
                    "Please reinstall the application or contact your administrator.",
                    "Corrupted files",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        return map;
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
