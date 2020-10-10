package com.sky.source.bean;

import java.util.Arrays;
import java.util.List;

/**
 *	动态数据源（主从数据）配置
 * @author 王帆
 * @time 2019年12月18日 下午1:30:23
 */
public class DynamicDataSourceSessionDefined extends SessionDefined{
	/***/
	private static final long serialVersionUID = 1192836010654746551L;
	public static List<String> DEFAULT_SALVER_METHOD_Like=Arrays.asList("find*","select*","query*","get*");
	
	private String aopPoint;
	private List<String> slaverMethod;
	private String primary;
	/**
	 *	动态主从数据源，包含的数据标签数据
	 */
	private List<DataSourceDefiend> dataSourceList;
	
	public DynamicDataSourceSessionDefined() {
		super();
	}
	public DynamicDataSourceSessionDefined(SessionDefined session) {
		super(session);
	}
	public DynamicDataSourceSessionDefined(SessionDefined session,List<DataSourceDefiend> dataSourceList) {
		super(session);
		this.setDataSourceList(dataSourceList);
	}

	public List<DataSourceDefiend> getDataSourceList() {
		return dataSourceList;
	}
	public void setDataSourceList(List<DataSourceDefiend> dataSourceList) {
		this.dataSourceList = dataSourceList;
	}
	public List<String> getSlaverMethod() {
		return slaverMethod;
	}
	public void setSlaverMethod(List<String> slaverMethod) {
		this.slaverMethod = slaverMethod;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataSourceList == null) ? 0 : dataSourceList.hashCode());
		result = prime * result + ((slaverMethod == null) ? 0 : slaverMethod.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
			
		if (getClass() != obj.getClass()) {
			return false;
		}
		DynamicDataSourceSessionDefined other = (DynamicDataSourceSessionDefined) obj;
		if (dataSourceList == null) {
			if (other.dataSourceList != null) {
				return false;
			}
		} else if (!dataSourceList.equals(other.dataSourceList)) {
			return false;
		}
		if (slaverMethod == null) {
			if (other.slaverMethod != null) {
				return false;
			}
		} else if (!slaverMethod.equals(other.slaverMethod)) {
			return false;
		}
		return true;
	}
	public String getPrimary() {
		return primary;
	}
	public void setPrimary(String primary) {
		this.primary = primary;
	}
	public String getAopPoint() {
		return aopPoint;
	}
	public void setAopPoint(String aopPoint) {
		this.aopPoint = aopPoint;
	}
	
	
}
