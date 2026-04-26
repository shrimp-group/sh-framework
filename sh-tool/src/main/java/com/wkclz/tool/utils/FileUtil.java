package com.wkclz.tool.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    private static final long SIZE_KB = 1024L;
    private static final long SIZE_MB = SIZE_KB * 1024;
    private static final long SIZE_GB = SIZE_MB * 1024;

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);


    public static String getTempPath() {
        return getTempPath(null);
    }
    public static String getTempPath(String customPath) {
        File file = getTempPathFile(customPath);
        return file.getAbsolutePath();
    }
    public static File getTempPathFile() {
        return getTempPathFile(null);
    }
    public static File getTempPathFile(String customPath) {
        Object o = System.getProperties().get("user.dir");
        String savePath =  o.toString() + "/tmp/" + (customPath == null ? "": customPath+"/");
        //文件保存位置
        File saveDir = new File(savePath);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        return saveDir;
    }


    public static List<String> getFileList(List<String> filesResult, String strPath) {

        if (filesResult == null) {
            filesResult = new ArrayList<>();
        }

        File dir = new File(strPath);
        if (!dir.exists()) {
            return filesResult;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String absolutePath = files[i].getAbsolutePath();
                if (files[i].isDirectory()) {
                    getFileList(filesResult, absolutePath);
                } else {
                    filesResult.add(absolutePath);
                }
            }
        }
        return filesResult;
    }


    /**
     * 读取文件
     *
     * @param path
     * @return
     */
    public static String readFile(String path) {
        File file = new File(path);
        return readFile(file);
    }

    /**
     * 读取文件
     *
     * @param file
     * @return
     */
    public static String readFile(File file) {
        try {
            if (!file.isFile()) {
                throw new RuntimeException("error file!");
            }
            return Files.readString(file.toPath());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static File writeFile(String filePath, String context) {
        return writeFile(new File(filePath), context);
    }
    public static File writeFile(File file, String context) {
        try {
            if (file.exists()) {
                throw new RuntimeException("文件已存在，无法覆盖： " + file.getAbsolutePath());
            }
            boolean newFile = file.createNewFile();
            if (!newFile) {
                throw new RuntimeException("创建文件失败");
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(context);
                writer.flush();
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean delFile(String path) {
        File file = new File(path);
        return delFile(file);
    }
    public static boolean delFile(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    delFile(f);
                }
            }
        }
        try {
            Files.delete(file.toPath());
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * 转换文件大小
     * @param fileS
     * @return
     */
    public static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString;
        if (fileS < SIZE_KB) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < SIZE_MB) {
            fileSizeString = df.format((double) fileS / SIZE_KB) + "K";
        } else if (fileS < SIZE_GB) {
            fileSizeString = df.format((double) fileS / SIZE_MB) + "M";
        } else {
            fileSizeString = df.format((double) fileS / SIZE_GB) + "G";
        }
        return fileSizeString;
    }


}
