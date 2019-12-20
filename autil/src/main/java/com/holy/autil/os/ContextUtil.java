package com.holy.autil.os;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Created by fengyoutian on 2015/4/20.
 */
public class ContextUtil {
	public static final String GPRS = "2";
	public static final String WIFI = "1";
	public static final String EXCEPTION = "0";
	public static final String UNKNOWN = "";
	private static final String SIM_TYPE_CMCC = "00";
	private static final String SIM_TYPE_UNICOM = "01";
	private static final String SIM_TYPE_TELECOM = "02";

	private static final String SPLIT_TAG = "|";

	/**
	 * 检查权限
	 * 
	 * @param context
	 * @param permissionName
	 * @return
	 */
	@SuppressLint("WrongConstant")
	public static boolean checkPermission(Context context, String permissionName) {
		return context.getPackageManager().checkPermission(permissionName, context.getPackageName()) == 0;
	}

	private static Object invokeTelephonyManagerMethod(String methodName, Context context) {
		try {
			Object phone = Context.class.getMethod("getSystemService", new Class[] { String.class }).invoke(context,
					new Object[] { "phone" });
			return phone.getClass().getMethod(methodName, (Class[]) null).invoke(phone, (Object[]) null);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private static boolean isZero(String id) {
		for (int i = 0; i < id.length(); i++) {
			if (id.charAt(i) != '0') {
				return false;
			}
		}
		return true;
	}

	public static String getIMEI(Context context) {
		String id = null;
		if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			id = (String) invokeTelephonyManagerMethod("getDeviceId", context);
		}
		return (TextUtils.isEmpty(id) || isZero(id)) ? UNKNOWN : id;
	}

	public static String getIMSI(Context context) {
		String id = null;
		if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			id = (String) invokeTelephonyManagerMethod("getSubscriberId", context);
		}
		return (TextUtils.isEmpty(id) || isZero(id)) ? UNKNOWN : id;
	}

	/**
	 * 获得ICCID
	 * 
	 * @param context
	 * @return
	 */
	public static String getSimSerialNumber(Context context) {
		if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			return (String) invokeTelephonyManagerMethod("getSimSerialNumber", context);
		}
		return UNKNOWN;
	}

	public static String getPhoneNumber(Context context) {
		if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			return (String) invokeTelephonyManagerMethod("getLine1Number", context);
		}
		return UNKNOWN;
	}

	public static String getUUID(Context context) {
		String sgDevice = "";
		String sgSerial = "";
		if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			sgDevice = (String) invokeTelephonyManagerMethod("getDeviceId", context);
			sgSerial = (String) invokeTelephonyManagerMethod("getSimSerialNumber", context);
		}
		return new UUID((long) (Settings.Secure.getString(context.getContentResolver(), "android_id"))
				.hashCode(), (((long) sgDevice.hashCode()) << 32) | ((long) sgSerial.hashCode())).toString();
	}

	public static String getPhoneNetwork(Context context) {
		if (!checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
			return EXCEPTION;
		}
		NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		if (networkInfo == null) {
			return EXCEPTION;
		}
		return networkInfo.getType() == 0 ? GPRS : WIFI;
	}

	public static String getNetworkState(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
			NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getActiveNetworkInfo();
			if (networkInfo != null) {
				return networkInfo.getTypeName();
			}
		}
		return UNKNOWN;
	}
	
	public static boolean getNetworkIsAvailable(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
			NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getActiveNetworkInfo();
			if (networkInfo != null) {
				return networkInfo.isAvailable();
			}
		}
		return false;
	}

	public static String getMobileExtraInfo(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
			NetworkInfo mobileInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (mobileInfo != null) {
				return mobileInfo.getExtraInfo();
			}
		}
		return UNKNOWN;
	}

	public static String getMobileSubType(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
			NetworkInfo mobileInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (mobileInfo != null) {
				return mobileInfo.getSubtypeName();
			}
		}
		return UNKNOWN;
	}

	public static String getSimCardType(Context context) {
		if (!checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			return UNKNOWN;
		}
		if (((Integer) invokeTelephonyManagerMethod("getSimState", context)).intValue() != 5) {
			return UNKNOWN;
		}
		String serialNumber = getSimSerialNumber(context);
		if (null != serialNumber && serialNumber.startsWith("898600")) {
			return SIM_TYPE_CMCC;
		}
		String imsi = (String) invokeTelephonyManagerMethod("getSubscriberId", context);
		if (TextUtils.isEmpty(imsi)) {
			return UNKNOWN;
		}
		if (imsi.contains("46000") || imsi.contains("46002") || imsi.contains("46007") || imsi.contains("46020")) {
			return SIM_TYPE_CMCC;
		}
		if (imsi.contains("46001") || imsi.contains("46006") || imsi.contains("46009")) {
			return SIM_TYPE_UNICOM;
		}
		return (imsi.contains("46003") || imsi.contains("46005") || imsi.contains("46011")) ? SIM_TYPE_TELECOM : UNKNOWN;
	}

	public static String getLocalIpAddress(Context context) {
		boolean flag = false;
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (checkPermission(context, Manifest.permission.ACCESS_WIFI_STATE)) {
			flag = wifiManager.isWifiEnabled();
		}

		if (flag) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			return (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "." + ((ipAddress >> 16) & 0xFF) + "."
					+ (ipAddress >> 24 & 0xFF);
		} else {
			try {
				Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
				if (en == null) {
					return UNKNOWN;
				}
				while (en.hasMoreElements()) {
					Enumeration<InetAddress> enumIpAddr = ((NetworkInterface) en.nextElement()).getInetAddresses();
					if (enumIpAddr != null) {
						while (enumIpAddr.hasMoreElements()) {
							InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
							if (!inetAddress.isLoopbackAddress()) {
								return inetAddress.getHostAddress();
							}
						}
					}
				}
			} catch (SocketException e) {
			}
		}
		return UNKNOWN;
	}

	public static String getBroad() {
		return Build.BRAND;
	}

	public static String getDevice() {
		return Build.DEVICE;
	}

	public static String getModel() {
		return Build.MODEL;
	}

	public static String getRelease() {
		return Build.VERSION.RELEASE;
	}

	/**
	 * 判断手机是否root（弹窗）
	 * 
	 * @return
	 */
	public synchronized static boolean getRootAhth() {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("exit\n");
			os.flush();
			int exitValue = process.waitFor();
			if (exitValue == 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 手机是否Root（不弹窗）
	 * 
	 * @return
	 */
	public static boolean isRootSystem() {
		File file = null;
		final String kSuSearchPaths[] = { "/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/" };
		try {
			for (int i = 0; i < kSuSearchPaths.length; i++) {
				file = new File(kSuSearchPaths[i] + "su");
				if (file != null && file.exists()) {
					return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	public static String getMac(Context context) {
		String mac = getMac4File();
		if (TextUtils.isEmpty(mac)) {
			mac = getMac4API(context);
		}
		return mac;
	}

	public static String getMac4API(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = wifi.getConnectionInfo();
			return info.getMacAddress();
		}
		return UNKNOWN;
	}

	public static String getMac4File() {
		String macSerial = "";
		try {
			Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
			InputStreamReader ir = new InputStreamReader(pp.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);

			String line;
			while ((line = input.readLine()) != null) {
				macSerial += line.trim();
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return macSerial;
	}

	public static String getCellInfo(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			try {
				CellLocation cellLocation = telephonyManager.getCellLocation();
				if (cellLocation instanceof CdmaCellLocation) {
					CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;

					return cdmaCellLocation.getNetworkId() + SPLIT_TAG + cdmaCellLocation.getBaseStationId()
							+ SPLIT_TAG + cdmaCellLocation.getSystemId() + SPLIT_TAG
							+ cdmaCellLocation.getBaseStationLongitude() / 14400 + SPLIT_TAG
							+ cdmaCellLocation.getBaseStationLatitude() / 14400;
				} else if (cellLocation instanceof GsmCellLocation) {
					GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;

					return gsmCellLocation.getLac() + SPLIT_TAG + gsmCellLocation.getCid()
							+ SPLIT_TAG + gsmCellLocation.getPsc();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return UNKNOWN;
	}

	public static String getSimInfo(Context context) {
		if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			return telephonyManager.getSimState() + SPLIT_TAG + telephonyManager.getSimOperator() + SPLIT_TAG +
					telephonyManager.getSimOperatorName() + SPLIT_TAG + telephonyManager.getSimCountryIso();
		}

		return UNKNOWN;
	}

	public static String getWifiInfo(Context context) {
		if (checkPermission(context, Manifest.permission.ACCESS_WIFI_STATE)) {
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();

			return wifiInfo.getSSID() + SPLIT_TAG + wifiInfo.getBSSID() + SPLIT_TAG + getMac(context);
		}

		return UNKNOWN;
	}
	
}
