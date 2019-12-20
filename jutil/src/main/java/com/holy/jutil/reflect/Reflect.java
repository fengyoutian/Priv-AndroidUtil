package com.holy.jutil.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 封装反射简单实用
 * @author fengyoutian
 * @date 2019/10/21
 */
public class Reflect {
	private Reflect() {}

	/**
	 * 反射创建实例
	 * @param clazz
	 * @param paramTypes
	 * @param params
	 * @return
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public static Object newInstance(Class clazz, Class[] paramTypes, Object... params)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
			InstantiationException {
		Constructor constructor = clazz.getConstructor(paramTypes);
		return constructor.newInstance(params);
	}

	/**
	 * 反射创建实例
	 * @param classLoader
	 * @param clazzName
	 * @param paramTypes
	 * @param params
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public static Object newInstance(
			ClassLoader classLoader, String clazzName, Class[] paramTypes, Object... params
	) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {
		Class clazz = classLoader.loadClass(clazzName);
		return newInstance(clazz, paramTypes, params);
	}

	/**
	 * 反射方法
	 * @param instance
	 * @param clazz
	 * @param methodName
	 * @param paramTypes
	 * @param paramObjs
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public static Object invoke(
			Object instance, Class clazz, String methodName, Class[] paramTypes, Object... paramObjs
	) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		// 静态方法
		if (null == instance) {
			instance = clazz;
		}
		Method method = clazz.getDeclaredMethod(methodName, paramTypes);
		method.setAccessible(true);
		return method.invoke(instance, paramObjs);
	}

	/**
	 * 反射方法
	 * @param obj
	 * @param classLoader
	 * @param clazzName
	 * @param methodName
	 * @param paramTypes
	 * @param paramObjs
	 * @return
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	public static Object invoke(
		Object obj, ClassLoader classLoader, String clazzName, String methodName, Class[] paramTypes, Object... paramObjs
	) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		Class clazz = classLoader.loadClass(clazzName);
		return invoke(obj, clazz, methodName, paramTypes, paramObjs);
	}

	/**
	 * 反射成员变量赋值
	 * @param instance
	 * @param clazz
	 * @param fieldName
	 * @param fieldValue
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	public static void setField(Object instance, Class clazz, String fieldName, Object fieldValue)
			throws NoSuchFieldException, IllegalAccessException {
		// 静态成员变量
		if (null == instance) {
			instance = clazz;
		}
		Field field = clazz.getField(fieldName);
		field.setAccessible(true);
		field.set(instance, fieldValue);
	}

	/**
	 * 反射成员变量赋值
	 * @param instance
	 * @param classLoader
	 * @param clazzName
	 * @param fieldName
	 * @param fieldValue
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	public static void setField(
			Object instance, ClassLoader classLoader, String clazzName, String fieldName, Object fieldValue
	) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class clazz = classLoader.loadClass(clazzName);
		setField(instance, clazz, fieldName, fieldValue);
	}

	/**
	 * 反射获取成员变量
	 * @param instance
	 * @param clazz
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	public static Object getField(Object instance, Class clazz, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {
		// 静态成员变量
		if (null == instance) {
			instance = clazz;
		}
		Field field = clazz.getField(fieldName);
		field.setAccessible(true);
		return field.get(instance);
	}

	/**
	 * 反射获取成员变量
	 * @param instance
	 * @param classLoader
	 * @param clazzName
	 * @param fieldName
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	public static Object getField(
			Object instance, ClassLoader classLoader, String clazzName, String fieldName
	) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class clazz = classLoader.loadClass(clazzName);
		return getField(instance, clazz, fieldName);
	}
}
