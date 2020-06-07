package com.sky.source.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.NamedThreadLocal;

/** 
 * 	使用ThreadLocal技术来记录当前线程中的数据源的key 
 * @author wangfan 
 * 
 */  
public class DynamicDataSourceHolder {  
      
    /**写库对应的数据源key*/  
    static final String MASTER = "master";  
  
    /**读库对应的数据源key*/  
    private static final String SLAVE = "slave";  
      
    /**使用ThreadLocal记录当前线程的数据源key*/  
    private static final ThreadLocal<Map<DynamicDataSource, String>> HOLDER = new NamedThreadLocal<Map<DynamicDataSource, String>>("dynamic data source master/slaver");  
  
    /** 
     * 	设置数据源key 
     * @param key 
     */  
    public static void putDataSourceKey(DynamicDataSource dynamicDataSource,String key) {  
    	Map<DynamicDataSource, String> cache = HOLDER.get();
    	if(cache==null) {
    		cache=new HashMap<DynamicDataSource, String>(16);
    	}
    	cache.put(dynamicDataSource, key);
    	HOLDER.remove();
    	HOLDER.set(cache);  
    }  
  
    /** 
     * 	获取数据源key 
     * @param dynamicDataSource 
     * @return 
     */  
    public static String getDataSourceKey(DynamicDataSource dynamicDataSource) {  
    	if(HOLDER.get()==null) {
    		return MASTER;
    	}
    	
        String key = HOLDER.get().get(dynamicDataSource);
        if(key==null) {
        	key=MASTER;
        }
        return MASTER;
    }  
      
    /** 
     * 	标记写库 
     */  
    public static void markMaster(DynamicDataSource dynamicDataSource){  
        putDataSourceKey(dynamicDataSource,MASTER);  
    }  
      
    /** 
     * 	标记读库 
     */  
    public static void markSlave(DynamicDataSource dynamicDataSource){  
        putDataSourceKey(dynamicDataSource,SLAVE);  
    }

	public static boolean isMaster(DynamicDataSource dynamicDataSource) {
		return MASTER.equals(getDataSourceKey(dynamicDataSource))?true:false;
	}  
  
}  
