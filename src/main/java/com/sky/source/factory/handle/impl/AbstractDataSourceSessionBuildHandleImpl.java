package com.sky.source.factory.handle.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.sky.source.bean.DataSourceDefiend;
import com.sky.source.factory.handle.DataSourceBuildHandle;

/**
 * 抽象DataSource-session 构建类，数据源配置构建实现
 * @author 王帆
 * @time 2019年12月23日 上午9:16:56
 */
public abstract class AbstractDataSourceSessionBuildHandleImpl implements DataSourceBuildHandle{
	private static List<String> DATA_PROPERTY_ATTR=Arrays.asList("url","password","username","driverClassName");
	private static List<String> STRING_FROMAT_SPILT=Arrays.asList("-","_");
	private static final String[] DATA_SOURCE_TYPE_NAMES = new String[] {
			"com.zaxxer.hikari.HikariDataSource",
			"org.apache.commons.dbcp.BasicDataSource", 
			"org.apache.commons.dbcp2.BasicDataSource",
			"org.apache.tomcat.jdbc.pool.DataSource"
			};
	
	@SuppressWarnings("unchecked")
	public Class<? extends DataSource> findType(String type) {
		if(type !=null) {
			try {
				return (Class<? extends DataSource>) ClassUtils.forName(type, this.getClass().getClassLoader());
			} catch (Exception e) {
			}
		}
		for (String name : DATA_SOURCE_TYPE_NAMES) {
			try {
				return (Class<? extends DataSource>) ClassUtils.forName(name, this.getClass().getClassLoader());
			}
			catch (Exception ex) {
			}
		}
		throw new RuntimeException("system error for not load default datasouce type");
	}
	
	/**
	 * 将字符串转成驼峰命名
	 * 	<pre>
	 * 		driver-class-name  >> driverClassName
	 * 		driver_class_name  >> driverClassName
	 *  </pre>
	 * @author 王帆
	 * @time 2019年12月23日 下午3:13:02
	 * @param str
	 * @return
	 */
	public static String parseBeanFromat(String str) {
		if(str !=null && (str.indexOf("_")>-1 || str.indexOf("-")>-1)) {
			char[] chars = str.toCharArray();
			boolean isUp=false;
			StringBuilder tmp=new StringBuilder();
			for(int i=0;i<chars.length;i++) {
				String s=String.valueOf(chars[i]);
				if(STRING_FROMAT_SPILT.contains(s)) {
					isUp=true;
				}else {
					tmp.append(isUp?s.toUpperCase():s);
					isUp=false;
				}
			}
			str=tmp.toString();
		}
		return str;
	}
	
	/**
	 * 根据参数与环境变量  构建DataSource 配置数据
	 * @author 王帆
	 * @time 2019年12月23日 上午10:53:08
	 * @param key
	 * @param map
	 * @param env
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DataSourceDefiend buildDataSource(String key,Map<String, Object> map, Environment env) {
		Map<String, Object> property=new HashMap<String, Object>(16);
		DataSourceDefiend dsd=new DataSourceDefiend();
		String beanName = getEnvironmentValue("NAME", map, env);
		String classType = getEnvironmentValue("TYPE", map, env);
		if(classType==null) {
			classType="";
		}
		
		//生成对应的datasource类型  和bean名称
		try {
			dsd.setType(findType(classType.toString()));
		} catch (Exception e) {
			dsd.setType(findType(null));
		}
		if(beanName !=null) {
			dsd.setName(beanName.toString());
		}else {
			dsd.setName((StringUtils.isEmpty(key)? key+"_":"")+dsd.getType().getSimpleName());
		}
		//清空属性中的特殊字段，不参与属性配置
		map.remove("NAME");
		map.remove("TYPE");
		if(map.keySet().contains("DATASOURCE")) {
			Object vk=map.get("DATASOURCE");
			if(vk instanceof String) {
				property.put(parseBeanFromat(vk.toString().substring(vk.toString().lastIndexOf(".")+1)), new RuntimeBeanReference(getEnvironmentValue("DATASOURCE",map,env)));
			}else if(vk instanceof Map){
				//获取子节点定义 数据源
				return buildDataSource(key, (Map<String, Object>)vk, env);
			}
		}else {
			//将属性map的剩余值放入 datasource 的属性配置中
			for(String k:map.keySet()) {
				int index=DATA_SOURCE_PROPERTY_KEY.indexOf(k);
				if(index>-1) {
					index=index/2;
					property.put(DATA_PROPERTY_ATTR.get(index), getEnvironmentValue(k,map,env));
				}else {
					String vk = map.get(k).toString();
					property.put(parseBeanFromat(vk.substring(vk.lastIndexOf(".")+1)), getEnvironmentValue(k,map,env));
				}
			}
			List<String> attr=new LinkedList<>(property.keySet());
			attr.removeAll(DATA_PROPERTY_ATTR);
			//检查 数据库必备的配置字段是否解析出来
			if(attr.size()+DATA_PROPERTY_ATTR.size() !=property.size()) {
				throw new RuntimeException("datasouce defined attr lose the need attr ! with the property: "+property.keySet());
			}
		}
		dsd.setPropertyMap(property);
		return dsd;
	}
	
	/**
	 * 获取环境变量中的属性值
	 * @author 王帆
	 * @time 2019年12月23日 上午9:17:02
	 * @param key
	 * @param map
	 * @param env
	 * @return
	 */
	protected String getEnvironmentValue(String key, Map<String, Object> map, Environment env) {
		Object v = map.get(key.toUpperCase());
		if(v !=null && v instanceof String) {
			return env.getProperty(v.toString());
		}
		return null;
	}
	
}
