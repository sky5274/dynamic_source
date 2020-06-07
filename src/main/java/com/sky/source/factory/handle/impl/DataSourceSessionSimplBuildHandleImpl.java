package com.sky.source.factory.handle.impl;

import java.util.Map;
import org.springframework.core.env.Environment;
import com.sky.source.bean.DataSourceDefiend;
import com.sky.source.bean.SessionDefined;

/**
 * 单一数据构建执行实现
 * @author 王帆
 * @time 2019年12月23日 上午10:55:47
 */
public class DataSourceSessionSimplBuildHandleImpl extends AbstractDataSourceSessionBuildHandleImpl{

	@Override
	public SessionDefined build(SessionDefined session, String lable, Map<String, Object> map, Environment env) {
		DataSourceDefiend dsd=buildDataSource(lable, map, env);
		if(dsd !=null) {
			session.setDataSourceDefiend(dsd);
		}
		return session;
	}
	
}
