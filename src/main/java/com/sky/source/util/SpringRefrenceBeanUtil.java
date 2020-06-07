package com.sky.source.util;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * spring bean 引用代理工具类
 * @author 王帆
 * @time 2019年12月24日 上午9:39:04
 */
public class SpringRefrenceBeanUtil implements ApplicationContextAware{
	static ApplicationContext applicationContext;
	
	/**
	 * 引用 spring-bean
	 * @author 王帆
	 * @time 2019年12月24日 上午9:43:00
	 * @param beanName
	 * @param classType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T refrenceBean(final String beanName,final Class<?>... classTypes) {
		return (T) Proxy.newProxyInstance(
					SpringRefrenceBeanUtil.class.getClassLoader(), 
					classTypes,
					(proxy,method,args)->{
						if(method==null) {
							return null;
						}
						if("equals".equals(method.getName())) {
							for(Class<?> c:classTypes) {
								if(SpringRefrenceBeanUtil.isInstanceof(args[0].getClass(), c)) {
									return true;
								}
							}
						}
						if(applicationContext !=null) {
							Object obj = applicationContext.getBean(beanName);
							if(obj !=null) {
								Class<?> clazz=obj.getClass();
								Class<?>[] cls=null;
								if(args !=null) {
									cls=new Class<?>[args.length];
									int i=0;
									for(Object r:args) {
										cls[i]=r.getClass();
										i++;
									}
								}
								Method m = clazz.getMethod(method.getName(), cls);
								return m.invoke(obj, args);
							}
						}
						return null;
					}
			);
	}
	
	/**
	 * 获取bean 实例
	 * @author 王帆
	 * @time 2019年12月24日 上午10:13:44
	 * @param name
	 * @return
	 */
	public static Object getBean(String name) {
		System.err.println(applicationContext);
		if(applicationContext !=null) {
			return applicationContext.getBean(name);
		}
		return null;
	}
	
	/**
	 * 是否继承\实现关系
	 * @author 王帆
	 * @time 2019年12月23日 上午9:17:25
	 * @param source
	 * @param target
	 * @return
	 */
	public static boolean isInstanceof(Class<?> source,Class<?> target) {
		return getAllSupperClass(source,null).contains(target);
	}
	
	public static List<Class<?>> getAllSupperClass(Class<?> clazz,List<Class<?>> list) {
		if(list==null) {
			list=new LinkedList<>();
		}
		list.add(clazz);
		list.addAll(Arrays.asList(clazz.getInterfaces()));
		if(!Object.class.getName().equals(clazz.getName())) {
			list=getAllSupperClass(clazz.getSuperclass(), list);
		}
		return list;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringRefrenceBeanUtil.applicationContext = applicationContext;
	}
}
