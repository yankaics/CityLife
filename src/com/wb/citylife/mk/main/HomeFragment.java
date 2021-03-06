package com.wb.citylife.mk.main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Layout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.NetworkImageView.NetworkImageListener;
import com.common.media.BitmapHelper;
import com.common.widget.ToastHelper;
import com.viewpagerindicator.LinePageIndicator;
import com.wb.citylife.R;
import com.wb.citylife.adapter.AdvPagerAdapter;
import com.wb.citylife.adapter.ChannelAdapter;
import com.wb.citylife.app.CityLifeApp;
import com.wb.citylife.bean.Advertisement;
import com.wb.citylife.bean.db.DbChannel;
import com.wb.citylife.bean.db.DbScrollNews;
import com.wb.citylife.bean.db.User;
import com.wb.citylife.config.ActionConfig;
import com.wb.citylife.config.ChannelType;
import com.wb.citylife.config.IntentExtraConfig;
import com.wb.citylife.config.NetConfig;
import com.wb.citylife.dialog.ChannelDialog;
import com.wb.citylife.mk.channel.AddChannelActivity;
import com.wb.citylife.mk.channel.OrderChannelActivity;
import com.wb.citylife.mk.estate.EstateListActivity;
import com.wb.citylife.mk.merchant.MerchantListActivity;
import com.wb.citylife.mk.mycenter.AccountManagerActivity;
import com.wb.citylife.mk.mycenter.LoginActivity;
import com.wb.citylife.mk.news.NewsListActivity;
import com.wb.citylife.mk.old.OldInfoListActivity;
import com.wb.citylife.mk.shoot.ShootListActivity;
import com.wb.citylife.mk.vote.VoteListActivity;
import com.wb.citylife.widget.GrideViewForScrollView;

public class HomeFragment extends Fragment implements HomeListener,
	OnItemClickListener, OnItemLongClickListener, OnClickListener, NetworkImageListener{
	
	//广告自动播放的时间间隔
	public static final int ADV_AUTO_MOVE_TIME = 1 * 3 * 1000;
	
	private Activity mActivity;
	private MainListener mainListener;
		
	private ScrollView mScrollView;
	
	//头像、姓名
	private ViewGroup userVg;
	private NetworkImageView mAvatarIv;
	private TextView mUserTv;
	
	//广告
	private ViewPager mAdvViewPager;
	private AdvPagerAdapter mAdvAdapter;
	private LinePageIndicator mAdvIndicator;
	private AdvTimeCount advAdvTimeCount;
	private Advertisement mAdv;	
	private List<DbScrollNews> scrollNewsList;
	private TextView advTitleTv;
	
	//栏目
	private GrideViewForScrollView mTypeGrideView;
	private ChannelAdapter mChannelAdapter;
	private List<DbChannel> mChannelList;
	
	//编辑的栏目位置
	private int channelPosition;
	
	private ChannelDialog optionDialog;
	
	Handler mHandler = new Handler();
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
		mainListener = (MainListener)activity;		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		IntentFilter intentFilter = new IntentFilter();
	    intentFilter.addAction(ActionConfig.ACTION_UPDATE_CHANNEL);
	    mActivity.registerReceiver(mReceiver, intentFilter); 
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.main_item_home, container, false);		
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);		
		initView(view);
		mainListener.setHomeListener(this);
//		advTest();
	}
	
	@SuppressLint("NewApi") 
	private void initView(View view) {
		mScrollView = (ScrollView) view.findViewById(R.id.scroll);
		userVg = (ViewGroup) view.findViewById(R.id.user_layout);
		mAvatarIv = (NetworkImageView) view.findViewById(R.id.avatar);
		mUserTv = (TextView) view.findViewById(R.id.username);
		mAvatarIv.setDefaultImageResId(R.drawable.default_avatar);
		mAvatarIv.setNetworkImageListener(this);		
		userVg.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(CityLifeApp.getInstance().checkLogin()) {
					Intent intent = new Intent(mActivity, AccountManagerActivity.class);
					startActivity(intent);
				} else {
					Intent intent = new Intent(mActivity, LoginActivity.class);
					startActivity(intent);
				}
			}
		});
		
		mAdvViewPager = (ViewPager) view.findViewById(R.id.adv_pager);
		mAdvIndicator = (LinePageIndicator) view.findViewById(R.id.adv_indicator);
		advTitleTv = (TextView) view.findViewById(R.id.title);
		mTypeGrideView = (GrideViewForScrollView) view.findViewById(R.id.type_grid);	
		mTypeGrideView.setOnItemClickListener(this);
		mTypeGrideView.setOnItemLongClickListener(this);
//		if(VERSION.SDK_INT >= 11) {
//			LayoutTransition transition = new LayoutTransition();
//			transition.disableTransitionType(LayoutTransition.APPEARING);
//			mTypeGrideView.setLayoutTransition(transition);
//		}
		
		optionDialog = new ChannelDialog(mActivity, R.style.popupStyle);
		optionDialog.setListener(this);
		
		advAdvTimeCount = new AdvTimeCount(ADV_AUTO_MOVE_TIME, ADV_AUTO_MOVE_TIME);
		mAdvIndicator.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				advTitleTv.setText(scrollNewsList.get(position).title);
				advAdvTimeCount.cancel();
				advAdvTimeCount.start();
			}
			
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//				advTitleTv.setText(scrollNewsList.get(position).title);
//				Log.d("onPageScrolled", position+"");
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
				
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		initUser();
	}
	
	private void initUser() {
		if(CityLifeApp.getInstance().checkLogin()) {
			mAvatarIv.setImageUrl(CityLifeApp.getInstance().getUser().avatarUrl, CityLifeApp.getInstance().getImageLoader());
			User user = CityLifeApp.getInstance().getUser();
			if(TextUtils.isEmpty(user.nickname)) {
				mUserTv.setText(getDateSx() + ", " + user.userphone);
			} else {
				mUserTv.setText(getDateSx() + ", " + user.nickname);
			}
		} else {
			mUserTv.setText("请先登录、注册");
		}
	}
	
	@Override
	public void onLoadLocalChannel(List<DbChannel> channelList) {
		mChannelList = channelList;
		mChannelAdapter = new ChannelAdapter(mActivity, channelList, true);
		mTypeGrideView.setAdapter(mChannelAdapter);
	}
	
	@Override
	public void onLoadLocalScrollNews(List<DbScrollNews> scrollNews) {
		scrollNewsList = scrollNews;
		mAdvAdapter = new AdvPagerAdapter(mActivity, scrollNewsList);
		mAdvViewPager.setAdapter(mAdvAdapter);
		mAdvIndicator.setViewPager(mAdvViewPager);
		advTitleTv.setText(scrollNewsList.get(0).title);
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {	
				mScrollView.scrollTo(0, 0);
			}
		}, 500);
	}
	
	@Override
	public void onChannelComplete(List<DbChannel> channelList) {
		if(mChannelList == null) {
			mChannelList = channelList;
			mChannelAdapter = new ChannelAdapter(mActivity, channelList, true);
			mTypeGrideView.setAdapter(mChannelAdapter);
			mScrollView.fullScroll(ScrollView.FOCUS_UP);
		} else {			
			mChannelAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onScrollNewsCommplete(List<DbScrollNews> scrollNews) {	
		if(scrollNews == null || scrollNews.size() == 0) 
			return;
		if(scrollNewsList == null) {
			scrollNewsList = scrollNews;
			mAdvAdapter = new AdvPagerAdapter(mActivity, scrollNewsList);
			mAdvViewPager.setAdapter(mAdvAdapter);
			mAdvIndicator.setViewPager(mAdvViewPager);
			if(scrollNews.size() > 0) {
				advTitleTv.setText(scrollNews.get(0).title);
				advAdvTimeCount.cancel();
				advAdvTimeCount.start();
			}
		} else {
			mAdvAdapter.notifyDataSetChanged();
			advTitleTv.setText(scrollNews.get(mAdvViewPager.getCurrentItem()).title);
		}		
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		DbChannel channel = (DbChannel) mChannelAdapter.getItem(position);
		switch(channel.type) {
		case ChannelType.CHANNEL_TYPE_NEWS:
			Intent newsIntent = new Intent(getActivity(), NewsListActivity.class);
			newsIntent.putExtra(IntentExtraConfig.CHANNEL_ID, channel.channelId);
			newsIntent.putExtra(IntentExtraConfig.CHANNEL_NAME, channel.name);
			startActivity(newsIntent);
			break;
			
		case ChannelType.CHANNEL_TYPE_VOTE:
			startActivity(new Intent(getActivity(), VoteListActivity.class));
			break;
			
		case ChannelType.CHANNEL_TYPE_OLD_MARKET:
			startActivity(new Intent(getActivity(), OldInfoListActivity.class));
			break;
			
		case ChannelType.CHANNEL_TYPE_SHOOT:
			startActivity(new Intent(getActivity(), ShootListActivity.class));
			break;
			
		case ChannelType.CHANNEL_TYPE_ESTATE:
			startActivity(new Intent(getActivity(), EstateListActivity.class));
			break;
			
		case ChannelType.CHANNEL_TYPE_MERCHANT:
			startActivity(new Intent(getActivity(), MerchantListActivity.class));
			break;
			
		case ChannelType.CHANNEL_TYPE_ADD:
			Intent intent = new Intent(mActivity, AddChannelActivity.class);
			CityLifeApp.getInstance().setChannels(mChannelList);
			mActivity.startActivity(intent);
			break;
		}		

	}
	
	/**
	 * 长按栏目弹出菜选项
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		DbChannel channel = (DbChannel) mChannelAdapter.getItem(position);
		if(channel.getType() != ChannelType.CHANNEL_TYPE_ADD) {
			channelPosition = position;			
			optionDialog.show();
		}
		return true;
	}	
	
	@Override
	public void onClick(View v) {
		optionDialog.dismiss();
		
		switch(v.getId()) {		
		case R.id.box_option_batch_manager:
			//栏目排序
			CityLifeApp.getInstance().setChannels(mChannelList);
			Intent intent = new Intent(mActivity, OrderChannelActivity.class);
			mActivity.startActivity(intent);
			break;
			
		case R.id.box_option_setlauncher:
			//发送至桌面
			addShortCut(channelPosition);
			break;
			
		case R.id.box_option_delete_item:
			//删除栏目
			mChannelAdapter.delChannel(channelPosition);			
			break;
		}
	}
		
//	@Override
//	public void onClick(View v) {
//		Item item = (Item)v.getTag(); 
//		if((item.getHolder().delBtn.getVisibility() == View.VISIBLE)) {
//			//提示用户是否要删除栏目
//			Dialog dialog = new ConfirmDialog().getDialog(getActivity(), "", "确认要删除"+item.getName()+"吗？", 
//					new DelConfirmListener(item));
//			dialog.setOnDismissListener(new OnDismissListener() {
//				
//				@Override
//				public void onDismiss(DialogInterface dialog) {
//					mTypeGrideView.cleanDelState();
//				}
//			});
//			dialog.show();
//		} else {
//			//跳转到栏目中
//			((ScaleLinearLayout) v).clickZoomOut();
//			if(item.getId() != -1) {
//				switch((int)item.getId()) {
//				case ChannelType.CHANNEL_TYPE_NEWS:
//					startActivity(new Intent(getActivity(), NewsListActivity.class));
//					break;
//				}
//			} else {
//				mTypeGrideView.cleanDelState();
//			}
//		}
//	}
//	
//	class DelConfirmListener implements DialogInterface.OnClickListener {
//		
//		private Item item;
//		
//		public DelConfirmListener(Item item) {
//			this.item = item;
//		}
//		
//		@Override
//		public void onClick(DialogInterface dialog, int which) {
//			int index = mTypeAdapter.indexOfItem(0, item);						
//			mTypeAdapter.deleteItem(0, index);
//			mTypeGrideView.notifyDataSetChanged();	
//		}
//	}
	
	/**
	 * 广告播放时间计时器
	 * @author liangbx
	 *
	 */
	class AdvTimeCount extends CountDownTimer {

		public AdvTimeCount(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			int currentItem = mAdvViewPager.getCurrentItem();
			if(currentItem < mAdvViewPager.getChildCount() - 1) {
				currentItem++;
			} else {
				currentItem = 0;
			}
			mAdvIndicator.setCurrentItem(currentItem);
			advAdvTimeCount.cancel();
			advAdvTimeCount.start();
		}

		@Override
		public void onTick(long millisUntilFinished) {
			
		}		
	}
	
	 BroadcastReceiver mReceiver = new BroadcastReceiver() {
			
		 @Override
		 public void onReceive(Context context, Intent intent) {
		    if(intent.getAction().equals(ActionConfig.ACTION_UPDATE_CHANNEL)) {
		    	mChannelList = CityLifeApp.getInstance().getChannels();
		    	if(mChannelList == null) {
		    		mChannelList = new ArrayList<DbChannel>();
		    	}
		    	mChannelAdapter = new ChannelAdapter(mActivity, mChannelList, true);
				mTypeGrideView.setAdapter(mChannelAdapter);
				CityLifeApp.getInstance().setChannels(null);
		    } 
		}
	};
	
	@Override
	public void onDestroy() {
		mActivity.unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	
	/**
	 * 添加图标
	 * @param tName 快捷方式的名称
	 */
	private void addShortCut(int position) {		
		final DbChannel channel = mChannelAdapter.getRealChannel(position);
		
        CityLifeApp.getInstance().getImageLoader().get(NetConfig.getPictureUrl(channel.getImageUrl()), new ImageListener() {
			
			@Override
			public void onErrorResponse(VolleyError arg0) {
				
			}
			
			@Override
			public void onResponse(ImageContainer container, boolean arg1) {
				
				// 安装的Intent  
		        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");

		        // 快捷名称  
		        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, channel.name);
		        // 快捷图标是允许重复
		        shortcut.putExtra("duplicate", false);

		        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
		        switch(channel.type) {
		        case ChannelType.CHANNEL_TYPE_NEWS:
		        	shortcutIntent.setClassName("com.wb.citylife", "com.wb.citylife.mk.news.NewsListActivity");
		        	break;
		        	
		        case ChannelType.CHANNEL_TYPE_VOTE:
		        	shortcutIntent.setClassName("com.wb.citylife", "com.wb.citylife.mk.vote.VoteListActivity");
		        	break;
		        	
		        case ChannelType.CHANNEL_TYPE_OLD_MARKET:
		        	shortcutIntent.setClassName("com.wb.citylife", "com.wb.citylife.mk.old.OldInfoListActivity");
		        	break;
		        	
		        case ChannelType.CHANNEL_TYPE_SHOOT:
		        	shortcutIntent.setClassName("com.wb.citylife", "com.wb.citylife.mk.shoot.ShootListActivity");
		        	break;
		        	
		        case ChannelType.CHANNEL_TYPE_ESTATE:
		        	shortcutIntent.setClassName("com.wb.citylife", "com.wb.citylife.mk.estate.EstateListActivity");
		        	break;
		        	
		        case ChannelType.CHANNEL_TYPE_MERCHANT:
		        	shortcutIntent.setClassName("com.wb.citylife", "com.wb.citylife.mk.merchant.MerchantListActivity");
		        	break;
		        }
		        
		        shortcutIntent.putExtra(IntentExtraConfig.CHANNEL_ID, channel.channelId);
		        shortcutIntent.putExtra(IntentExtraConfig.CHANNEL_NAME, channel.name);
		        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);		     

		        // 快捷图标  
		        if(container.getBitmap() == null) {
		        	ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(mActivity, R.drawable.ic_launcher);       
		        	shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
		        } else {
		        	shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, container.getBitmap());
		        }
		        
		        // 发送广播  
		        mActivity.sendBroadcast(shortcut);
		        
		        ToastHelper.showToastInBottom(mActivity, "图标发送成功");
			}
		});     
    }
	
	@Override
	public void onGetBitmapListener(ImageView imageView, Bitmap bitmap) {
		if(bitmap != null) {
			imageView.setImageBitmap(BitmapHelper.toRoundCorner(bitmap, bitmap.getHeight()/2));
		}
	}  
	
	public String getDateSx() {
		String sx = "";
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		if (hour >= 6 && hour < 8) {
			sx = "早上好";
		} else if (hour >= 8 && hour < 11) {
			sx = "上午好";
		} else if (hour >= 11 && hour < 13) {
			sx = "中午好";
		} else if (hour >= 13 && hour < 19) {
			sx = "下午好";
		} else {
			sx = "晚上好";
		}
		
		return sx;
	}
	
	/************************************************ 测试数据 **********************************************/
//	private void advTest() {
//		mAdv = new Advertisement();
//		mAdv.respCode = 0;
//		mAdv.respMsg = "ok";
//		mAdv.totalCount = 2;
//		mAdv.resources = new ArrayList<Advertisement.AdvItem>();
//		
//		Advertisement.AdvItem item  = mAdv.new AdvItem();
//		item.id = "1";
//		item.imageUrl = "http://pic27.nipic.com/20130313/1628220_145734522153_2.jpg";
//		item.title = "舞动青春";
//		item.linkUrl = "";
//		mAdv.resources.add(item);
//		
//		item  = mAdv.new AdvItem();
//		item.id = "2";
//		item.imageUrl = "http://pic16.nipic.com/20110910/4582261_110721084388_2.jpg";
//		item.title = "创意无限";
//		item.linkUrl = "";
//		mAdv.resources.add(item);
//		
//		mAdvAdapter = new AdvPagerAdapter(mActivity, mAdv);
//		mAdvViewPager.setAdapter(mAdvAdapter);
//		mAdvIndicator.setViewPager(mAdvViewPager);
//	}
				
}
	
