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

import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Scanner;

/**
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
 * @version 2.0
 */
public final class Utils {

    private static final String WINDOWS_KEY = "SerialNumber";
    private static final String UNIX_KEY = "Serial Number:";
    private static final String SERIAL_ERROR_MESSAGE = "Could not obtain the current machine's serial number.";
    private static final SecureRandom random = new SecureRandom();
    private static final char[] hexArray = "0123456789abcdef".toCharArray();
    private static String sn = null;
    private static MessageDigest md;

    private Utils() {
    }

    public static String getSerialNumber() {
        if (sn != null) {
            return sn;
        }

        if (SystemUtils.IS_OS_WINDOWS) return getSerialNumberWindows();
        if (SystemUtils.IS_OS_LINUX) return getSerialNumberUnix(false);
        if (SystemUtils.IS_OS_MAC_OSX) return getSerialNumberUnix(true);
        return null;
    }

    private static String getSerialNumberWindows() {

        Process process = getWindowsProcess();

        try (OutputStream os = process.getOutputStream();
             InputStream is = process.getInputStream();
             Scanner sc = new Scanner(is)) {

            os.close();
            while (sc.hasNext()) {
                String next = sc.next();
                if (WINDOWS_KEY.equals(next)) {
                    sn = sc.next().trim();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(SERIAL_ERROR_MESSAGE, e);
        }

        if (sn == null) {
            throw new RuntimeException(SERIAL_ERROR_MESSAGE);
        }

        return sn;
    }

    public static String getSerialNumberUnix(boolean isMacOSX) {

        Process process;
        if (isMacOSX) {
            process = getMacOSXProcess();
        } else {
            process = getLinuxProcess();
        }

        try (OutputStream os = process.getOutputStream();
             InputStream is = process.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            os.close();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(UNIX_KEY)) {
                    sn = line.split(UNIX_KEY)[1].trim();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(SERIAL_ERROR_MESSAGE, e);
        }

        if (sn == null) {
            throw new RuntimeException(SERIAL_ERROR_MESSAGE);
        }

        return sn;
    }

    private static Process getWindowsProcess() {
        Runtime runtime = Runtime.getRuntime();
        try {
            return runtime.exec(new String[]{"wmic", "bios", "get", "serialnumber"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Process getLinuxProcess() {
        Runtime runtime = Runtime.getRuntime();
        try {
            return runtime.exec(new String[]{"dmidecode", "-t", "system"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Process getMacOSXProcess() {
        Runtime runtime = Runtime.getRuntime();
        try {
            return runtime.exec(new String[]{"/usr/sbin/system_profiler", "SPHardwareDataType"});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
