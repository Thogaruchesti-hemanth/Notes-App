package com.example.NotesNest.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void zipSingleFile(File inputFile, File outputZip, String entryName) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
             FileInputStream fis = new FileInputStream(inputFile);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            ZipEntry entry = new ZipEntry(entryName);
            entry.setSize(inputFile.length());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    public static void unzipSingleFile(File zipFile, File outputFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

            ZipEntry entry = zis.getNextEntry();
            if (entry == null) throw new IllegalStateException("Zip is empty");

            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                bos.flush();
            }
            zis.closeEntry();
        }
    }

}
