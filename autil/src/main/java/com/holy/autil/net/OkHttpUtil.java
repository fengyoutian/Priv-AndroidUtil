package com.holy.autil.net;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpUtil {
	private static final String TAG = "[OkHttpUtil]";
	private OkHttpUtil() { }

	private static OkHttpClient client;
	public static OkHttpClient getClient() {
		if (null == client) {
			client = new OkHttpClient.Builder()
					.retryOnConnectionFailure(true)
					.connectTimeout(30, TimeUnit.SECONDS)
					.writeTimeout(30, TimeUnit.SECONDS)
					.readTimeout(30, TimeUnit.SECONDS)
					.build();
		}
		return client;
	}

	public interface DownloadListener {
		void onSuccess(File file);
		void onFailed(int code, String msg);
		void onProgress(int progress);
	}

	/**
	 * 使用 OKHttp 下载文件
	 * <p>
	 *     小文件下载，大文件会中断
	 * </p>
	 * @param url
	 * @param outFile
	 * @param listener
	 * @return
	 */
	public static boolean downloadFile(String url, File outFile, DownloadListener listener) {
		OkHttpClient client = getClient();
		Request request = new Request.Builder().url(url).build();
		FileOutputStream fos = null;
		try {
			Response response = client.newCall(request).execute();
			if (response.code() != 200) {
				listener.onFailed(response.code(), "Request server failed!");
				return false;
			}
			ResponseBody body = response.body();
			if (body == null) {
				listener.onFailed(response.code(), "Body is null!");
				return false;
			}
			long total = body.contentLength();
			long sum = 0;

			InputStream inputStream = body.byteStream();
			fos = new FileOutputStream(outFile);
			byte[] buffer = new byte[1024];
			int count;
			while ((count = inputStream.read(buffer)) >= 0) {
				fos.write(buffer, 0, count);
				sum += count;
				int progress = (int) ((sum * 1.0) / total * 100);
				if (listener != null) {
					listener.onProgress(progress);
				}
			}
			fos.flush();

			listener.onSuccess(outFile);
			return true;
		} catch (IOException e) {
			listener.onFailed(500, e.getMessage());
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return false;
	}

	/**
	 * 断点续传下载
	 * @param url
	 * @param outFile
	 * @param listener
	 * @param retry
	 */
	public static void downloadFile(String url, File outFile, DownloadListener listener, int retry) {
		new Task(url, outFile, listener, retry).start();
	}

	static class Task {
		private static Set<String> downList = Collections.newSetFromMap(new ConcurrentHashMap<>());

		private String url;
		private File out;
		private DownloadListener call;
		private int retry = 5;

		private long total = 0;
		private long downTotal = 0;
		private int retryCount = 0;

		private boolean isStart = false;
		public static final int DOWNLOAD_FAILED = 500;
		public static final int DOWNLOAD_FAILED_RETRY = 501;
		public Task(String url, File out, DownloadListener call, int retrey) {
			this.url = url;
			this.out = out;
			// 接管回调
			this.call = new DownloadListener() {
				@Override
				public void onSuccess(File file) {
					call.onSuccess(file);

					downList.remove(url); // 删除下载缓存
				}

				@Override
				public void onFailed(int code, String msg) {
					Log.i(TAG, "Task code: " + code + ", msg: " + msg + ", threadId: " + Thread.currentThread().getId() + ", retry: " + retryCount);
					if (code == DOWNLOAD_FAILED_RETRY && retryCount < retrey) {
						try {
							// 休眠时间依次递增
							Thread.sleep(1000 * (retryCount + 1));
						} catch (InterruptedException e) {
						}
						retryCount++;

						down();
						return;
					}
					call.onFailed(code, msg);

					if (code != DOWNLOAD_FAILED) {
						downList.remove(url); // 删除下载缓存
					}
				}

				@Override
				public void onProgress(int progress) {
					call.onProgress(progress);
				}
			};

			if (retrey >= 0) {
				this.retry = retrey;
			}
		}

		/**
		 * 开始下载
		 */
		public void start() {
			if (isStart) {
				return;
			}

			if (downList.contains(url)) {
				call.onFailed(DOWNLOAD_FAILED, "该链接下载中...");
				return;
			}

			Long total = getTotal();
			if (null == total) {
				call.onFailed(DOWNLOAD_FAILED, "获取文件大小失败!");
				return;
			}

			if (out.exists()) {
				out.delete(); // 删除老文件
			}

			isStart = true;
			this.total = total;
			downList.add(url);
			down();
		}

		/**
		 * 获取文件总大小
		 * @return
		 */
		private Long getTotal() {
			Request request = new Request.Builder()
					.url(url)
					.build();

			try (Response response = getClient().newCall(request).execute()) {
				if (response != null && response.isSuccessful()) {
					long total = response.body().contentLength();
					return total == 0 ? null : total;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * 根据下载长度下载
		 */
		private void down() {
			Request request = new Request.Builder()
					.addHeader("RANGE", "bytes=" + downTotal + "-" + total)
					.url(url)
					.build();

			Response res = null;
			try {
				res = getClient().newCall(request).execute();
			} catch (IOException e) {
				call.onFailed(DOWNLOAD_FAILED_RETRY, e.getMessage());
				e.printStackTrace();
				return;
			}

			try (InputStream is = res.body().byteStream(); FileOutputStream fos = new FileOutputStream(out, true)) {
				byte[] buffer = new byte[4096];
				int len;
				while ((len = is.read(buffer)) != - 1) {
					fos.write(buffer, 0, len);
					downTotal += len;
					call.onProgress((int) ((downTotal * 1.0) / total * 100));
				}
				fos.flush();
				call.onSuccess(out);
			} catch (IOException e) {
				call.onFailed(DOWNLOAD_FAILED_RETRY, e.getMessage());
				e.printStackTrace();
			}
		}
	}

}
