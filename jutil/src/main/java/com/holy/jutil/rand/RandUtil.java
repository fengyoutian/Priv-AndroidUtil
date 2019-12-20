package com.holy.jutil.rand;

import java.util.Random;

public class RandUtil {
	private RandUtil() {}

	/**
	 * 构造区间
	 */
	public static class BuildInterval {
		private int min;
		private int max;

		public BuildInterval(int max) {
			this.min = 0;
			this.max = max;
		}
		public BuildInterval(int min, int max) {
			this.min = min;
			this.max = max;
		}

		public int getMin() {
			return min;
		}

		public void setMin(int min) {
			this.min = min;
		}

		public int getMax() {
			return max;
		}

		public void setMax(int max) {
			this.max = max;
		}
	}

	/**
	 * 取 [0, max] 内的随机数
	 * @param max
	 * @return
	 */
	public static double rand(int max) {
		return rand(0, max);
	}

	/**
	 * 取 [min, max] 内的随机数
	 * @param min
	 * @param max
	 * @return
	 */
	public static double rand(int min, int max) {
		return Math.random() * (max - min + 1) + min;
	}

	/**
	 * 取 [seed.min, seed.max] 内的随机数
	 * @param seed
	 * @return
	 */
	public static double rand(BuildInterval seed) {
		return rand(seed.min, seed.max);
	}

	/**
	 * 取 [0, max] 内的随机数
	 * @param max
	 * @return
	 */
	public static int rand2Int(int max) {
		return rand2Int(0, max);
	}

	/**
	 * 取 [min, max] 内的随机数
	 * @param min
	 * @param max
	 * @return
	 */
	public static int rand2Int(int min, int max) {
		Random ran = new Random();
		return ran.nextInt(max - min + 1) + min;
	}

	/**
	 * 取 [seed.min, seed.max] 内的随机数
	 * @param seed
	 * @return
	 */
	public static int rand2Int(BuildInterval seed) {
		return rand2Int(seed.min, seed.max);
	}

	/**
	 * 计算概率
	 * 	* 取值超出范围均返回 false
	 * @param pr 取值区间 0 ~ 1
	 * @return 值在 [0, pr] 范围内都返回true
	 */
	public static boolean calcPR(float pr) {
		if (pr < 0 || pr > 1) {
			return false;
		}
		return Math.random() < pr;
	}


}
