package com.sky.source.factory.handle.impl;

import java.util.Arrays;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import com.sky.source.bean.DataSourceDefiend;
import com.sky.source.bean.DynamicDataSourceSessionDefined;
import com.sky.source.bean.SessionDefined;
import com.sky.source.dynamic.DynamicDataSource;
import com.sky.source.util.SpringRefrenceBeanUtil;

/**
 * datasource session default builder
 * @author 王帆
 * @time 2019年12月23日 上午9:04:39
 */
public class DataSourceSessionDefaultBuildHandelImpl extends AbstractDataSourceSessionBuildHandleImpl{

	@SuppressWarnings("unchecked")
	@Override
	public SessionDefined build(SessionDefined session, String lable, Map<String, Object> map, Environment env) {
		String slaverMethods=getEnvironmentValue(LIMIT_PROPERTY_KEY.get(5), map, env);
		String datasourceBeanName=getEnvironmentValue(LIMIT_PROPERTY_KEY.get(3), map, env);
		String datasourceBeanType=getEnvironmentValue(LIMIT_PROPERTY_KEY.get(4), map, env);
		
		//根据一级配置确定  是否是多数据源session
		boolean isDynamicData=!StringUtils.isEmpty(slaverMethods) && !StringUtils.isEmpty(datasourceBeanName);
		Class<? extends DataSource> dataSourceType=null;
		try {
			if(!isDynamicData && !StringUtils.isEmpty(datasourceBeanType)) {
				dataSourceType= (Class<DataSource>) Class.forName(datasourceBeanType);
				//类型是否继承或实现
				isDynamicData=SpringRefrenceBeanUtil.isInstanceof(dataSourceType,DynamicDataSource.class);
			}else {
				dataSourceType=DynamicDataSource.class;
			}
		} catch (Exception e) {
		}
		
		if(isDynamicData) {
			DynamicDataSourceSessionDefined dySession=new DynamicDataSourceSessionDefined(session);
			dySession.setSlaverMethod(slaverMethods==null?null:Arrays.asList(slaverMethods.split(",")));
			DataSourceDefiend dsd=new DataSourceDefiend();
			dsd.setType(dataSourceType==null?DynamicDataSource.class:dataSourceType);
			dsd.setName(datasourceBeanName!=null ?datasourceBeanName:(StringUtils.isEmpty(session.getLable())?session.getLable()+"_":"")+dsd.getType().getSimpleName());
			dySession.setDataSourceDefiend(dsd);
			session=dySession;
		}
		return session;
	}
}
