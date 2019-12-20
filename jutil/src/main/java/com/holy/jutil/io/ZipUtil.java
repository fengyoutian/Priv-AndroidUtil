package com.holy.jutil.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip工具
 * 
 * @author fengyoutian
 *
 */
public class ZipUtil {
	/** 
     * 解压缩
     * 
     * @param srcPath 		解压文件路径
     * @param destDir 		解压目录
	 * @throws IOException 
     */  
    public static ArrayList<String> ectract(String srcPath, String destDir) throws IOException {
        ArrayList<String> allFileName = new ArrayList<String>();
        // 先指定压缩档的位置和档名，建立FileInputStream对象
        FileInputStream fins = new FileInputStream(srcPath);
        // 将fins传入ZipInputStream中
        ZipInputStream zins = new ZipInputStream(fins);
        ZipEntry ze = null;
        while ((ze = zins.getNextEntry()) != null) {
        	allFileName.add(ectract(zins, ze, destDir));
        }
        fins.close();
        zins.close();
        
        return allFileName;  
    }
    
    /** 
     * 解压以关键词开头的文件
     * 
     * @param srcPath 		解压文件路径
     * @param destDir 		解压目录
     * @return names		关键词
	 * @throws IOException 
     */  
    public static ArrayList<String> ectractByNames(String srcPath, String destDir, String... names) throws IOException {
        ArrayList<String> allFileName = new ArrayList<String>();
        // 先指定压缩档的位置和档名，建立FileInputStream对象
        FileInputStream fins = new FileInputStream(srcPath);
        // 将fins传入ZipInputStream中
        ZipInputStream zins = new ZipInputStream(fins);
        ZipEntry ze = null;
        boolean isFilter = false;
        while ((ze = zins.getNextEntry()) != null) {
        	for (String keyword : names) {
        		if (ze.getName().startsWith(keyword)) {
        			isFilter = true;
        			break;
            	}
        	}
        	
        	if (isFilter) {
        		allFileName.add(ectract(zins, ze, destDir));
        	}
        	isFilter = false;
        }
        fins.close();
        zins.close();
        
        return allFileName;  
    }
	
    /** 
     * 过滤掉以关键词开头的文件，解压缩
     * 
     * @param srcPath 		解压文件路径
     * @param destDir 		解压目录
     * @return filter		过滤关键词
	 * @throws IOException 
     */  
    public static ArrayList<String> ectractByFilter(String srcPath, String destDir, String... filter) throws IOException {
        ArrayList<String> allFileName = new ArrayList<String>();
        // 先指定压缩档的位置和档名，建立FileInputStream对象
        FileInputStream fins = new FileInputStream(srcPath);
        // 将fins传入ZipInputStream中
        ZipInputStream zins = new ZipInputStream(fins);
        ZipEntry ze = null;
        boolean isFilter = false;
        while ((ze = zins.getNextEntry()) != null) {
        	for (String keyword : filter) {
        		if (ze.getName().startsWith(keyword)) {
        			isFilter = true;
        			break;
            	}
        	}
        	
        	if (!isFilter) {
        		allFileName.add(ectract(zins, ze, destDir));
        	}
        	isFilter = false;
        }
        fins.close();
        zins.close();
        
        return allFileName;  
    }
    
    private static String ectract(ZipInputStream zins, ZipEntry ze, String destDir) throws IOException {
    	File zfile = new File(destDir + File.separator + ze.getName());
        File fpath = new File(zfile.getParentFile().getPath());
        if (ze.isDirectory()) {
            if (!zfile.exists())
                zfile.mkdirs();
            zins.closeEntry();
        } else {
            if (!fpath.exists())
                fpath.mkdirs();
            FileOutputStream fouts = new FileOutputStream(zfile);
            int i;
            byte[] buffer = new byte[256];
            while ((i = zins.read(buffer)) != -1)
                fouts.write(buffer, 0, i);
            zins.closeEntry();
            fouts.close();
        }
        
        return zfile.getAbsolutePath();
    }
    
	/*
	 * @author yanglihang
	 */
	public static void compress(String srcDir, String destDir) throws IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destDir));
		File file = new File(srcDir);
		compress(out, file, "", false);
		out.close();
	}

	/*
	 * @author yanglihang
	 */
	private static void compress(ZipOutputStream out, File file, String base, boolean flag) throws IOException {
		if (file.isDirectory()) {
			File[] fl = file.listFiles();
			out.setMethod(ZipOutputStream.DEFLATED);
			if(flag) {
				out.putNextEntry(new ZipEntry(base + "/"));
			}
			base = base.length() == 0 ? "" : base + "/";
			
			for (int i = 0; i < fl.length; i++) {
				compress(out, fl[i], base + fl[i].getName(),  true);
			}
		} else {
			FileInputStream in = new FileInputStream(file);
			byte[] data = new byte[in.available()];
			in.read(data);
			in.close();
			out.setMethod(ZipOutputStream.DEFLATED);
			out.putNextEntry(new ZipEntry(base));
			out.write(data);
		}
	}
	
	/**
	 * 将zip复制到zipOutStream中
	 * 
	 * @param zipPath
	 * @param zipOutStream
	 * @throws IOException
	 */
	public static void copy(String zipPath, ZipOutputStream zipOutStream) throws IOException {
		ZipFile zipFile = new ZipFile(zipPath);
		
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			copy(zipFile, zipOutStream, zipEntries.nextElement());
			zipOutStream.closeEntry();
		}
		zipFile.close();
	}
	
	/**
	 * 将zip中符合names关键字的文件复制到zipOutStream
	 * 
	 * @param zipPath
	 * @param zipOutStream
	 * @param names
	 * @throws IOException 
	 */
	public static void copyByNames(String zipPath, ZipOutputStream zipOutStream, String... names) throws IOException {
		ZipFile zipFile = new ZipFile(zipPath);
		
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		ZipEntry zipEnty = null;
		boolean isFilter = false;
		while (zipEntries.hasMoreElements()) {
			zipEnty = zipEntries.nextElement();
			for (String keyword : names) {
        		if (zipEnty.getName().startsWith(keyword)) {
        			isFilter = true;
        			break;
            	}
        	}
        	
        	if (isFilter) {
        		copy(zipFile, zipOutStream, zipEnty);
				zipOutStream.closeEntry();
        	}
        	isFilter = false;
		}
		zipFile.close();
	}
	
	/**
	 * 将zip复制到zipOutStream时根据filter条件过滤
	 * 
	 * @param zipPath
	 * @param zipOutStream
	 * @param filter
	 * @throws IOException 
	 */
	public static void copyByFilter(String zipPath, ZipOutputStream zipOutStream, String... filter) throws IOException {
		ZipFile zipFile = new ZipFile(zipPath);
		
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		ZipEntry zipEnty = null;
		boolean isFilter = false;
		while (zipEntries.hasMoreElements()) {
			zipEnty = zipEntries.nextElement();
			for (String keyword : filter) {
        		if (zipEnty.getName().startsWith(keyword)) {
        			isFilter = true;
        			break;
            	}
        	}
        	
        	if (!isFilter) {
        		copy(zipFile, zipOutStream, zipEnty);
        	}
        	
        	zipOutStream.closeEntry();
        	isFilter = false;
		}
		zipFile.close();
	}
	
	public static void copy(ZipFile zipFile, ZipOutputStream zipOutStream, ZipEntry zipEntry) throws IOException {
		long compressSize = zipEntry.getCompressedSize();
		long size = zipEntry.getSize();
		if (compressSize == size) {
			zipOutStream.putNextEntry(zipEntry); // 若是没压缩的，则直接put
		} else {
			zipOutStream.putNextEntry(new ZipEntry(zipEntry.getName())); // 若是压缩过的，则new一个Entry
		}
		FileUtil.write(zipFile.getInputStream(zipEntry), zipOutStream);
	}

}
