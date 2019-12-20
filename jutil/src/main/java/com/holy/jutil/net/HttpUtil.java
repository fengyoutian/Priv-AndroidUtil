package com.holy.jutil.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Network
 * 
 * Created by fengyoutian on 2017年1月11日.
 */
public class HttpUtil {
	/**
	 * 配置基本参数
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static HttpURLConnection getHttpURLConnection(String path, int timeOut) throws IOException {
		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(timeOut); // 连接超时10000ms
		conn.setReadTimeout(timeOut); // 读取超时10000ms
		conn.setDoOutput(true); // 允许输出
		conn.setUseCaches(false); // 不使用缓存
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Charset", "UTF-8");
		conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		
		return conn;
	}


	/**
	 * 默认超时时间 10s
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static HttpURLConnection getHttpURLConnection(String path) throws IOException {
		return getHttpURLConnection(path, 10000);
	}
	
	/**
	 * 不带参数的链接
	 * 
	 * @param path
	 * @param version
	 * @return
	 */
	public static InputStream connction(String path, String version) {
		try {
			HttpURLConnection conn = getHttpURLConnection(path);
			conn.setRequestProperty("version", version);
			
			conn.connect();
			if (conn.getResponseCode() == 200) {
				return conn.getInputStream();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 带参数的链接
	 * 
	 * @param path
	 * @param data
	 * @param version
	 * @return
	 */
	public static InputStream connection(String path, byte[] data, String version) {
		OutputStream out = null;
		try {
			HttpURLConnection conn = getHttpURLConnection(path);
			conn.setRequestProperty("Content-Length", String.valueOf(data.length));
			conn.setRequestProperty("version", version);
	
			out = conn.getOutputStream();
			out.write(data);
			
			if (conn.getResponseCode() == 200) {
				return conn.getInputStream();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}
}
