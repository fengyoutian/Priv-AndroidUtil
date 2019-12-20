package com.holy.jutil.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Set;

/**
 * File util
 * 
 * Created by fengyoutian on 2017年1月11日.
 */
public class FileUtil {

    /**
     * 删除目录
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir == null) {
            return false;
        }
        boolean success = true;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String file : children) {
                boolean ret = deleteDir(new File(dir, file));
                if (!ret) {
                    success = false;
                }
            }
            if (success) {
                // if all subdirectory are deleted, delete the dir itself.
                return dir.delete();
            }
        }
        return dir.delete();
    }

    /**
     * 删除目录
     * @param dir
     * @param ignores
     * @return
     */
    public static boolean deleteDir(File dir, Set<File> ignores) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String file : children) {
                boolean success = deleteDir(new File(dir, file), ignores);
                if (!success) {
                    return false;
                }
            }
        }
        return ignores != null && ignores.contains(dir) || dir.delete();
    }
    
    /**
     * 遍历目录及其子目录下的所有文件
     * 
     * @param path 目录全路径
     * @param list 列表：保存文件对象
     */
    public static void list(File path, List<File> list) {
        if (!path.exists()){
            return;
        }
        
        if (path.isFile()){
            list.add(path);
        } else{
            File[] files = path.listFiles();
            for (File file : files){
                list(file, list);
            }
        }
    }

    /**
     * 复制文件
     * @param source
     * @param target
     * @throws IOException
     */
    public static void copyFile(File source, File target) throws IOException {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(target);
            FileChannel iChannel = inputStream.getChannel();
            FileChannel oChannel = outputStream.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                buffer.clear();
                int r = iChannel.read(buffer);
                if (r == -1)
                    break;
                buffer.limit(buffer.position());
                buffer.position(0);
                oChannel.write(buffer);
            }
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
        }
    }

    /**
     * 复制文件
     * @param source
     * @param target
     * @throws IOException
     */
    public static void copyFile(String source, String target) throws IOException {
        File from = new File(source);
        if (!from.exists()) {
            return;
        }
        if (from.isFile()) {
            copyFile(from, new File(target));
        } else {
            copyDir(source, target);
        }
    }

    /**
     * 复制整个目录
     * @param sourcePath
     * @param targetPath
     * @throws IOException
     */
    public static void copyDir(String sourcePath, String targetPath) throws IOException {
        File from = new File(sourcePath);
        if (!from.exists()) {
            return;
        }

        File to = new File(targetPath);
        if (!to.exists()) {
            boolean mkdirs = to.mkdirs();
            if (!mkdirs) {
                return;
            }
        }

        String[] child = from.list();
        for (String file : child) {
            File childSource = new File(sourcePath, file);
            if (childSource.isDirectory()) {
                copyDir(sourcePath + File.separator + file, targetPath + File.separator + file);
            } else {
                copyFile(childSource, new File(targetPath, file));
            }
        }
    }
    
    /**
     * 将is流写到descFile文件中
     * 
     * @param is
     * @param descFile
     * 
     */
    public static void replaceFile(InputStream is, File descFile) {
        OutputStream os = null;
        
        try {
            os = new FileOutputStream(descFile);
            write(is, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            closeQuietly(os);
            closeQuietly(is);
        }
    }
    
    /**
     * 将srcFile文件写到descFile文件中
     * 
     * @param srcFile
     * @param descFile
     * 
     */
    public static void replaceFile(File srcFile, File descFile) {
        OutputStream os = null;
        InputStream is = null;
        
        try {
            os = new FileOutputStream(descFile);
            is = new FileInputStream(srcFile);
            write(is, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            closeQuietly(os);
            closeQuietly(is);
        }
    }
    
    public static String readFile2String(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            return new String(getByte(file));
        } else {
            return null;
        }
    }
    
    public static byte[] readFile2Byte(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            return getByte(file);
        } else {
            return null;
        }
    }
    
    public static void write(InputStream input, OutputStream output) throws IOException {
        byte[] buff = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = input.read(buff)) != -1) {
            output.write(buff, 0, bytesRead);
        }
        input.close();
    }
    
    public static void writeToFile(String content, File file) throws IOException {
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        
        bw.close();
        fw.close();
    }

    /**
     * 将输入流读取到缓存中
     * @param buffer
     * @param is
     * @throws IOException
     */
    public static void read2Buffer(StringBuffer buffer, InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        reader.close();
        is.close();
    }
    
    /**
     * 将文本文件中的内容读入到buffer中
     * 
     * @param buffer        buffer
     * @param file          文件
     * @throws IOException
     */
    public static void read2Buffer(StringBuffer buffer, File file) throws IOException {
        InputStream is = new FileInputStream(file);
        read2Buffer(buffer, is);
    }
    
    /**
     * 把一个文件转化为字节
     *
     * @param file
     * @return byte[]
     * @throws IOException 
     */
    public static byte[] getByte(File file) throws IOException {
        byte[] bytes = null;
        if (null != file && file.exists()) {
            InputStream is = new FileInputStream(file);
            int length = (int) file.length();
            // 当文件的长度超过了int的最大值
            if (length > Integer.MAX_VALUE) {
                is.close();
                return null;
            }
            bytes = new byte[length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            // 如果得到的字节长度和file实际的长度不一致就可能出错了
            if (offset < bytes.length) {
                is.close();
                return null;
            }
            is.close();
        }
        return bytes;
    }

    /**
     * 把输出流转化为字节数组
     * <p>
     *     同getByte()
     * </p>
     * @param inStream
     * @return
     * @throws IOException
     */
    public static byte[] toByteArray(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        return swapStream.toByteArray();
    }

    /**
     * 关闭流
     * @param closeable
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
