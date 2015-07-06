/*
 * Copyright 2014 University of Aveiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edduarte.protbox.core;

import edduarte.protbox.Protbox;
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores common elements or default values used throughout the application.
 *
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public final class Constants {

    public static final char SPECIAL_FILE_FIRST_CHAR = '_';

    public static final Font FONT;

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

    static {
        Font loadedFont = null;
        try {

            InputStream fontStream = Protbox.class.getResourceAsStream("HelveticaNeue.otf");

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


    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }


    public static BufferedImage getAsset(String resourceFileName) {
        BufferedImage result = cachedAssets.get(resourceFileName);

        if (result == null) {
            try {
                InputStream stream = Protbox.class.getResourceAsStream(resourceFileName);
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
