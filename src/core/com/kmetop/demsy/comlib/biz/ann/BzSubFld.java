package com.kmetop.demsy.comlib.biz.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface BzSubFld {

	public long id() default 0;

	public String name() default "";

	public String code() default "";

	public String property() default "";

	public String desc() default "";

	public int order() default 0;

	public int gridOrder() default 0;

	public boolean gridField() default true;

	public String refrenceSystem() default "";

	public boolean masterMapping() default false;

	public boolean password() default false;

	public String options() default "";

	public String regexpMask() default "";

	public String pattern() default "";

	public int gridWidth() default 0;

	public String mode() default "";

	public String uiTemplate() default "";

	public String parent() default "";

	public boolean disabled() default false;

	public boolean disabledNavi() default false;

	String cascadeMode() default "";

	public boolean groupBy() default false;

	String uploadType() default "";

	boolean tostring() default false;

	String columnDefinition() default "";

	String defalut() default "";

	int length() default 512;

}
