package com.sky.source.factory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import com.sky.source.bean.SessionDefined;
import com.sky.source.factory.handle.DataSourceBuildHandle;
import com.sky.source.factory.handle.impl.DataSourceSessionDefaultBuildHandelImpl;
import com.sky.source.factory.handle.impl.DataSourceSessionDynamicBuildHandleImpl;
import com.sky.source.factory.handle.impl.DataSourceSessionSimplBuildHandleImpl;

/**
 * data-source session build factory
 * 
 * @see  com.sky.source.config.MutilDynamicDataSourceRegisterConfig
 * @author 王帆
 * @time 2019年12月18日 下午1:33:55
 */
public class DataSourceSessionBuildFactory {
	private static Log log=LogFactory.getLog(DataSourceSessionBuildFactory.class);
 	private static List<DataSourceBuildHandle> buildHandleList=new LinkedList<>();
	static {
		buildHandleList.add(new DataSourceSessionDefaultBuildHandelImpl());
		buildHandleList.add(new DataSourceSessionDynamicBuildHandleImpl());
		buildHandleList.add(new DataSourceSessionSimplBuildHandleImpl());
	}
	
	/**属性key资源集合*/
	private Set<String> sourceKeys=new  LinkedHashSet<>();
	/**属性key 的前缀*/
	private String prefix=null;
	
	/** 属性分布缓存 */
	private Map<String, Map<String, Object>> keyScatterMap=new HashMap<String,  Map<String, Object>>();
	
	/**
	 * 	准备构建多种数据源配置相关的构建工厂
	 * @author 王帆
	 * @time 2019年12月20日 下午1:33:06
	 * @param prefix   	属性key 的前缀
	 * @param keys		属性key集合
	 * @return
	 */
	public static DataSourceSessionBuildFactory prepare(String prefix,Collection<String> keys) {
		return new DataSourceSessionBuildFactory(prefix,keys);
	}
	
	
	public DataSourceSessionBuildFactory() {}
	public DataSourceSessionBuildFactory(String prefix,Collection<String> keys) {
		Assert.notNull(prefix,"dynamic data source config must with the key prefix");
		setPrefix(prefix);
		sourceKeys.addAll(keys);
	}
	
	/**
	 * 	根据spring 环境熟悉配置  设置构建的多数据源配置数据
	 * @author 王帆
	 * @time 2019年12月20日 下午1:34:17
	 * @param env
	 * @return
	 */
	public Set<SessionDefined> bulid(Environment env){
		//分析keys 集合，分类并汇总标签DataSource 属性
		initScattKeysToMap();
		
		//分析生成session defined 配置集合
		return buildSessionDefined(env);
	}
	
	/**
	 * 构建DataSource-session defined
	 * @author 王帆
	 * @time 2019年12月23日 下午1:07:17
	 * @param env
	 * @return
	 */
	protected Set<SessionDefined> buildSessionDefined(Environment env) {
		log.debug("start load session defined datasource");
		if(!keyScatterMap.isEmpty()) {
			Set<SessionDefined> sessions=new LinkedHashSet<SessionDefined>();
			Set<String> lableKeys = keyScatterMap.keySet();
			for(String lable:lableKeys) {
				sessions.add(buildSeession(lable,keyScatterMap.get(lable),env));
			}
			return sessions;
		}
		return null;
	}
	
	private SessionDefined buildSeession(String lable, Map<String, Object> map, Environment env) {
		if(CollectionUtils.isEmpty(map)) {
			return null;
		}
		/*
		 *	使用监听者模式开发 循环 构建datasurce-session
		 */
		 
		//数据源session 构建基本数据
		SessionDefined session=new SessionDefined();
		session.setLable(lable);
		session.setBasePackage(getEnvironmentValue(DataSourceBuildHandle.LIMIT_PROPERTY_KEY.get(1),map,env));
		session.setMapperLocaltion(getEnvironmentValue(DataSourceBuildHandle.LIMIT_PROPERTY_KEY.get(0),map,env));
		String beanName=getEnvironmentValue(DataSourceBuildHandle.LIMIT_PROPERTY_KEY.get(6), map, env);
		if(beanName==null) {
			beanName=getEnvironmentValue(DataSourceBuildHandle.LIMIT_PROPERTY_KEY.get(7), map, env);
		}
		session.setBeanName(beanName);
		for(DataSourceBuildHandle build:buildHandleList) {
			//session 的数据源配置未查询到，执行下一构建策略
			if(session.getDataSourceDefiend() !=null ) {
				break;
			}
			session=build.build(session, lable, map, env);
		}
		if(beanName==null) {
			beanName=lable;
			if(session.getDataSourceDefiend()!=null) {
				beanName+="_"+session.getDataSourceDefiend().getName();
			}
		}
		session.setBeanName(beanName);
		return session;
	}
	
	private String getEnvironmentValue(String key, Map<String, Object> map, Environment env) {
		Object v = map.get(key.toUpperCase());
		if(v !=null && v instanceof String) {
			return env.getProperty(v.toString());
		}
		return null;
	}
	
	/**
	 * 将spring key集合  按一级，二级 存储到系统的key-lable 缓存中
	 * @author 王帆
	 * @time 2019年12月21日 下午8:16:06
	 */
	protected void initScattKeysToMap() {
		if(!CollectionUtils.isEmpty(sourceKeys)) {
			int startIndex=prefix.length()+1;
			for(String key:sourceKeys) {
				String lableProperty=key.substring(startIndex).toUpperCase();
				List<String> attrs=new LinkedList<>(Arrays.asList(lableProperty.split("\\.")));
				if(attrs.isEmpty()) {
					continue;
				}
				switch (attrs.size()) {
				case 1:
					//无标签的  数据源配置
					setLableDefinedCache("",attrs.get(0),key);
					break;
				case 2:
					//有标签lable 的数据源配置
					setLableDefinedCache(key.substring(startIndex, key.indexOf(".", startIndex)),attrs.get(1),key);
					break;

				default:
					//有标签的  必定是 动态（主从） 数据源的配置分布缓存
					setLableDefinedCache(key.substring(startIndex, key.indexOf(".", startIndex)),
								lableProperty.replaceAll(attrs.get(0)+".","").replaceAll("."+attrs.get(attrs.size()-1), "").replaceAll("DATASOURCE.",""),
								attrs.get(attrs.size()-1),
								key);
					break;
				}
			}
			log.debug("data source config key map :"+keyScatterMap);
		}
	}
	
	/**
	 * 按照标签lable  key-value  存储属性key的值
	 * @author 王帆
	 * @time 2019年12月21日 下午8:14:08
	 * @param label
	 * @param key
	 * @param value
	 */
	protected void setLableDefinedCache(String label,String key,String value) {
		Map<String, Object> cache = keyScatterMap.get(label);
		if(cache==null) {
			cache=new HashMap<>(16);
		}
		if(key !=null) {
			key=key.replaceAll("-||_", "");
		}
		cache.put(key, value);
		keyScatterMap.put(label, cache);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setLableDefinedCache(String label,String sedLable,String key,String value) {
		Map<String, Object> cache = keyScatterMap.get(label);
		if(cache==null) {
			cache=new HashMap<>(16);
		}
		Object sedCache = cache.get(sedLable);
		if(sedCache==null) {
			sedCache=new HashMap<>(16);
		}
		if(sedCache instanceof Map) {
			Map sedCacheMap = (Map)sedCache;
			if(key !=null) {
				key=key.replaceAll("-||_", "");
			}
			sedCacheMap.put(key, value);
			cache.put(sedLable, sedCacheMap);
			keyScatterMap.put(label, cache);
		}
		
	}
	

	public Set<String> getSourceKeys() {
		return sourceKeys;
	}
	public void setSourceKeys(Set<String> sourceKeys) {
		this.sourceKeys = sourceKeys;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
