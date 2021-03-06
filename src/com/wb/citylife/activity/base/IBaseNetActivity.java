package com.wb.citylife.activity.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.wb.citylife.app.CityLifeApp;

/**
 * 此类用于初始化网络请求队列和停止网络请求
 * @author liangbx
 *
 */
public class IBaseNetActivity extends FragmentActivity{
	
	public RequestQueue mQueue;
	private String requestTag;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		mQueue = CityLifeApp.getInstance().getRequestQueue();	
		requestTag = CityLifeApp.getInstance().getRequestTag();
	}
	
	@Override
	protected void onDestroy() {			
		if(mQueue != null)
			mQueue.cancelAll(requestTag);
		super.onDestroy();
	}
	
	/**
	 * 启动请求队列
	 * @param request
	 */
	@SuppressWarnings("rawtypes")
	public void startRequest(Request request) {
		request.setTag(requestTag);
		mQueue.add(request);
		mQueue.start();
	}
		
	/**
	 * 网络请求的错误处理
	 * @param respCode 响应码
	 * @param respMsg 错误信息 为null时不显示错误信息
	 */
	public void processError(int respCode, String respMsg) {
		
	}
}
