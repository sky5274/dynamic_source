package com.sky.source.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import com.sky.source.config.MutilDynamicDataSourceRegisterConfig;


/**
 * 	动态配置数据源可用
 * @author 王帆
 * @time 2019年12月18日 上午10:55:45
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({ TYPE })
@Import(MutilDynamicDataSourceRegisterConfig.class)
public @interface EnableDynamicDataConfig {
	
	@AliasFor("prefix")
	String value() default "";
	
	/**
	 * 	配置api前缀
	 * @author 王帆
	 * @time 2019年12月18日 上午10:54:49
	 * @return
	 */
	@AliasFor("value")
	String prefix() default ""; 
}
