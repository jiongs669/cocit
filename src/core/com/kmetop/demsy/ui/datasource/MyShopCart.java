package com.kmetop.demsy.ui.datasource;

import static com.kmetop.demsy.mvc.MvcConst.URL_UI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jiongsoft.cocit.orm.expr.CndExpr;
import com.kmetop.demsy.Demsy;
import com.kmetop.demsy.actions.OrderActions;
import com.kmetop.demsy.comlib.eshop.IOrderItem;
import com.kmetop.demsy.comlib.eshop.IProduct;
import com.kmetop.demsy.comlib.security.IModule;
import com.kmetop.demsy.comlib.ui.IPageBlock;
import com.kmetop.demsy.config.SoftConfigManager;
import com.kmetop.demsy.lang.Str;
import com.kmetop.demsy.mvc.MvcConst.MvcUtil;
import com.kmetop.demsy.mvc.ui.UIBlockContext;
import com.kmetop.demsy.mvc.ui.UIBlockDataModel;
import com.kmetop.demsy.util.sort.SortUtils;

public class MyShopCart extends UiBaseDataSource {

	protected CndExpr getExpr(UIBlockContext parser) {
		return null;
	}

	public Map process(UIBlockContext parser) {
		Map ctx = new HashMap();

		Map<Long, IOrderItem> cart = OrderActions.getShopCart();
		Iterator<Long> items = cart.keySet().iterator();
		double itemsPrice = 0;
		int totalAmount = 0;
		List<IOrderItem> list = new LinkedList();
		while (items.hasNext()) {
			IOrderItem oi = cart.get(items.next());
			list.add(oi);
			itemsPrice += oi.getSubtotal();
			totalAmount += oi.getAmount();
		}
		SortUtils.sort(list, "createdBy", false);

		UIBlockDataModel data = parser.getCatalog();

		ctx.put("itemSize", list.size());
		ctx.put("itemsPrice", itemsPrice);
		ctx.put("totalAmount", totalAmount);

		Double postFee = itemsPrice >= SoftConfigManager.me().getEshopNotPostFee() ? 0 : SoftConfigManager.me().getEshopPostFee();
		ctx.put("totalPrice", itemsPrice + postFee);

		ctx.put("postFee", postFee);
		ctx.put("cfgPostFee", SoftConfigManager.me().getEshopPostFee());
		ctx.put("cfgNotPostFee", SoftConfigManager.me().getEshopNotPostFee());
		ctx.put("cfgNotPostFeeDesc", SoftConfigManager.me().getEshopNotPostFeeDesc());

		ctx.put("data", data);
		Long moduleID = null;
		Long linkID = null;
		IModule module = Demsy.moduleEngine.getModule(Demsy.me().getSoft(), Demsy.bizEngine.getSystem(IProduct.SYS_CODE));
		if (module != null)
			moduleID = module.getId();
		IPageBlock block = parser.getBlock();
		if (block.getLink() != null)
			linkID = block.getLink().getId();

		for (IOrderItem obj : list) {
			data.addItem(makeItemData(block, obj, linkID, moduleID));
		}

		return ctx;
	}

	public UIBlockDataModel makeItemData(IPageBlock block, IOrderItem obj, Long linkID, Long moduleID) {
		UIBlockDataModel item = new UIBlockDataModel();

		item.setName(obj.toString());

		item.setImg(Demsy.contextPath + obj.getProduct().getImage().toString());

		if (linkID != null && moduleID != null) {
			item.setHref(MvcUtil.contextPath(URL_UI, linkID, moduleID + ":" + obj.getProduct().getId()));
		}
		if (!Str.isEmpty(block.getLinkTarget())) {
			item.setTarget(block.getLinkTarget());
		}

		item.setObj(obj);

		return item;
	}
}
