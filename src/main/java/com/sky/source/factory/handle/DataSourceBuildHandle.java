package com.sky.source.factory.handle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;

import com.sky.source.bean.SessionDefined;

/**
 * 数据源配置信息构建执行接口
 * @author 王帆
 * @time 2019年12月23日 上午9:01:01
 */
public interface DataSourceBuildHandle {
	
	public static List<String> LIMIT_PROPERTY_KEY=Arrays.asList("mapperlocaltion","basepackage","lable","datasource","type","slavermethod","name","beanname");
	public static List<String> DATA_SOURCE_PROPERTY_KEY=Arrays.asList("URL","JDBCURL","PASSWORD","PASSWD","USERNAME","USER","DRIVERCALSSNAME");
		
	/**
	 * 
	 * @author 王帆
	 * @time 2019年12月23日 上午9:02:33
	 * @param session
	 * @return
	 */
	public SessionDefined build(SessionDefined session,String lable,Map<String, Object> map,Environment env);
	
	default  boolean isBeanValue(String str) {
		if(str !=null) {
			str=str.trim();
			if( str.endsWith(")")) {
				if(str.startsWith("$(") || str.startsWith("#(")) {
					return true;
				}
			}
		}
		return false;
	}
}
