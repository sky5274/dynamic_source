package com.sky.source.factory.handle.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.sky.source.bean.DataSourceDefiend;
import com.sky.source.bean.DynamicDataSourceSessionDefined;
import com.sky.source.bean.SessionDefined;
import com.sky.source.dynamic.DynamicDataSource;

/**
 * 主从动态数据源构建执行实现
 * @author 王帆
 * @time 2019年12月23日 上午10:54:42
 */
public class DataSourceSessionDynamicBuildHandleImpl extends AbstractDataSourceSessionBuildHandleImpl{

	@Override
	public SessionDefined build(SessionDefined session, String lable, Map<String, Object> map, Environment env) {
		String slaverMethods=getEnvironmentValue(LIMIT_PROPERTY_KEY.get(5), map, env);
		String datasourceBeanName=getEnvironmentValue(LIMIT_PROPERTY_KEY.get(3), map, env);
		//在集合中 排除 session+datasource 配置属性名
		List<String> keys = new LinkedList<>();
		map.keySet().stream().filter(it->!LIMIT_PROPERTY_KEY.contains(it.toLowerCase())).forEach(it->keys.add(it));
		List<String> otherKeys=new LinkedList<>(keys);
		otherKeys.removeAll(DATA_SOURCE_PROPERTY_KEY);
		if(!CollectionUtils.isEmpty(otherKeys)) {
			Map<String, String> dataRefMap=new HashMap<>();
			List<DataSourceDefiend> datasources=new LinkedList<>();
			//存在其他的配置标签
			for(String k:otherKeys){
				Object kv = map.get(k);
				if(kv instanceof String) {
					String val= env.getProperty(kv.toString());
					//检查是否是bean配置的标签
					if(isBeanValue(val)) {
						dataRefMap.put(k.toLowerCase(), val.substring(2, val.length()-1));
					}
				}else if(kv instanceof Map){
					//多种数据源配置情况
					@SuppressWarnings("unchecked")
					DataSourceDefiend dsd=buildDataSource(k, (Map<String,Object>)kv, env);
					if(dsd !=null) {
						dataRefMap.put(k.toLowerCase(), dsd.getName());
						datasources.add(dsd);
					}
				}
			}
			//符合多数据源混合session 配置情况
			if(!CollectionUtils.isEmpty(dataRefMap)) {
				Map<String, Object> property=new HashMap<>();
				Map<String, Object> propertytmp=new HashMap<>();
//				propertytmp.put("targetDataSources", dataRefMap);
				property.put("dataSourceBeanMap", dataRefMap);
				property.put("label", lable);   //动态数据源标签
//				property.put("sourceMap", propertytmp);
				
				//数据源配置
				DataSourceDefiend dsd=new DataSourceDefiend();
				dsd.setType(DynamicDataSource.class);
				dsd.setName(datasourceBeanName!=null ?datasourceBeanName:(StringUtils.isEmpty(session.getLable())?session.getLable()+"_":"")+dsd.getType().getSimpleName());
				dsd.setPropertyMap(property);
				
				DynamicDataSourceSessionDefined dySession=new DynamicDataSourceSessionDefined(session);
				dySession.setSlaverMethod(slaverMethods==null?null:Arrays.asList(slaverMethods.split(",")));
				dySession.setDataSourceDefiend(dsd);
				dySession.setPrimary(getEnvironmentValue("primary",map,env));
				dySession.setDataSourceList(datasources);
				session=dySession;
			}
		}
		return session;
	}

}
