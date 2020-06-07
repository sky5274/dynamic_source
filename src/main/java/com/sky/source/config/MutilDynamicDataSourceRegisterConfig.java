package com.sky.source.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.sky.source.annotation.EnableDynamicDataConfig;
import com.sky.source.bean.DataSourceDefiend;
import com.sky.source.bean.DynamicDataSourceSessionDefined;
import com.sky.source.bean.SessionDefined;
import com.sky.source.dynamic.adapter.DynamicDataSourceAdapter;
import com.sky.source.factory.DataSourceSessionBuildFactory;
import com.sky.source.util.SpringRefrenceBeanUtil;

/**
 * 	数据源动态配置,根据配置，加载多数据源（包含主从数据）session-factory  以及主从策略，尝试多数据事务服务控制
 * 	<pre>
 * 	示例：yml格式
 * 	API.prefix: dynamicData.session
 * 	dynamicData:
 * 	  session:
 * 	    mapper-localtion:com/mysql_d/*.xml                ---
 * 	    base-package: com.mysql_d.*                         |
 *	    name: datasource                                    |    	  第一种数据源session配置，无数据源配置标签，默认使用系统默认环节配置
 *	    driverClassName:com.mysql.jdbc.Driver               |————	bean 的名称使用类名
 *	    url: jdbc:mysql://localhost:3306/test               |
 *	    username: root                                      |
 *	    password: root                                    ---
 * 	    mysql:                                            ---
 * 	      mapper-localtion:com/mysql/*.xml                  |
 * 	      base-package: com.mysql.*                         |
 *	      type: com.alibaba.druid.pool.DruidDataSource      |         第二种数据源session配置：数据源含有标签，在设置session factory 时添加lable到bean的名称中
 *	      name: mysql_data_source                           |————   使用默认的数据源类型设置数据源bean
 *	      driverClassName:com.mysql.jdbc.Driver             |
 *	      url: jdbc:mysql://localhost:3306/test             |
 *	      username: root                                    |
 *	      password: root                                  ---
 *	    mysql——1:                                         ---
 * 	      mapper-localtion:com/mysql1/*.xml                 |
 * 	      base-package: com.mysql1.*                        |
 * 	      slaver-method:find*,select*,query*                 |
 *	      master:                                           |
 *	        driverClassName:com.mysql.jdbc.Driver           |
 *	        url: jdbc:mysql://localhost:3306/test           |         第三种 数据源session配置：继承第二种配置的基础上，添加了主从数据库的配置模型
 *	        username: root                                  |————   使用的数据源是:com.sky.source.dynamic.DynamicDataSource 提供数据源
 *	        password: root                                  |       同时需要可以配置从数据源的方法切换aop
 *	      salver:                                           |
 *	        driverClassName:com.mysql.jdbc.Driver           |
 *	        url: jdbc:mysql://localhost:3306/test           |
 *	        username: root                                  |
 *	        password: root                                  |
 *	                                                      ---
 *	    mysql——2:                                         ---
 * 	      mapper-localtion:com/mysql1/*.xml                 |
 * 	      base-package: com.mysql1.*                        |
 * 	      slaver-method:find*,select*,query*                 |————	  第四种 数据源session配置
 * 	      type:com.sky.source.dynamic.DynamicDataSource     |       使用反射机制，配置数据源标签，并指定动态主从数据源
 *	      master: $(mysql-1)                                |       同时需要可以配置从数据源的方法切换aop
 *	      salver: #(orcale-2)                               |
 *	                                                      ---
 *	    datasource2:                                      ---
 * 	      mapper-localtion:com/mysql1/*.xml                 |
 * 	      base-package: com.mysql1.*                        |————      第五种数据源session配置模式
 * 	      datasource:$(mysql-1)                           ---       使用反射机制，配置数据源标签，使用springbean做数据源
 * </pre>
 * @author 王帆
 * @time 2019年12月18日 上午10:47:37
 */
public class MutilDynamicDataSourceRegisterConfig implements ImportBeanDefinitionRegistrar,EnvironmentAware{
	private static Log log=LogFactory.getLog(MutilDynamicDataSourceRegisterConfig.class);
	private static String PREFIX_KEY = "API.prefix";
	private static List<String> DEFAULT_DS_ADAPTER_METHOD = Arrays.asList("find*","select*","query.*","get*");
	/**属性键 名称前缀*/
	private  String defaultKey = "dynamicData.session";
	private Environment env;
	ResourcePatternResolver resourceResolver=new PathMatchingResourcePatternResolver();

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if(env==null) {
			return;
		} 
		if(importingClassMetadata != null) {
			AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableDynamicDataConfig.class.getName()));
			if(annoAttrs != null) {
				String temp = annoAttrs.getString("prefix");
				if(!StringUtils.isEmpty(temp)) {
					defaultKey=temp;
				}
			}
		}

		if(env instanceof StandardEnvironment) {
			String prefixKey = env.getProperty(PREFIX_KEY, defaultKey);

			//在spring context 环境属性配置中   获取 符合prefixkey 的键集合
			Set<String> keys = getDataSourceDefinedKeys(prefixKey);

			log.debug("dynamic data source build with spring evnironment");
			//通过获取的满足的key集合， 进行构建形成多种数据源配置信息
			Set<SessionDefined> sessionDefinedList = DataSourceSessionBuildFactory.prepare(prefixKey,keys).bulid(env);

			//加载datasource-session 配置；注册相关bean并以及执行加载行为
			loadSessionDefiend(registry,sessionDefinedList);
		}
		
		
		GenericBeanDefinition definition = prepareBeanDefined(SpringRefrenceBeanUtil.class);
		definition.setScope(BeanDefinition.SCOPE_SINGLETON);
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition,SpringRefrenceBeanUtil.class.getSimpleName()) ;
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}

	/**
	 * 	加载datasource 相关配置，以及执行注册行为
	 * @author 王帆
	 * @time 2019年12月19日 下午6:31:42
	 * @param registry
	 * @param sessionDefinedList
	 */
	protected void loadSessionDefiend(BeanDefinitionRegistry registry, Collection<SessionDefined> sessionDefinedList) {
		if(CollectionUtils.isEmpty(sessionDefinedList)) {
			return;
		}
		for(SessionDefined sdef:sessionDefinedList) {
			DataSourceDefiend datasource= sdef.getDataSourceDefiend();
			if(datasource != null) {
				registBean(registry,datasource);

				if(sdef instanceof DynamicDataSourceSessionDefined) {
					DynamicDataSourceSessionDefined dyDef = (DynamicDataSourceSessionDefined)sdef;
					for(DataSourceDefiend dsd:dyDef.getDataSourceList()) {
						registBean(registry, dsd);
					}
					//注册主从数据源 aop切换bean
					loadDynamicDataSourceAdapterDefiend(registry,sdef.getLable(),dyDef);
				}

				//根据数据源以及 session factory 配置信息 注册 session-factory and session-trancation, mapper-scanner 扫描mapper以及自动注入mapper
				loadDataSourceSessionBean(sdef,registry);
			}
		}
	}

	protected void loadDataSourceSessionBean(SessionDefined session,BeanDefinitionRegistry registry) {
		//注册datasource  ibatis sesion-factory
		if(session!=null && session.getDataSourceDefiend() !=null && session.getMapperLocaltion() !=null) {
			//preapre data source bean
			GenericBeanDefinition definition = prepareBeanDefined(SqlSessionFactoryBean.class);
			definition.setScope(BeanDefinition.SCOPE_SINGLETON);

			/*
			 *   bean defined property value add 
			 */
			definition.getPropertyValues().add("dataSource", new RuntimeBeanReference(session.getDataSourceDefiend().getName()));
			
			definition.getPropertyValues().add("mapperLocations", getResource(session.getMapperLocaltion()));
			if(session.getBeanName() ==null) {
				session.setBeanName((StringUtils.isEmpty(session.getLable())?"":session.getLable()+"_")+SqlSessionFactory.class.getSimpleName());
			}

			//regist sessionfactory bean into context
			BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition,session.getBeanName()) ;
			BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
			
			//init ibatis mapper source mapper scanner
			loadDataSourceMapperScanner(session,registry);
		}
	}
	
	/**
	 * 加载 动态（主从）数据源aop监听
	 * @author 王帆
	 * @param dyDatasourceDefiend 
	 * @param lable 
	 * @param registry 
	 * @time 2019年12月26日 下午4:56:39
	 */
	protected void loadDynamicDataSourceAdapterDefiend(BeanDefinitionRegistry registry, String lable, DynamicDataSourceSessionDefined dyDatasourceDefiend) {
		//定义aop 数据源适配器
		GenericBeanDefinition dynamicDataSourceAdapterDefined = prepareBeanDefined(DynamicDataSourceAdapter.class);
		Set<String> methods=new LinkedHashSet<>(DEFAULT_DS_ADAPTER_METHOD);
		methods.addAll(dyDatasourceDefiend.getSlaverMethod());
		dynamicDataSourceAdapterDefined.getPropertyValues().add("dataSource", new RuntimeBeanReference(dyDatasourceDefiend.getBeanName()));
		dynamicDataSourceAdapterDefined.getPropertyValues().add("defaultSlaverMethodStart", methods);
		BeanDefinitionHolder dynamicDataSourceAdapterDefinitionHolder = new BeanDefinitionHolder(dynamicDataSourceAdapterDefined,lable+"_"+DynamicDataSourceAdapter.class.getSimpleName()) ;
		BeanDefinitionReaderUtils.registerBeanDefinition(dynamicDataSourceAdapterDefinitionHolder, registry);

		//aop advisor defiend
		GenericBeanDefinition pointcutAdvisorDefined = prepareBeanDefined(DefaultPointcutAdvisor.class);
		JdkRegexpMethodPointcut pointcut=new JdkRegexpMethodPointcut();
		pointcut.setPattern(dyDatasourceDefiend.getBasePackage()+".*");
		pointcutAdvisorDefined.getConstructorArgumentValues().addIndexedArgumentValue(0, pointcut);
		pointcutAdvisorDefined.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference(dynamicDataSourceAdapterDefinitionHolder.getBeanName()));
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(pointcutAdvisorDefined,lable+"_"+pointcutAdvisorDefined.getBeanClassName()) ;
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}
	
	/**
	 * 根据文件模糊路径进行配置
	 * @author 王帆
	 * @time 2019年12月26日 下午4:54:11
	 * @param path
	 * @return
	 */
	private Resource[] getResource(String path) {
		List<Resource> resources = new ArrayList<Resource>();
	    if (path != null) {
	      for (String mapperLocation : path.split(",|;")) {
	        try {
	          Resource[] mappers = resourceResolver.getResources(mapperLocation);
	          resources.addAll(Arrays.asList(mappers));
	        } catch (IOException e) {
	          // ignore
	        }
	      }
	    }
	    return resources.toArray(new Resource[resources.size()]);
	}

	/**
	 *  load datasource mapper scanner
	 * @author 王帆
	 * @time 2019年12月23日 上午8:48:56
	 * @param session
	 * @param registry
	 */
	protected void loadDataSourceMapperScanner(SessionDefined session,BeanDefinitionRegistry registry) {
		//注册datasource  ibatis mapper scanner
		if(session!=null && session.getBeanName() !=null && session.getDataSourceDefiend() !=null && session.getBasePackage() !=null) {
			// ibatis mapper scan init
			ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
			scanner.setSqlSessionFactoryBeanName(session.getBeanName());
			scanner.registerFilters();
			scanner.scan(StringUtils.tokenizeToStringArray(session.getBasePackage(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
		}
	}

	/**
	 * 	获取env 中的前置key 符合的集合
	 * @author 王帆
	 * @time 2019年12月19日 下午6:27:11
	 * @param prefixKey
	 * @return
	 */
	protected Set<String> getDataSourceDefinedKeys(String prefixKey) {
		Set<String> keys = new LinkedHashSet<>();
		StandardEnvironment senv = (StandardEnvironment)env;
		Iterator<PropertySource<?>> its = senv.getPropertySources().iterator();
		//获取spring 属性配置资源中map\property 类型资源的属性key
		while(its.hasNext()) {
			PropertySource<?> s = its.next();
			Object ss = s.getSource();
			if(ss instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, String> ms = (Map<String, String>)ss;
				ms.keySet().stream().filter(it-> it.startsWith(prefixKey)).forEach(it-> keys.add(it));
			}
			if(ss instanceof Properties) {
				Properties ps = (Properties)ss;
				ps.keySet().stream().filter(it-> it.toString().startsWith(prefixKey)).forEach(it-> keys.add(it.toString()));
			}
		}
		return keys;
	}

	/**
	 * 	注册数据源
	 * @author 王帆
	 * @time 2019年12月19日 下午6:28:02
	 * @param registry
	 * @param datasourceDefined
	 */
	protected void registBean(BeanDefinitionRegistry registry,DataSourceDefiend datasourceDefined) {
		//判断是否是bean标签
		if( datasourceDefined.isBeanValue()) {
			return;
		}

		//preapre data source bean
		GenericBeanDefinition definition = prepareBeanDefined(datasourceDefined.getType());
		definition.setScope(BeanDefinition.SCOPE_SINGLETON);

		/*
		 *   bean defined property value add 
		 */
		Map<String, ? extends Object> propertyValues = datasourceDefined.getPropertyMap();
		propertyValues.entrySet().stream().forEach(it-> {
			definition.getPropertyValues().add(it.getKey(), it.getValue());	
		});
		//regist bean into context
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition,datasourceDefined.getName()) ;
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}

	/**
	 *	 根据class 类型 前置准备bean defiend
	 * @author 王帆
	 * @time 2019年12月19日 下午6:28:25
	 * @param clazzF
	 * @return
	 */
	private GenericBeanDefinition prepareBeanDefined(Class<?> clazz) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
		GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
		definition.setBeanClassName(clazz.getName());
		definition.setAutowireCandidate(true);
		definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
		return definition;
	}

	@Override
	public void setEnvironment(Environment environment) {
		env=environment;
	}
}
