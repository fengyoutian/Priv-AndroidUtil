package com.holy.autil.log;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 常用日志封装
 */
public class Logger {
	private String TAG;

	public enum LEVEL {
		DEBUG,
		INFO,
		WARN,
		ERROR,
	}

	/**
	 * 日志输出总开关，默认开
	 */
	private static boolean logSwitch = true;
	public static boolean isLogSwitch() {
		return logSwitch;
	}
	public static void setLogSwitch(boolean logSwitch) {
		Logger.logSwitch = logSwitch;
		launchDelTimer(); // 启动删除定时器
	}

	/**
	 * sdcard 根目录存在 killbill.holy 则打印debug
	 */
	private static LEVEL level = new File(Environment.getExternalStorageDirectory().getPath(), "killbill.holy").exists() ? LEVEL.DEBUG : LEVEL.INFO;
	public static void setLevel(LEVEL level) {
		Logger.level = level;
	}
	public static LEVEL getLevel() {
		return level;
	}

	/**
	 * 日志输出到文件的配置
	 */
	private static boolean log2FileSwitch = false; // 日志写入文件开关
	private static LEVEL level4FileLog = LEVEL.INFO; // 文件日志等级
	private static final String DEFAULT_FILE_LOG_DIR = Environment.getExternalStorageDirectory().getPath() + "/com.holy.log"; // 默认日志文件目录
	private static File fileOutDir = null;
	private static final String LOG_FILE_SUFFIX = ".log";
	private static int fileSaveDay = 7; // 日志文件的最多保存天数
	private static final SimpleDateFormat myLogSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 日志的输出格式
	private static final SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd"); // 日志文件格式
	private static Object logfileLock = new Object();

	/**
	 * 初始化log文件
	 * @param log2FileSwitch 输出日志文件开关
	 * @param level4FileLog 输出日志文件等级
	 * @param fileOutDir 文件输出目录, 默认 /sdcard/com.holy.log
	 * @param fileSaveDay 文件保存天数, 正整数, 默认保存7天
	 */
	public static void init4LogFile(boolean log2FileSwitch, LEVEL level4FileLog, File fileOutDir, Integer fileSaveDay) {
		Logger.log2FileSwitch = log2FileSwitch;
		Logger.level4FileLog = level4FileLog;

		if (null == fileOutDir) {
			fileOutDir = new File(DEFAULT_FILE_LOG_DIR);
		}
		Logger.fileOutDir = fileOutDir;
		if (!fileOutDir.exists()) {
			fileOutDir.mkdirs();
		}

		if (null != fileSaveDay && fileSaveDay > 0) {
			Logger.fileSaveDay = fileSaveDay;
		}

		launchDelTimer(); // 启动删除定时器
	}
	public static boolean isLog2FileSwitch() {
		return log2FileSwitch;
	}
	public static void setLog2FileSwitch(boolean log2FileSwitch) {
		Logger.log2FileSwitch = log2FileSwitch;
		launchDelTimer(); // 启动删除定时器
	}
	public static LEVEL getLevel4FileLog() {
		return level4FileLog;
	}
	public static void setLevel4FileLog(LEVEL level4FileLog) {
		Logger.level4FileLog = level4FileLog;
	}

	public Logger(String logTag) {
		TAG = logTag;
	}

	/**
	 * debug日志
	 * @param msg
	 */
	public void debug(String msg) {
//		Log.d(TAG, "[DEBUG] " + msg);
		print(LEVEL.DEBUG, msg, null);
	}

	/**
	 * 标准输出日志
	 * @param msg
	 */
	public void info(String msg) {
//		Log.i(TAG, "[INFO] " + msg);
		print(LEVEL.INFO, msg, null);
	}

	/**
	 * 警告日志
	 * @param msg
	 */
	public void warn(String msg) {
//		Log.w(TAG, "[WARN] " + msg);
		print(LEVEL.WARN, msg, null);
	}

	/**
	 * 错误日志
	 * @param msg
	 */
	public void error(String msg) {
		error(msg, null);
	}

	/**
	 * 错误日志
	 * @param t
	 */
	public void error(Throwable t) {
		error(null, t);
	}

	/**
	 * 错误日志
	 * @param msg
	 * @param t
	 */
	public void error(String msg, Throwable t) {
		print(LEVEL.ERROR, msg, t);

//		StringWriter stringWriter = new StringWriter();
//		PrintWriter printWriter = new PrintWriter(stringWriter);
//		t.printStackTrace(printWriter);
//
//		StringBuilder sb = new StringBuilder("[ERROR] ");
//		if (!TextUtils.isEmpty(msg)) {
//			sb.append(msg).append(" ");
//		}
//		sb.append(!TextUtils.isEmpty(t.getMessage()) ? t.getMessage() : t)
//				.append("\n\t").append(stringWriter.toString());
//
//		Log.e(TAG, sb.toString());
	}

	/**
	 * 提供 format 输入习惯
	 * @param level
	 * @param format
	 * @param args
	 */
	public void format(LEVEL level, String format, Object... args) {
		String msg = String.format(format, args);
		switch (level) {
			case DEBUG:
				debug(msg);
				break;
			case INFO:
				info(msg);
				break;
			case WARN:
				warn(msg);
				break;
			case ERROR:
				error(msg);
				break;
		}
	}

	/**
	 * 构建日志输出信息
	 * @param level
	 * @param msg
	 * @param t
	 * @return
	 */
	private String buildMsg(LEVEL level, String msg, Throwable t) {
		StringBuilder sb = new StringBuilder("[").append(level.name()).append("] ");
		if (!TextUtils.isEmpty(msg)) {
			sb.append(msg);
		}

		if (null != t) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			t.printStackTrace(printWriter);

			sb.append(" ")
					.append(!TextUtils.isEmpty(t.getMessage()) ? t.getMessage() : t)
					.append("\n\t")
					.append(stringWriter.toString());
		}
		return sb.toString();
	}

	/**
	 * 打印log
	 * @param level
	 * @param msg
	 * @param t
	 */
	private void print(LEVEL level, String msg, Throwable t) {
		if (!logSwitch) {
			return; // 关闭日志
		}

		String outMsg = buildMsg(level, msg, t);
		if (level.compareTo(Logger.level) >= 0) {
			switch (level) {
				case DEBUG:
					Log.d(TAG, outMsg);
					break;
				case INFO:
					Log.i(TAG, outMsg);
					break;
				case WARN:
					Log.w(TAG, outMsg);
					break;
				case ERROR:
					Log.e(TAG, outMsg);
					break;
			}
		}

		// 日志文件开关打开 && 满足日志输出等级
		if (log2FileSwitch && level.compareTo(level4FileLog) >= 0) {
			write2File(outMsg);
		}
	}

	/**
	 * 写入日志文件
	 * @param msg
	 */
	private void write2File(String msg) {
		Date now = new Date();
		String fileName = logfile.format(now);
		String sdfMsg = myLogSdf.format(now) + " " + TAG + msg;

		File file = new File(fileOutDir, fileName + LOG_FILE_SUFFIX);
		if (!file.exists()) {
			// 多线程输出日志时，在那一瞬间可能会出问题，加个锁，拿到锁之后重新判断文件是否创建好
			synchronized (logfileLock) {
				if (!file.exists()) {
					try {
						file.createNewFile();
					} catch (IOException e) {
					}
				}
			}
		}

		try {
			FileWriter fileWriter = new FileWriter(file, true); // 追加方式
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(sdfMsg);
			bufferedWriter.newLine();
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 日志删除定时器
	 */
	private static Timer timer;
	private synchronized static void launchDelTimer() {
		// log关闭 || file输出关闭
		if (!logSwitch || !log2FileSwitch) {
			stopDelTimer();
			return;
		}

		if (null != timer) {
			return;
		}

		timer = new Timer();
		timer.schedule(new DelFileTicker(), 10 * 1000, 24 * 3600 * 1000);
	}
	private static void stopDelTimer() {
		if (null == timer) {
			return;
		}

		timer.cancel();
		timer = null;
	}

	/**
	 * 删除过期log文件任务
	 */
	private static class DelFileTicker extends TimerTask {
		@Override
		public void run() {
			if (!fileOutDir.exists()) {
				return;
			}

			for (File file : fileOutDir.listFiles()) {
				try {
					// 去掉后缀
					String fileName = file.getName().replace(LOG_FILE_SUFFIX, "");
					// 比较日期
					String nowStr = logfile.format(new Date()); // 取相同格式再比较
					Date now = logfile.parse(nowStr);
					Date fileDate = logfile.parse(fileName);
					long dffDay = dffDay(now, fileDate);

//					Log.i("Logger", nowStr + " - " + fileName + " = " + dffDay);
					if (dffDay > fileSaveDay && file.exists()) {
						file.delete();
					}
				} catch (ParseException e) {
				}
			}
		}

		/**
		 * 用毫秒比较天数
		 * @param before
		 * @param after
		 * @return
		 */
		private long dffDay(Date before, Date after) {
			return before.getTime() / 1000 / 3600 / 24 - after.getTime() / 1000 / 3600 / 24;
		}
	}

}
