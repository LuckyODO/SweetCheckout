package com.alipay.api.domain;

import java.util.List;

import com.alipay.api.AlipayObject;
import com.alipay.api.internal.mapping.ApiField;
import com.alipay.api.internal.mapping.ApiListField;

/**
 * 统一收单线下交易查询
修改路由策略到R
 *
 * @author auto create
 * @since 1.0, 2024-10-10 16:20:54
 */
public class AlipayTradeQueryModel extends AlipayObject {

	private static final long serialVersionUID = 7188238489752751173L;

	/**
	 * 订单支付时传入的商户订单号,和支付宝交易号不能同时为空。
trade_no,out_trade_no如果同时存在优先取trade_no
	 */
	@ApiField("out_trade_no")
	private String outTradeNo;

	/**
	 * 查询选项，商户传入该参数可定制本接口同步响应额外返回的信息字段，数组格式。
	 */
	@ApiListField("query_options")
	@ApiField("string")
	private List<String> queryOptions;

	public String getOutTradeNo() {
		return this.outTradeNo;
	}
	public void setOutTradeNo(String outTradeNo) {
		this.outTradeNo = outTradeNo;
	}

	public List<String> getQueryOptions() {
		return this.queryOptions;
	}
	public void setQueryOptions(List<String> queryOptions) {
		this.queryOptions = queryOptions;
	}

}
