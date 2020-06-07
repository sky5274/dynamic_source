package com.sky.source.bean;

import java.io.Serializable;

import javax.sql.DataSource;

/**
 * data source session defined
 * @author 王帆
 * @time 2019年12月18日 下午1:25:52
 */
public class SessionDefined implements Serializable{
	/***/
	private static final long serialVersionUID = 7942831637911190083L;
	/**xml 路径*/
	private String mapperLocaltion;
	/** mapper 扫描路径*/
	private String basePackage;
	/**数据源标签*/
	private String lable;
	private String beanName;
	
	private DataSourceDefiend dataSourceDefiend;
	
	public SessionDefined() {}
	public SessionDefined(SessionDefined session) {
		if(session!=null) {
			this.setLable(session.getLable());
			this.setBasePackage(session.getBasePackage());
			this.setMapperLocaltion(session.getMapperLocaltion());
			this.setDataSourceDefiend(session.getDataSourceDefiend());
		}
	}

	public String getMapperLocaltion() {
		return mapperLocaltion;
	}

	public void setMapperLocaltion(String mapperLocaltion) {
		this.mapperLocaltion = mapperLocaltion;
	}

	public String getBasePackage() {
		return basePackage;
	}

	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

	public String getLable() {
		return lable;
	}

	public void setLable(String lable) {
		this.lable = lable;
	}

	public DataSourceDefiend getDataSourceDefiend() {
		return dataSourceDefiend;
	}

	public void setDataSourceDefiend(DataSourceDefiend dataSourceDefiend) {
		this.dataSourceDefiend = dataSourceDefiend;
	}
	public String getBeanName() {
		return beanName;
	}
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
	
	/**
	 * 获取数据源类型
	 * @author 王帆
	 * @time 2019年12月21日 下午10:23:07
	 * @return
	 */
	public Class<? extends DataSource> getDataSourceType(){
		return this.dataSourceDefiend==null? null:this.dataSourceDefiend.getType();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((basePackage == null) ? 0 : basePackage.hashCode());
		result = prime * result + ((dataSourceDefiend == null) ? 0 : dataSourceDefiend.hashCode());
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		result = prime * result + ((lable == null) ? 0 : lable.hashCode());
		result = prime * result + ((mapperLocaltion == null) ? 0 : mapperLocaltion.hashCode());
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
			
		SessionDefined other = (SessionDefined) obj;
		if (basePackage == null) {
			if (other.basePackage != null)
				return false;
		} else if (!basePackage.equals(other.basePackage)) {
			return false;
		}
		if (dataSourceDefiend == null) {
			if (other.dataSourceDefiend != null) {
				return false;
			}
		} else if (!dataSourceDefiend.equals(other.dataSourceDefiend)) {
			return false;
		}
		if (lable == null) {
			if (other.lable != null) {
				return false;
			}
		} else if (!lable.equals(other.lable)) {
			return false;
		}
		if (beanName == null) {
			if (other.beanName != null) {
				return false;
			}
		} else if (!beanName.equals(other.beanName)) {
			return false;
		}
		if (mapperLocaltion == null) {
			if (other.mapperLocaltion != null) {
				return false;
			}
		} else if (!mapperLocaltion.equals(other.mapperLocaltion)) {
			return false;
		}
		return true;
	}
}
