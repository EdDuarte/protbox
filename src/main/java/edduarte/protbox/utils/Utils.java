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

package edduarte.protbox.utils;

import java.awt.*;
import java.security.SecureRandom;
import java.text.DecimalFormat;

/**
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
 * @version 2.0
 */
public final class Utils {

    private static final SecureRandom random = new SecureRandom();
    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    private Utils() {
    }

    /**
     * Generates and returns a 128-bit random hash in hexadecimal format.
     */
    public static String generateRandomHash() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * The color data the calendar stores is an int. We want it in hex so that
     * we can compare it to the data Google has posted in their API. So we convert
     * it to hex and do some math to get it into the form Google shares.
     *
     * @param color The string the calendar database stores
     * @return The hex string as Google lists in their API
     * http://code.google.com/apis/calendar/data/2.0/reference.html#gCalcolor
     */
    private static String getColorHex(String color) {
        if (color == null) {
            return "";
        }

        try {
            int hex = Integer.parseInt(color);
            hex &= 0xFFFFFFFF;

            return String.format("#%08x", hex);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public static String capitalizeWord(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuilder(strLen)
                .append(Character.toTitleCase(str.charAt(0)))
                .append(str.substring(1))
                .toString();
    }

    /**
     * Centers the specified component based on the user main screen dimensions and resolution.
     *
     * @param component the component to be moved to the center of the screen
     */
    public static void setComponentLocationOnCenter(Component component) {
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
                scrID = i + 1;
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

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0 kb";
        }

        final String[] units = new String[]{"bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
