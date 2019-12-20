package com.holy.autil.net;

import com.holy.autil.log.Logger;
import com.holy.jutil.io.FileUtil;
import com.holy.jutil.security.Crypter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpDownload {
	private static final String TAG = "[OkHttpDownload]";
	private static final Logger mLogger = new Logger(TAG);
	private OkHttpDownload() { }

	private static int retryMax = 10;
	public static void setRetryMax(int retryMax) {
		OkHttpDownload.retryMax = retryMax;
	}
	private static OkHttpClient client = new OkHttpClient.Builder()
			.retryOnConnectionFailure(true)
			.connectTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build();

	/**
	 * 多线程下载api
	 * @param url
	 * @param out
	 * @param listener
	 */
	public static void down(String url, File out, Listener listener) {
		// 开始任务调度
		Dispatcher.getInstance().start(url, out, listener);
	}

	/**
	 * 下载监听
	 */
	public interface Listener {
		void onSuccess(File file);
		void onFailed(Throwable t);
		void onProgress(long progress);
	}

	/**
	 * 调度器
	 */
	private static class Dispatcher {
		private static final String TAG = "[Dispatcher]";
		private static final Dispatcher instance = new Dispatcher();
		private Dispatcher() {}
		public static Dispatcher getInstance() {
			return instance;
		}

		private final Set<String> downloading = new ConcurrentSkipListSet<>();
		/**
		 * 当前状态
		 */
		private final Deque<Task> ready = new ArrayDeque<>();
		private final Deque<Task> running = new ArrayDeque<>();
		private final Deque<Task> stop = new ArrayDeque<>();

		/**
		 * 开始下载
		 * @param url
		 * @param out
		 * @param listener
		 */
		public void start(String url, File out, Listener listener) {
			if (downloading.contains(Crypter.md5(url))) {
				listener.onFailed(new Exception("Repeat download, url is downloading..."));
				return;
			}

			Request req = new Request.Builder().url(url).build();
			client.newCall(req).enqueue(new Callback() {
				private int retryCount = 0;

				@Override
				public void onFailure(@NotNull Call call, @NotNull IOException e) {
					// 超时重试三次
					if (retryCount < retryMax) {
						retryCount++;
						try {
							Thread.sleep(1000 * (retryCount * 2)); // 2s, 4s, 6s
						} catch (InterruptedException ex) {
						}
						client.newCall(call.request()).enqueue(this);

						mLogger.warn(TAG + "connect error. retryCount: " + retryCount);
					} else {
						listener.onFailed(e);
					}
				}

				@Override
				public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
					long total = response.body().contentLength();
					if (total <= -1) {
						return;
					}
					Task task = new Task(url, out, total, listener);
					task.start();

					running.add(task);
					downloading.add(Crypter.md5(url));
				}
			});
		}

		/**
		 * 回收任务
		 * @param task
		 */
		public void recycle(Task task) {
			running.remove(task);
			downloading.remove(Crypter.md5(task.mUrl));
		}

	}

	/**
	 * 下载任务
	 */
	private static class Task {
		private static final String TAG = "[Task]";
		private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
		private static final int THREAD_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));

		private List<Unit> mUnits;
		private @Nullable
		ExecutorService mService;

		private String mUrl;
		private File mOutFile;
		private long mTotal;
		private long mProgress;
		private Listener mListener;
		private volatile int mFinishNum = 0;
		public Task(String url, File out, long total, Listener listener) {
			mUrl = url;
			mOutFile = out;
			mTotal = total;
			mListener = listener;
			mUnits = new ArrayList<>();
		}

		public ExecutorService getService() {
			if (null == mService) {
				synchronized (this) {
					if (null == mService) {
						mService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60,
								TimeUnit.SECONDS, new SynchronousQueue<>(), runnable -> {
									Thread thread = new Thread(runnable, OkHttpDownload.TAG);
									thread.setDaemon(false); // 无守护线程
									return thread;
								});
					}
				}
			}
			return mService;
		}

		public void start() {
			mLogger.debug("cpu: " + CPU_COUNT + ", thread: " + THREAD_SIZE);

			for (int i = 0; i < THREAD_SIZE; i++) {
				long threadTotal = mTotal / THREAD_SIZE;
				long start = i * threadTotal;
				long end = (start + threadTotal) - 1;
				if (i == THREAD_SIZE - 1) {
					end = mTotal - 1;
				}
				Unit unit = new Unit(mUrl, getTmpFile(), i, start, end, new Listener() {
					@Override
					public void onSuccess(File file) {
						synchronized (Task.this) {
							mFinishNum++;
							if (mFinishNum == THREAD_SIZE) {
								try {
									FileUtil.copyFile(file, mOutFile);
									mListener.onSuccess(mOutFile);
									clean();
								} catch (IOException e) {
									mLogger.format(Logger.LEVEL.WARN, TAG + "%s copy to %s fail!", file.getName(), mOutFile.getName());
									mListener.onSuccess(file);
								}
								Dispatcher.getInstance().recycle(Task.this);
							}
						}
					}

					@Override
					public void onFailed(Throwable t) {
						synchronized (Task.this) {
							stop(); // 终止下载
							mListener.onFailed(t);
							clean();
							Dispatcher.getInstance().recycle(Task.this);
						}
					}

					@Override
					public void onProgress(long progress) {
						synchronized (Task.this) {
							mProgress += progress;
							mListener.onProgress((long) ((mProgress * 1.0) / mTotal * 100));
						}
					}
				});
				getService().execute(unit); // 通过线程池执行

				mUnits.add(unit);
			}
		}

		public void stop() {
			for (Unit unit : mUnits) {
				unit.stop();
			}
		}

		private File getTmpFile() {
			File dir = mOutFile.getParentFile();
			File file = new File(dir, Crypter.md5(mUrl));
			if (!dir.exists()) {
				dir.mkdirs();
			}
			return file;
		}

		private void clean() {
			mUnits.clear();
			File tmpFile = getTmpFile();
			if (tmpFile.exists()) {
				tmpFile.delete();
			}
		}
	}

	/**
	 * 下载线程单元
	 */
	private static class Unit implements Runnable {
		private static final String TAG = "[Unit]";
		private static final int STATUS_DOWNLOAD = 1;
		private static final int STATUS_STOP = 2;

		private int id;
		private String mUrl;
		private File mFile;
		private long mStart;
		private long mEnd;
		private Listener mListener;
		private int mStatus = STATUS_DOWNLOAD;
		private long mProgress = 0; // 当前下载进度
		public Unit(String url, File tempFile, int id, long start, long end, Listener listener) {
			this.id = id;
			mUrl = url;
			mFile = tempFile;
			mStart = start;
			mEnd = end;
			mListener = listener;
		}

		@Override
		public void run() {
			if (mStart + mProgress >= mEnd) {
				mListener.onSuccess(mFile);
				return;
			}

			Request req = new Request.Builder()
					.addHeader("RANGE", "bytes=" + (mStart + mProgress) + "-" + mEnd)
					.url(mUrl)
					.build();

			try {
				Response res = client.newCall(req).execute();

				if (!mFile.exists()) {
					mFile.createNewFile();
				}
				try (InputStream is = res.body().byteStream(); RandomAccessFile accessFile = new RandomAccessFile(mFile, "rwd")) {
					accessFile.seek(mStart + mProgress);
					int len;
					byte[] buffer = new byte[1024 * 4];
					while ((len = is.read(buffer)) != -1) {
						if (mStatus == STATUS_STOP) {
							break;
						}
						mProgress += len;
						accessFile.write(buffer, 0, len);
						mListener.onProgress(len);
					}
					mListener.onSuccess(mFile);
				} catch (IOException e) {
					mLogger.error(TAG + "unit id: " + id + " run[" + mStart + "," + mEnd + "] progress: " + mProgress + ", error msg: " + e.getMessage());
					onFailed(e);
				}
			} catch (IOException e) {
				mLogger.error(TAG + "unit id: " + id + " run[" + mStart + "," + mEnd + "] error msg: " + e.getMessage());
				onFailed(e);
			}
		}

		private int retryCount = 0;
		private void onFailed(Throwable t) {
			mLogger.format(Logger.LEVEL.WARN, TAG + "unit id: %d failed, retry: %d", id, retryCount);
			if (retryCount < retryMax) {
				retryCount++;
				try {
					Thread.sleep(1000 * (retryCount + 1));
				} catch (InterruptedException ex) {
				}
				run();
				return;
			}

			mListener.onFailed(t);
		}

		public void stop() {
			mStatus = STATUS_STOP;
		}
	}

}
