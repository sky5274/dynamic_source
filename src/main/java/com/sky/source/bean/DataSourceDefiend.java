package com.sky.source.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * 	数据源属性配置数据定义
 * @author 王帆
 * @time 2019年12月19日 下午5:20:10
 */
public class DataSourceDefiend implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private Class<? extends DataSource> type;
	private Map<String, ? extends Object> propertyMap=new HashMap<String, Object>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<String, ? extends Object> getPropertyMap() {
		return propertyMap;
	}
	public void setPropertyMap(Map<String, ? extends Object> propertyMap) {
		this.propertyMap = propertyMap;
	}
	public Class<? extends DataSource> getType() {
		return type;
	}
	public void setType(Class<? extends DataSource> type) {
		this.type = type;
	}
	public  boolean isBeanValue() {
		if(name !=null) {
			name=name.trim();
			if( name.endsWith("}")) {
				if(name.startsWith("${") || name.startsWith("#{")) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((propertyMap == null) ? 0 : propertyMap.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		DataSourceDefiend other = (DataSourceDefiend) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (propertyMap == null) {
			if (other.propertyMap != null) {
				return false;
			}
		} else if (!propertyMap.equals(other.propertyMap)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}
	
	
}
