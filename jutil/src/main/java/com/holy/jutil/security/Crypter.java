package com.holy.jutil.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by fengyoutian on 2015/5/26.
 */
public class Crypter {

    public static String md5(String str) {
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] b = md.digest();

            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                int i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String md5(File file) {
         try {
             MessageDigest md5 = MessageDigest.getInstance("MD5");
             byte[] bytes = getByte(file);
             if (null != bytes) {
                 md5.update(bytes);
                 return byte2Hex(md5.digest());
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return null;
    }

    /**
     * 把一个文件转化为字节
     *
     * @param file
     * @return byte[]
     * @throws Exception
     */
    public static byte[] getByte(File file) throws Exception {
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
	 * change byte array to Hex string
	 * 
	 * @param byteArray
	 * @return
	 */
	public static String byte2Hex(byte[] byteArray) {
		String result = "";
		for (int offset = 0; offset < byteArray.length; offset++) {
			String toHexString = Integer.toHexString(byteArray[offset] & 0xFF);
			if (toHexString.length() == 1) {
				result += "0" + toHexString;
			} else {
				result += toHexString;
			}
		}
		return result;
	}

	/**
	 * change Hex string to byte array
	 * 
	 * @param str
	 * @return
	 */
	public static byte[] hex2Byte(String str) {
		byte[] resultByte = null;
		if (isValid(str) && str.length() % 2 == 0) {
			byte[] strByte = str.getBytes();
			resultByte = new byte[strByte.length / 2];
			for (int i = 0; i < strByte.length; i += 2) {
				String item = new String(strByte, i, 2);
				resultByte[i / 2] = (byte) Integer.parseInt(item, 16);
			}
		}
		return resultByte;
	}
	
	/**
	 * check whether object is valid
	 * 
	 * @param obj
	 * @return
	 */
	public static boolean isValid(Object obj) {
		if (obj instanceof String) {
			String s = (String) obj;
			if (s != null && !s.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Generate random String, including number and alphabet
	 * 
	 * @param len
	 * @return
	 */
	public static String genRandString(int len) {
		return genRandString(len, false);
	}

	/**
	 * Generate random String
	 * 
	 * @param len
	 * @param numberOnly
	 * @return
	 */
	public static String genRandString(int len, boolean numberOnly) {
		// String include 0-9a-zA-Z
		char[] buffer = new char[len];
		Random random = new Random();
		for (int i = 0; i < len; i++) {
			if (numberOnly) {
				buffer[i] = String.valueOf(random.nextInt(65535) % 10).charAt(0);
			} else {
				buffer[i] = int2SHex(random.nextInt(65535) % 61);
			}
		}
		return new String(buffer);
	}

	/**
	 * Convert number to super Hex (0-9a-zA-Z)
	 * 
	 * @param n
	 * @return
	 */
	public static char int2SHex(int n) {
		if (n < 10) {
			return String.valueOf(n).charAt(0);
		}
		if (n < 36) {
			n = (int) 'a' + n - 10;
			return (char) n;
		}
		if (n < 62) {
			n = (int) 'A' + n - 36;
			return (char) n;
		}
		return '0';
	}
	
	/**
	 * AES解密
	 * 
	 * @param content
	 * @return
	 */
	public static byte[] decryptAES(byte[] content, byte[] key) {
		try {
			Key secretKey = new SecretKeySpec(key, "AES"); // 用Base64解码成byte[]之后加载key
			// 解密
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // 加解密方式/工作模式/填充方式
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return cipher.doFinal(content);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * AES加密
	 * 
	 * @param content 需要编码的内容
	 */
	public static byte[] encodeAES(byte[] content, byte[] key) {
		try {
			Key secretKey = new SecretKeySpec(key, "AES"); // 用Base64解码成byte[]之后加载key
			// 加密
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // 加解密方式/工作模式/填充方式
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return cipher.doFinal(content);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
