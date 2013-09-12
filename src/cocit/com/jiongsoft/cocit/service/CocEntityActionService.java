package com.jiongsoft.cocit.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nutz.lang.Mirror;

import com.jiongsoft.cocit.Cocit;
import com.jiongsoft.cocit.CocitHttpContext;
import com.jiongsoft.cocit.mvc.adaptor.EntityParamNode;
import com.jiongsoft.cocit.orm.Corm;
import com.jiongsoft.cocit.orm.expr.CndExpr;
import com.jiongsoft.cocit.orm.expr.Expr;
import com.jiongsoft.cocit.ui.widget.CuiWidgetModelFactory;
import com.jiongsoft.cocit.utils.ActionUtil;
import com.jiongsoft.cocit.utils.Json;
import com.jiongsoft.cocit.utils.Log;
import com.jiongsoft.cocit.utils.StringUtil;

/**
 * 实体Action服务类：通过路径参数即可构造出实体Action Service对象。
 * 
 * @author yongshan.ji
 * 
 */
public class CocEntityActionService {

	public CocSoftService softService;

	public Corm orm;

	public Throwable exception;

	/*
	 * 下列属性只有在opArgs存在的情况下才会被创建。
	 */
	public String opArgs;

	public Long moduleID;

	public Long tableID;

	public Long operationID;

	public String operationMode;

	public CocEntityModuleService moduleService;

	public CocEntityTableService tableService;

	public CocEntityOperationService operationService;

	public CuiWidgetModelFactory widgetFactory;

	/*
	 * 
	 */
	public CocEntityManager manager;

	public Object entity;

	public String[] entityID;

	/**
	 * 构造一个Action Service对象
	 * 
	 * @param opArgs
	 *            参数组成“moduleID:tableID:operationID”
	 * @return
	 */
	public static CocEntityActionService make(String opArgs) {
		return new CocEntityActionService(opArgs, null, null);
	}

	/**
	 * 
	 * @param opArgs
	 *            参数组成“moduleID:tableID:operationID”
	 * @param entityArgs
	 *            要加载的实体数据
	 * @param entityParamNode
	 *            实体HTTP参数节点
	 * @return
	 */
	public static CocEntityActionService make(String opArgs, String entityArgs, EntityParamNode entityParamNode) {
		return new CocEntityActionService(opArgs, entityArgs, entityParamNode);
	}

	private CocEntityActionService(String opArgs, String entityArgs, EntityParamNode entityParamNode) {
		Log.debug("CocEntityActionService... {opArgs:%s, entityArgs:%s, entityParamNode:%s}", opArgs, entityArgs, entityParamNode);

		this.opArgs = opArgs;
		try {
			softService = Cocit.getHttpContext().getSoftService();
			orm = softService.getOrm();

			widgetFactory = Cocit.getWidgetModelFactory();

			parseOpArgs(opArgs);

			/*
			 * 初始化实体管理器
			 */
			CocEntityManagerFactory entityManagerFactory = Cocit.getEntityManagerFactory();
			manager = entityManagerFactory.getEntityManager(moduleService, tableService);

			parseEntityArgs(entityArgs);

			// 注入HTTP参数到实体对象中
			if (entityParamNode != null && manager != null) {
				Class type = manager.getType();
				entity = entityParamNode.inject(Mirror.me(type), entity, null);
			}

		} catch (Throwable e) {
			Log.warn("", e);
			exception = e;
		}
	}

	private void parseOpArgs(String opArgs) {
		if (StringUtil.isNil(opArgs))
			return;

		String[] array = ActionUtil.decodeArgs(opArgs);

		String argModuleID = array.length > 0 ? array[0] : null;
		String argTableID = array.length > 1 ? array[1] : null;
		String argOperationID = array.length > 2 ? array[2] : null;

		Log.debug("CocEntityActionService.parseOpArgs: opArgs = %s {moduleID:%s, tableID:%s, operationID:%s}", opArgs, argModuleID, argTableID, argOperationID);

		/*
		 * 获取数据模块和数据表对象
		 */
		moduleID = StringUtil.castTo(argModuleID, 0L);
		tableID = StringUtil.castTo(argTableID, 0L);
		operationID = StringUtil.castTo(argOperationID, 0L);

		CocServiceFactory softFactory = Cocit.getServiceFactory();

		moduleService = softFactory.getEntityModuleService(moduleID);
		tableService = softFactory.getEntityTableService(moduleID, tableID);
		operationService = softFactory.getEntityOperationService(moduleID, tableID, operationID);
		if (operationService != null)
			operationMode = operationService.getOperationMode();

		Log.debug("CocEntityActionService.parseOpArgs: moduleService = %s, tableService = %s", moduleService, tableService);

	}

	private void parseEntityArgs(String entityArgs) {
		if (StringUtil.isNil(entityArgs))
			return;

		Log.debug("CocEntityActionService.parseEntityArgs: entityArgs = %s", entityArgs);

		// 加载单条数据
		entityID = StringUtil.toArray(entityArgs);
		if (entityID != null && entityID.length == 1) {
			Long id = StringUtil.castTo(entityID[0], 0L);
			entity = manager.load(id, operationMode);
		}
	}

	private CndExpr makeRuleExprFromJson(String jsonExpr, StringBuffer logExpr) {

		if (StringUtil.isNil(jsonExpr) || jsonExpr.charAt(0) != '{') {
			return null;
		}

		CndExpr retExpr = null;

		Map map = Json.fromJson(Map.class, jsonExpr);
		Iterator<String> exprs = map.keySet().iterator();
		while (exprs.hasNext()) {
			String prop = exprs.next();

			if (!StringUtil.isNil(prop)) {
				String value = map.get(prop).toString();

				if (!StringUtil.isNil(value)) {
					if (retExpr == null) {
						retExpr = Expr.contains(prop, value);

						logExpr.append("(" + prop + " contains " + value + ")");
					} else {
						retExpr = retExpr.and(Expr.contains(prop, value));

						logExpr.append(" and (" + prop + " contains " + value + ")");
					}
				}
			}
		}

		return retExpr;
	}

	/**
	 * 将JSON表达式转换成 in 表达式，即JSON对象中的字段值为数组。
	 * <p>
	 * JSON格式：
	 * <p>
	 * <code>
	 * {field-1:["value-1","value-2",...,"value-n"], field-2:[...], ... , field-n: [...]}
	 * </code>
	 */
	private CndExpr makeInExprFromJson(String jsonExpr, StringBuffer logExpr) {

		if (StringUtil.isNil(jsonExpr) || jsonExpr.charAt(0) != '{') {
			return null;
		}

		CndExpr retExpr = null;

		Map map = Json.fromJson(Map.class, jsonExpr);
		Iterator<String> exprs = map.keySet().iterator();
		while (exprs.hasNext()) {
			String prop = exprs.next();
			Object value = map.get(prop);
			if (value instanceof List) {
				List valueList = (List) value;

				if (retExpr == null) {
					retExpr = Expr.in(prop, valueList);

					logExpr.append("(" + prop + " in " + Json.toJson(valueList) + ")");
				} else {
					retExpr = retExpr.and(Expr.in(prop, valueList));

					logExpr.append(" and (" + prop + " in " + Json.toJson(valueList) + ")");
				}
			} else {
				String str = value.toString();

				if (retExpr == null) {
					retExpr = Expr.eq(prop, str);

					logExpr.append("(" + prop + " eq " + str + ")");
				} else {
					retExpr = retExpr.and(Expr.eq(prop, str));

					logExpr.append("and (" + prop + " in " + str + ")");
				}
			}
		}

		return retExpr;
	}

	/**
	 * 解析查询条件，可以支持如下三种查询表达式：
	 * <UL>
	 * <LI>参数(query.filterExpr)格式：{field-1:["value-1","value-2",...,"value-n"], field-2:[...], ... , field-n: [...]}
	 * <LI>参数(query.parentExpr)格式：{field-1:["value-1","value-2",...,"value-n"], field-2:[...], ... , field-n: [...]}
	 * <LI>参数(query.keywords)格式：{field-1 eq: "value-1", field-2 gt: value_2, ...}
	 * </UL>
	 */
	@SuppressWarnings("unused")
	public CndExpr makeExpr() {
		CocitHttpContext httpContext = Cocit.getHttpContext();

		StringBuffer logExpr = new StringBuffer();
		CndExpr retExpr = null;

		String queryFilterExpr = httpContext.getParameterValue("query.filterExpr", "");
		String queryParentExpr = httpContext.getParameterValue("query.parentExpr", "");
		String queryKeywords = httpContext.getParameterValue("query.keywords", "");

		CndExpr naviTreeExpr = this.makeInExprFromJson(queryFilterExpr, logExpr);
		CndExpr parentGridExpr = this.makeInExprFromJson(queryParentExpr, logExpr);
		CndExpr searchBoxExpr = this.makeRuleExprFromJson(queryKeywords, logExpr);
		if (naviTreeExpr != null) {
			if (retExpr == null)
				retExpr = naviTreeExpr;
			else
				retExpr = retExpr.and(naviTreeExpr);
		}
		if (parentGridExpr != null) {
			if (retExpr == null)
				retExpr = parentGridExpr;
			else
				retExpr = retExpr.and(parentGridExpr);
		}
		if (searchBoxExpr != null) {
			if (retExpr == null)
				retExpr = searchBoxExpr;
			else
				retExpr = retExpr.and(searchBoxExpr);
		}

		// 解析JSON表达式
		if (retExpr == null) {
			retExpr = Expr.notNull("id");
			logExpr.append("(id not null)");
		}

		/*
		 * 解析分页和排序
		 */
		int pageIndex = httpContext.getParameterValue("pageIndex", 1);
		int pageSize = httpContext.getParameterValue("pageSize", ActionUtil.DEFAULT_PAGE_SIZE);
		String sortField = httpContext.getParameterValue("sortField", "id");
		String sortOrder = httpContext.getParameterValue("sortOrder", "desc");

		if (sortOrder.toLowerCase().equals("asc")) {
			retExpr = retExpr.addAsc(sortField);
		} else {
			retExpr = retExpr.addDesc(sortField);
		}
		logExpr.append("( order by " + sortField + " " + sortOrder + ")");

		retExpr = retExpr.setPager(pageIndex, pageSize);
		logExpr.append("(pageIndex=" + pageIndex + " pageSize=" + pageSize + ")");

		/*
		 * 返回解析结果
		 */
		Log.info("查询条件：opArgs = %s, queryExpr = %s", opArgs, logExpr.toString());
		return retExpr;
	}
}
