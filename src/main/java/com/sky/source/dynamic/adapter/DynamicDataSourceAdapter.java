package com.sky.source.dynamic.adapter;

import java.lang.reflect.Method;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import com.sky.source.dynamic.DynamicDataSource;
import com.sky.source.dynamic.DynamicDataSourceHolder;

/**
 * 	主从数据库 方法调用适配器
 * @author 王帆
 * 2017年10月10日上午9:06:16
 */
public class DynamicDataSourceAdapter  implements MethodBeforeAdvice{
	private Log log=LogFactory.getLog(DynamicDataSourceAdapter.class);    
	/**从数据库调用规则*/
	private  Set<String> defaultSlaverMethodStart;
	/**主从动态数据源*/
	private DynamicDataSource dataSource;
	/**数据库分配标记*/
	private String lable;
	
	private static String LOG_format="dynamic dataSource (M/S)[%s-%s] mark to-> %s";
	
	public DynamicDataSourceAdapter() {}
	/**
	 * 
	 * @param dataSource
	 * @param lable
	 */
	public DynamicDataSourceAdapter(DynamicDataSource dataSource,String lable) {
		super();
		this.setDataSource(dataSource);
		this.setLable(lable);
	}
	

	/** 
	 * 	通配符匹配 
	 *  
	 * Return if the given method name matches the mapped name. 
	 * <p> 
	 * The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches, as well as direct 
	 * equality. Can be overridden in subclasses. 
	 *  
	 * @param methodName the method name of the class 
	 * @param mappedName the name in the descriptor 
	 * @return if the names match 
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String) 
	 */  
	protected boolean isMatch(String methodName, String mappedName) {  
		return PatternMatchUtils.simpleMatch(mappedName, methodName);  
	}

	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		System.err.println(method.getName());
		if(dataSource !=null && !CollectionUtils.isEmpty(defaultSlaverMethodStart)) {
			// 获取到当前执行的方法名  
			String methodName = method.getName();  
			boolean isSlaver = false;  
			// 使用策略规则匹配  
			for (String mappedName : defaultSlaverMethodStart) { 
				if (isMatch(methodName, mappedName)) {  
					isSlaver = true;  
					break;  
				}  
			}  

			if (isSlaver) {  
				// 标记为读库 (从) 
				DynamicDataSourceHolder.markSlave(dataSource);  
			} else {  
				// 标记为写库 (主) 
				DynamicDataSourceHolder.markMaster(dataSource);  
			} 
			log.debug(String.format(LOG_format, lable,dataSource,isSlaver?"slaver":"master"));
		}
	}

	public DynamicDataSource getDataSource() {
		return dataSource;
	}

	public DynamicDataSourceAdapter setDataSource(DynamicDataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}  

	public String getLable() {
		return lable;
	}
	public DynamicDataSourceAdapter setLable(String lable) {
		this.lable = lable;
		return this;
	}
	public Set<String> getDefaultSlaverMethodStart() {
		return defaultSlaverMethodStart;
	}
	public void setDefaultSlaverMethodStart(Set<String> defaultSlaverMethodStart) {
		this.defaultSlaverMethodStart = defaultSlaverMethodStart;
	}

}
