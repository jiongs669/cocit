package com.kmetop.demsy.mvc.view;

import org.nutz.ioc.Ioc;
import org.nutz.mvc.View;
import org.nutz.mvc.ViewMaker;

import com.jiongsoft.cocit.cui.CuiModelView;
import com.kmetop.demsy.lang.Str;
import com.kmetop.demsy.mvc.MvcConst;

public class DemsyViewMaker implements ViewMaker, MvcConst {

	@Override
	public View make(Ioc ioc, String type, String value) {
		if (Str.isEmpty(value)) {
			int idx = type.indexOf('.');
			if (idx > -1) {
				value = type.substring(0, idx);
				type = value;
			}
		}

		if (type.equals(VW_BIZ))
			return new BizView();
		if (type.equals("st"))
			return new SmartyView(value);
		if (type.equals(CuiModelView.VIEW_TYPE)) {
			return CuiModelView.make();
		}

		return null;
	}
}
