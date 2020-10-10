package com.sky.source.dynamic;

import java.lang.reflect.Field;  
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;  
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.sky.source.util.SpringRefrenceBeanUtil;  

/** 
 * 	定义动态数据源，实现通过集成Spring提供的AbstractRoutingDataSource，只需要实现determineCurrentLookupKey方法即可 
 * 	由于DynamicDataSource是单例的，线程不安全的，所以采用ThreadLocal保证线程安全，由DynamicDataSourceHolder完成。 
 *  
 * @author sky
 * 
 */  
public class DynamicDataSource extends AbstractRoutingDataSource {  

	private  Log log = LogFactory.getLog(DynamicDataSource.class);  
	private volatile String masterKey=DynamicDataSourceHolder.MASTER;
	
	private volatile Integer slaverCount;  
	private volatile Integer masterCount;  
	private String label;

	/** 轮询计数,初始为-1,AtomicInteger是线程安全的*/  
	private AtomicInteger slavercounter = new AtomicInteger(-1);  
	private AtomicInteger mastercounter = new AtomicInteger(-1);  

	/** 记录读库的key*/  
	private List<Object> slaverDataSources = new ArrayList<Object>(0);
	private List<Object> masterDataSources = new ArrayList<Object>(0);
	
	private String masterLable;
	
	private Map<String, String> dataSourceBeanMap=new HashMap<String, String>();

	@Override  
	protected synchronized Object determineCurrentLookupKey() {  
		// 使用DynamicDataSourceHolder保证线程安全，使用当前线程的数据源判断  ，数据源类型
		Object m = DynamicDataSourceHolder.isMaster(this)? getMasterKey() :getSlaveKey();
		log.debug("dynamic is get "+m);
		return m;   
	}  

	@SuppressWarnings("unchecked")  
	@Override  
	public void afterPropertiesSet() {
		//初始数据源资源
		initDataSourceBean();
		super.afterPropertiesSet();  
		// 由于父类的resolvedDataSources属性是私有的子类获取不到，需要使用反射获取  
		Field field = ReflectionUtils.findField(AbstractRoutingDataSource.class, "resolvedDataSources"); 
		// 设置可访问  
		field.setAccessible(true); 

		try {  
			Map<Object, DataSource> resolvedDataSources = (Map<Object, DataSource>) field.get(this);  
			// 读库的数据量等于数据源总数减去写库的数量  
			if(CollectionUtils.isEmpty(resolvedDataSources)) {
				throw new Error("dynamic data source is empty");
			}
			StringBuffer sbuf=new StringBuffer();
			for (Map.Entry<Object, DataSource> entry : resolvedDataSources.entrySet()) {  
				sbuf.append(entry.getKey()+",");
				if (masterKey.contains(entry.getKey().toString())) {  
					masterDataSources.add(entry.getKey());
				} else {
					slaverDataSources.add(entry.getKey());  
				}
			}  
			
			if(masterDataSources.isEmpty()) {
				//随机数向下取整
				int index = (int) Math.floor(Math.random()*(slaverDataSources.size()-1));
				masterDataSources.add(slaverDataSources.get(index));
				slaverDataSources.remove(index);
			}
			this.slaverCount = slaverDataSources.size();  
			this.masterCount = masterDataSources.size();  
			log.debug("====> system ["+label+"] has the database: "+sbuf.toString());
			sbuf=null;
		} catch (Exception e) {  
			log.error("afterPropertiesSet error! ", e);  
		}  
	}  

	/** 
	 * 轮询算法实现 
	 *  
	 * @return 
	 */  
	public Object getSlaveKey() {  
		// 得到的下标为：0、1、2、3……  
		Integer index = slavercounter.incrementAndGet() % slaverCount;  
		boolean flag=slavercounter.get() > 9999;
		if (flag) {
			// 以免超出Integer范围  
			// 还原  
			slavercounter.set(-1); 
		}  
		return slaverDataSources.get(index);  
	}
	public Object getMasterKey() {  
		// 得到的下标为：0、1、2、3……  
		Integer index = mastercounter.incrementAndGet() % masterCount;  
		boolean flag=mastercounter.get() > 9999;
		if (flag) {
			// 以免超出Integer范围  
			// 还原  
			mastercounter.set(-1); 
		}  
		return masterDataSources.get(index);  
	}

	public String getMaskerKey() {
		return masterKey;
	}
	public void setMasterKey(String masterKey) {
		this.masterKey = masterKey;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((masterDataSources == null) ? 0 : masterDataSources.hashCode());
		result = prime * result + ((slaverDataSources == null) ? 0 : slaverDataSources.hashCode());
		result = prime * result + ((dataSourceBeanMap == null) ? 0 : dataSourceBeanMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DynamicDataSource other = (DynamicDataSource) obj;
		if (masterDataSources == null) {
			if (other.masterDataSources != null) {
				return false;
			}
		} else if (!masterDataSources.equals(other.masterDataSources)) {
			return false;
		}
		if (slaverDataSources == null) {
			if (other.slaverDataSources != null) {
				return false;
			}
		} else if (!slaverDataSources.equals(other.slaverDataSources)) {
			return false;
		}
		if (dataSourceBeanMap == null) {
			if (other.dataSourceBeanMap != null) {
				return false;
			}
		} else if (!dataSourceBeanMap.equals(other.dataSourceBeanMap)) {
			return false;
		}
		return true;
	}

	public Map<String, String> getDataSourceBeanMap() {
		return dataSourceBeanMap;
	}

	public void setDataSourceBeanMap(Map<String, String> dataSourceBeanMap) {
		this.dataSourceBeanMap = dataSourceBeanMap;
	}  
	
	private void initDataSourceBean() {
		if(!CollectionUtils.isEmpty(dataSourceBeanMap)) {
			Map<Object, DataSource> dataSourceMap=new HashMap<>(16);
			for(Entry<String, String> entry:dataSourceBeanMap.entrySet()) {
				DataSource ds = SpringRefrenceBeanUtil.refrenceBean(entry.getValue(),DataSource.class);
//				if(ds ==null) {
//					throw new IllegalAccessError(String.format("DynamicDataSource bean for targetDataSources with %s datasource bean is null ", entry.getValue()));
//				}else if(!(ds instanceof DataSource)) {
//					throw new IllegalAccessError(String.format("DynamicDataSource bean for targetDataSources with %s  bean is not  datasource", entry.getValue()));
//				}
//				System.err.println(ds);
				dataSourceMap.put(entry.getKey(), (DataSource)ds);
			}
			if(CollectionUtils.isEmpty(dataSourceMap)) {
				throw new IllegalAccessError(String.format("DynamicDataSource bean for targetDataSources need kv datasource map"));
			}
			Field field = ReflectionUtils.findField(AbstractRoutingDataSource.class, "targetDataSources"); 
			// 设置可访问  
			field.setAccessible(true); 
			try {
				field.set(this, dataSourceMap);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(String.format("DynamicDataSource bean for targetDataSources set error"),e);
			}
		}
	}

	public String getMasterLable() {
		if(StringUtils.isEmpty(masterLable)) {
			return masterKey;
		}
		return masterLable;
	}

	public void setMasterLable(String masterLable) {
		this.masterLable = masterLable;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}  

