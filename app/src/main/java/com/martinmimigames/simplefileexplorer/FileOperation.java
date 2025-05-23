package com.martinmimigames.simplefileexplorer;

import android.content.Context;
import android.os.Build;
import mg.utils.notify.ToastHelper;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;

public class FileOperation {

    public static File[] getAllStorages(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] paths = context.getExternalFilesDirs(null);
            var pathArray = new ArrayList<File>();
            for (File file : paths) {
                if (file == null) continue;
                final String path = file.getAbsolutePath();
                pathArray.add(new File(path.substring(0, path.lastIndexOf("/Android/") + 1)).getAbsoluteFile());
            }
            paths = new File[pathArray.size()];
            for (int i = 0; i < pathArray.size(); i++) {
                paths[i] = pathArray.get(i);
            }
            return paths;
        } else {
            final File primary = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE"))).getAbsoluteFile();
            final String secondary = System.getenv("SECONDARY_STORAGE");
            if (secondary != null)
                return new File[]{primary, new File(secondary).getAbsoluteFile()};
            else
                return new File[]{primary};
        }
    }

    public static boolean move(final File src, final File dst) {
        if (dst.equals(src))
            return true;

        // add (copy) to back of file/folder name to avoid overwrite
        if (dst.exists()) {
            return true;
//            return copy(src, new File(dst.getParent(), getMiddleName(dst) + "(copy)" + getNameExtension(dst)));
        }

        // move logic based on android version, if failed, use copy & delete logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.move(Paths.get(src.getPath()), Paths.get(dst.getPath()));
                return true;
            } catch (IOException ignored) {
            }
        } else {
            if (src.renameTo(dst))
                return true;
        }

        // if move logic didn't work, try copy & delete
        if (copy(src, dst))
            return delete(src);
        return false;
    }

    public static boolean copy(final File src, final File dst) {
        // add (copy) to back of file/folder name to avoid overwrite
        if (dst.exists()) {
            return true;
//            return copy(src, new File(dst.getParent(), getMiddleName(dst) + "(copy)" + getNameExtension(dst)));
        }

        // open & walk directory to copy data
        if (src.isDirectory()) {
            // create required folder to copy to
            if (!dst.mkdir())
                return false;

            // copy each item in directory
            for (File file : Objects.requireNonNull(src.listFiles())) {
                if (!copy(file, new File(dst.getPath(), file.getName())))
                    return false;
            }
            return true;
        }

        // copy data inside file
        if (src.isFile()) {
            try {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    final InputStream in = new FileInputStream(src);
                    final OutputStream out = new FileOutputStream(dst);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }
                return true;
            } catch (IOException ignored) {
            } // fall through and return false;
        }
        return false;
    }

    private static String getMiddleName(final File file) {
        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            return file.getName().substring(0, lastDot);
        }
        return file.getName();
    }

    private static String getNameExtension(final File file) {
        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            return file.getName().substring(lastDot);
        }
        return "";
    }

    public static boolean delete(final File file) {
        if (file.isFile()) {
            return file.delete();
        } else if (file.isDirectory()) {
            for (File subFile : Objects.requireNonNull(file.listFiles())) {
                if (!delete(subFile))
                    return false;
            }
            return file.delete();
        }
        return false;
    }

    public static String getReadableMemorySize(long size) {
        String unit = "B";
        if (size <= 1000L) {
            return size + unit;
        }
        final String[] units = {"KB", "MB", "GB", "TB", "PB"};
        size = size * 100;
        for (int unitJump = 0; size >= 100000 && unitJump < units.length; unitJump++) {
            size = size / 1000L;
            unit = units[unitJump];
        }
        float sizeWithPoint = size / 100f;
        return sizeWithPoint + unit;
    }

    /**
     * Calculates the MD5 of a file.
     * @param context the activity context
     * @param file the file to calculate the md5 for
     * @return a String containing the md5 or null if interrupted/error
     */
    public static String getMD5(Context context, File file) {
        try (var inputStream = new FileInputStream(file)) {
            var digest = MessageDigest.getInstance("MD5");
            var buffer = new byte[1024];
            int readSize;
            while ((readSize = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, readSize);
                if (Thread.currentThread().isInterrupted()) return null;
            }
            return new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            ToastHelper.showShort(context, "No md5 support from device");
        } catch (FileNotFoundException e) {
            // should not happen,
            // file must exist before invoking this method
        } catch (IOException ignored) {
            // should not happen
        }
        return null;
    }
}
