package com.yanmiao.sbcnavigation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.navisdk.hudsdk.BNRemoteConstants;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNEnlargeRoad;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGAssistant;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGAuthSuccess;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCarFreeStatus;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCarInfo;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCarTunelInfo;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCruiseEnd;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCruiseStart;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCurShapeIndexUpdate;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGCurrentRoad;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGDestInfo;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGGPSLost;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGGPSNormal;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGManeuver;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGNaviEnd;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGNaviStart;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGNearByCameraInfo;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGNextRoad;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGRPYawComplete;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGRPYawing;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGRemainInfo;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGRouteInfo;
import com.baidu.navisdk.hudsdk.BNRemoteMessage.BNRGServiceArea;
import com.baidu.navisdk.hudsdk.client.BNRemoteVistor;
import com.baidu.navisdk.hudsdk.client.HUDConstants;
import com.baidu.navisdk.hudsdk.client.HUDSDkEventCallback;
import com.baidu.navisdk.hudsdk.client.HUDSDkEventCallback.OnConnectCallback;
import com.baidu.navisdk.hudsdk.client.HUDSDkEventCallback.OnRGInfoEventCallback;
import com.yanmiao.sbcnavigation.StringUtils.UnitLangEnum;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class MainActivity extends Activity {
    private final static String MODULE_TAG = "BNEVENT";
    private static final int REQUEST_CONNECT_DEVICE = 1;//选中蓝牙返回数据
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 2;//蓝牙是否打开状态
    public static final String DEVICE_NAME = "device_name";
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_STATE_CHANGE = 1;//服务开启 状态更改通知
    public static final int MESSAGE_TOAST = 5;//失败
    public static final String TOAST = "toast";//失败信息key
    private BluetoothChatService mChatService = null;//蓝牙通信处理服务类
    private String mConnectedDeviceName = null;//蓝牙名称
    private Button mSendButton;
    public static final int MESSAGE_WRITE = 3;//写入数据
    public static final int MESSAGE_READ = 2;//接收另一设备发送数据
    private View mSimpleRGLayout;
    private View mNaviAfterView = null;
    private ImageView mTurnIcon = null;// 转向信息
    private TextView mAfterMetersInfoTV = null;// 行驶n米后 表示n的值
    private TextView mAfterLabelInfoTV = null;
    private TextView mGoLabelTV = null;
    private TextView mGoWhereInfoTV = null;// 将要进入的目的地

    private View mAlongRoadView = null;
    private TextView mCurRoadNameTV = null;
    private TextView mCurRoadRemainDistTV = null;

    private TextView mNewCurRoadTv = null;

    private View mRemainInfoLayout;
    private TextView mTotalDistTV = null;
    private TextView mArriveTimeTV = null;

    private TextView mNavilogTv = null;// 导航信息反馈提示
    private EditText mServerIPTv = null;// 请输入服务器地址
    private Button mConnectBtn = null;
    private Button mCloseBtn = null;

    private Handler mMainHandler = null;
    private ProgressDialog mProgressDialog;

    private View mEnlargeRoadMapView = null;
    private ImageView mEnlargeImageView = null;
    private TextView mRemainDistTV = null;
    private TextView mNextRoadTV = null;
    private ProgressBar mProgressBar = null;
    private View mCarPosLayout;
    private View bnav_rg_enlarge_image_mask;
    private ImageView mCarPosImgView;

    private int mEnlargeType;
    private String mRoadName;
    private int mTotalDist;
    private int mRemDist;
    private int mProgress;
    private boolean mbUpdateRasterInfo;

    private int mCarPosX = 0;
    private int mCarPosY = 0;

    private Matrix mRotateMatrix;
    private int mCarRotate;

    private boolean mForceRefreshImage = false;
    /**
     * 初始化连接信息 判断是否连接成功 连接结果反馈
     */
    private OnConnectCallback mConnectCallback = new OnConnectCallback() {

        @Override
        public void onReConnected() {
            Log.e(BNRemoteConstants.MODULE_TAG, "reConnect to server success");
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mSimpleRGLayout.setVisibility(View.VISIBLE);
                    mRemainInfoLayout.setVisibility(View.VISIBLE);
                    mNavilogTv.setText("重新连接到百度导航");
                }
            });

        }

        @Override
        public void onConnected() {
            Log.e(BNRemoteConstants.MODULE_TAG, "connect to server success");
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    mSimpleRGLayout.setVisibility(View.VISIBLE);
                    mRemainInfoLayout.setVisibility(View.VISIBLE);
                    mNavilogTv.setText("成功连接到百度导航");

                    mConnectBtn.setClickable(false);
                    mCloseBtn.setClickable(true);
                }
            });
        }

        @Override
        public void onClose(int arg0, String arg1) {
            Log.e(BNRemoteConstants.MODULE_TAG, "MainActivity.................onClose()  disconnect, reason = " + arg1);
            final int reasonId = arg0;
            final String reason = arg1;
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    mSimpleRGLayout.setVisibility(View.GONE);
                    mRemainInfoLayout.setVisibility(View.GONE);
                    mNavilogTv.setText("连接断开, " + reason);

                    if (reasonId == HUDSDkEventCallback.OnConnectCallback.CLOSE_LBS_AUTH_FALIED) {
                        mConnectBtn.setClickable(false);
                        mCloseBtn.setClickable(false);
                    } else {
                        mConnectBtn.setClickable(true);
                        mCloseBtn.setClickable(false);
                    }
                }
            });
        }

        @Override
        public void onAuth(BNRGAuthSuccess arg0) {
            if (arg0 != null) {
                Log.d(BNRemoteConstants.MODULE_TAG, "auth success, serverVer = " + arg0.getServerVersion());
                final String serverVer = arg0.getServerVersion();
                mMainHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mNavilogTv.setText("认证成功， 服务器版本: " + serverVer);
                    }
                });
            }
        }

        @Override
        public void onStartLBSAuth() {

        }

        @Override
        public void onEndLBSAuth(int result, String reason) {
            // TODO Auto-generated method stub
            if (result == 0) {
                mConnectBtn.setClickable(true);
                mCloseBtn.setClickable(false);
            }
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE://蓝牙状态更改
//						Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
//                            mTitle.setText("已连接");
//                            mTitle.append(mConnectedDeviceName);
//                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING://正在连接
                            Toast.makeText(MainActivity.this, "正在连接", Toast.LENGTH_SHORT).show();
                            break;
                        //未连接
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Toast.makeText(MainActivity.this, "未连接", Toast.LENGTH_SHORT).show();
//							mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME://连接设备名称
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_WRITE://接收写入数据
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    Toast.makeText(getApplicationContext(), writeMessage, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_READ://接收另一设备发送数据
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST://连接失败
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    /////****//////////////////////////////////////////////////////
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);
        initViews();
        Log.e(BNRemoteConstants.MODULE_TAG, "onCreate..................");
        mMainHandler = new Handler(getMainLooper());
        BNRemoteVistor.getInstance().init(getApplicationContext(), getPackageName(),
                getAppVersionName(MainActivity.this, getPackageName()), mRGEventCallback, mConnectCallback);
        BNRemoteVistor.getInstance().setShowLog(true);
        // 获得蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // 启动蓝牙
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null)
                setupChat();

        }

    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE://和某一个设备通信
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT://蓝牙链接结果返回
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                    Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(this, "蓝牙被禁止，请开启蓝牙", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void setupChat() {
//		Log.d(TAG, "setupChat()");

//		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
//		mConversationView = (ListView) findViewById(R.id.in);
//		mConversationView.setAdapter(mConversationArrayAdapter);
//
//		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
//		mOutEditText.setOnEditorActionListener(mWriteListener);
//
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendMessage("bu");
            }
        });

        mChatService = new BluetoothChatService(this, mHandler);

//		mOutStringBuffer = new StringBuffer("");
    }

    /**
     * 发送信息
     *
     * @param message
     */
    private void sendMessage(String message) {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "不能连接这个设备", Toast.LENGTH_SHORT).show();
            return;
        }
        //写入数据
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    @Override
    public void onDestroy() {
        if (BNRemoteVistor.getInstance().isConnect()) {
            BNRemoteVistor.getInstance().close(HUDSDkEventCallback.OnConnectCallback.CLOSE_NORMAL, "User Exit");
        }
        BNRemoteVistor.getInstance().unInit();
        super.onDestroy();
        if (mChatService != null)
            mChatService.stop();
    }

    /**
     * 辅助诱导信息为导航或者电子狗诱导过程中的路面状况、测速摄像头等交通道路信息。
     * 在导航辅助诱导数据中，assistantType参数代表的辅助诱导类型，请根据实际需要对应自定义的诱导图标。
     */
    private OnRGInfoEventCallback mRGEventCallback = new OnRGInfoEventCallback() {
        @Override
        public void onAssistant(BNRGAssistant arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onAssistant......distance = " + arg0.getAssistantDistance()
                    + ", type = " + arg0.getAssistantType());

            String assistantTips = "";
            String assistantTypeS = "合流";
            if (arg0.getAssistantDistance() > 0) {
                switch (arg0.getAssistantType()) {
                    case HUDConstants.AssistantType.JOINT:
                        assistantTypeS = "合流";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.TUNNEL:
                        assistantTypeS = "隧道";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.BRIDGE:
                        assistantTypeS = "桥梁";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.RAILWAY:
                        assistantTypeS = "铁路";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.BLIND_BEND:
                        assistantTypeS = "急转弯";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.BLIND_SLOPE:
                        assistantTypeS = "陡坡";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.ROCKFALL:
                        assistantTypeS = "落石";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.ACCIDENT:
                        assistantTypeS = "事故多发区";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.SPEED_CAMERA:
                        assistantTypeS = "测速摄像";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS + "限速： "
                                + arg0.getAssistantLimitedSpeed();
                        break;
                    case HUDConstants.AssistantType.TRAFFIC_LIGHT_CAMERA:
                        assistantTypeS = "交通信号灯摄像";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.INTERVAL_CAMERA:
                        assistantTypeS = "区间测速";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.CHILDREN:
                        assistantTypeS = "注意儿童";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.UNEVEN:
                        assistantTypeS = "路面不平";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.NARROW:
                        assistantTypeS = "道路变窄";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.VILLAGE:
                        assistantTypeS = "前面村庄";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.SLIP:
                        assistantTypeS = "路面易滑";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.OVER_TAKE_FORBIDEN:
                        assistantTypeS = "禁止超车";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    case HUDConstants.AssistantType.HONK:
                        assistantTypeS = "请铭喇叭";
                        assistantTips = "前方" + getFormatAfterMeters(arg0.getAssistantDistance()) + assistantTypeS;
                        break;
                    default:
                        break;
                }
            }

            final String assistantTipstr = assistantTips;
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText(assistantTipstr);
                }
            });
        }

        /**
         * 电子狗状态信息主要包括电子狗开始、结束。
         */
        @Override
        public void onCruiseEnd(BNRGCruiseEnd arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "cruise end");
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("关闭电子狗");
                }
            });
        }

        @Override
        public void onCruiseStart(BNRGCruiseStart arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "cruise start");
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("开启电子狗");
                }
            });
        }

        ////////////////////////////////

        /**
         * 当前路名指当前定位点道路名信息（诱导下一段路名信息请参见“机动点”相关内容）。
         */
        @Override
        public void onCurrentRoad(BNRGCurrentRoad arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onCurrentRoad...........curRoadName = " + arg0.getCurrentRoadName());

            final String curRoadName = arg0.getCurrentRoadName();
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNewCurRoadTv.setText("当前道路: " + curRoadName);
                    mNewCurRoadTv.setVisibility(View.VISIBLE);
                }
            });
        }

        /**
         * GPS信息回调主要用于进行GPS状态正常或者丢星的通知
         */
        @Override
        public void onGPSLost(BNRGGPSLost arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onGPSLost....");
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("GPS信号丢失");
                }
            });
        }

        @Override
        public void onGPSNormal(BNRGGPSNormal arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onGPSNormal....");

            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("GPS信号正常");
                }
            });
        }

        /////////////////////////////////////////////////

        /**
         * 导航机动点信息指的是根据用户当前GPS定位点生成的引导用户到下一驾驶操作点的信息
         */
        @Override
        public void onManeuver(BNRGManeuver arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onManeuver...........name = " + arg0.getManeuverName()
                    + ", distance = " + arg0.getManeuverDistance() + ",nextRoadName = " + arg0.getNextRoadName());

            final String afterMeterS = getFormatAfterMeters(arg0.getManeuverDistance());
            final String nextRoadName = arg0.getNextRoadName();
            final boolean isAlong = arg0.mIsStraight;
            String turnName = arg0.name;
            int turnIconResId = SimpleGuideModle.gTurnIconID[0];

            if (turnName != null && !"".equalsIgnoreCase(turnName)) {
                turnName = turnName + ".png";
            }

            if (turnName != null && !"".equalsIgnoreCase(turnName)) {
                turnIconResId = SimpleGuideModle.getInstance().getTurnIconResId(turnName);
            }

            final int turnIcon = turnIconResId;

            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mAfterMetersInfoTV.setText(afterMeterS);
                    mGoWhereInfoTV.setText(nextRoadName);
                    sendMessage(nextRoadName);
                    mNaviAfterView.setVisibility(View.VISIBLE);
                    mAlongRoadView.setVisibility(View.GONE);
                    mTurnIcon.setImageDrawable(getResources().getDrawable(turnIcon));
                }
            });
        }

        /**
         * 导航状态信息主要包括导航开始、结束，导航过程中偏航、偏航结束。
         */
        @Override
        public void onNaviEnd(BNRGNaviEnd arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onNaviEnd...........");

            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("导航结束");
                }
            });
        }

        @Override
        public void onNaviStart(BNRGNaviStart arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onNaviStart...........");
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("导航开始");
                }
            });
        }

        @Override
        public void onRoutePlanYawComplete(BNRGRPYawComplete arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onRoutePlanYawComplete............");

            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("偏航算路完成");
                }
            });
        }

        @Override
        public void onRoutePlanYawing(BNRGRPYawing arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onRoutePlanYawing............");

            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText("偏航中...");
                }
            });
        }

        ////////////////////////////////////////////////////////
        @Override
        public void onNextRoad(BNRGNextRoad arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onNextRoad...........nextRoadName = " + arg0.getNextRoadName());
        }

        /**
         * 本次导航当前位置距离目的地剩下的距离及时间信息
         */
        @Override
        public void onRemainInfo(BNRGRemainInfo arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onRemainInfo.............distance = " + arg0.getRemainDistance()
                    + ", time = " + arg0.getRemainTime());

            final String remainDistance = calculateTotalRemainDistString(arg0.getRemainDistance());
            final String remainTime = calculateArriveTime(arg0.getRemainTime());
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mTotalDistTV.setText(remainDistance);
                    mArriveTimeTV.setText(remainTime);
                }
            });
        }

        /**
         * 当用户行驶到高速路段，导航展示的告诉服务区及距离服务区距离等信息
         */
        @Override
        public void onServiceArea(BNRGServiceArea arg0) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onServiceArea............name = " + arg0.getServiceAreaName()
                    + ", distance = " + arg0.getServiceAreaDistance());

            final String serviceAreaTips = getFormatAfterMeters(arg0.getServiceAreaDistance()) + " "
                    + arg0.getServiceAreaName();
            mMainHandler.post(new Runnable() {

                @Override
                public void run() {
                    mNavilogTv.setText(serviceAreaTips);
                }
            });
        }

        /**
         *
         * 路口放大图主要包括放大图数据、路名、剩余距离等信息。
         */
        @Override
        public void onEnlargeRoad(BNEnlargeRoad enlargeRoad) {
            Log.d(BNRemoteConstants.MODULE_TAG, "onEnlargeRoad......enlargeRoad = " + enlargeRoad);

            final BNEnlargeRoad enlargeInfo = enlargeRoad;
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleEnlargeRoad(enlargeInfo);
                }
            });
        }

        /**
         * 车标自由态主要是说明当前是否处于野路，即自由态
         */
        @Override
        public void onCarFreeStatus(BNRGCarFreeStatus arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onCarFreeStatus...... ");

        }

        /**
         * 车点信息主要包括车标当前经纬度、方向和速度。
         */
        @Override
        public void onCarInfo(BNRGCarInfo arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onCarInfo...... ");

        }

        /**
         * 隧道信息主要通知当前是否处于隧道中。
         */
        @Override
        public void onCarTunelInfo(BNRGCarTunelInfo arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onCarTunelInfo...... ");
        }

        @Override
        public void onCurShapeIndexUpdate(BNRGCurShapeIndexUpdate arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onCurShapeIndexUpdate...... ");
        }

        /**
         * 目的地信息包括目的地经纬度、总距离和图标ID。
         */
        @Override
        public void onDestInfo(BNRGDestInfo arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onDestInfo...... ");
        }

        /**
         * 摄像头信息向上通知车点附近所有摄像头信息，包括摄像头经纬度、类型。
         */
        @Override
        public void onNearByCamera(BNRGNearByCameraInfo arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onNearByCamera...... ");
        }

        /**
         * AR路线包括路线形状点信息、时间线信息和交规信息。
         */
        @Override
        public void onRouteInfo(BNRGRouteInfo arg0) {
            // TODO Auto-generated method stub
            Log.d(BNRemoteConstants.MODULE_TAG, "onRouteInfo...... ");
        }

    };

    /**
     * 路口放大图主要包括放大图数据、路名、剩余距离等信息。 信息处理方法
     */
    private void handleEnlargeRoad(BNEnlargeRoad enlargeRoad) {
        int enlargeType = enlargeRoad.getEnlargeRoadType();
        int enlargeState = enlargeRoad.getEnlargeRoadState();

        if (enlargeState == HUDConstants.EnlargeMapState.EXPAND_MAP_STATE_HIDE) {
            if (mEnlargeRoadMapView != null) {
                mEnlargeRoadMapView.setVisibility(View.GONE);
            }
        } else {

            if (mEnlargeRoadMapView != null) {
                if (enlargeState == HUDConstants.EnlargeMapState.EXPAND_MAP_STATE_UPDATE
                        && mEnlargeRoadMapView.getVisibility() != View.VISIBLE) {
                    return;
                }
                mEnlargeRoadMapView.setVisibility(View.VISIBLE);
            }

            mEnlargeType = enlargeType;
            if (enlargeState == HUDConstants.EnlargeMapState.EXPAND_MAP_STATE_SHOW) {
                mbUpdateRasterInfo = false;
            } else {
                mbUpdateRasterInfo = true;
            }

            Bitmap basicImage = enlargeRoad.getBasicImage();
            Bitmap arrowImage = enlargeRoad.getArrowImage();

            String roadName = enlargeRoad.getRoadName();
            mTotalDist = enlargeRoad.getTotalDist();
            mRemDist = enlargeRoad.getRemainDist();
            if (!TextUtils.isEmpty(roadName)) {
                mRoadName = roadName;
            }

            mProgress = 0;
            if (mRemDist <= 0 || mTotalDist <= 0) {
                mProgress = 100;
            } else {
                mProgress = (int) (mTotalDist - mRemDist) * 100 / mTotalDist;
            }

            if (enlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_VECTOR) {
                mCarPosX = enlargeRoad.getCarPosX();
                mCarPosY = enlargeRoad.getCarPosY();
                mCarRotate = enlargeRoad.getCarPosRotate();
                mCarRotate = -mCarRotate;
            } else if (null != mCarPosImgView) {
                mCarPosImgView.setVisibility(View.INVISIBLE);
            }

            if (enlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_DEST_STREET_VIEW) {

                if (null != bnav_rg_enlarge_image_mask) {
                    bnav_rg_enlarge_image_mask.setVisibility(View.INVISIBLE);
                }
            }

            updateEnlargeRoadView(basicImage, arrowImage);
        }
    }

    private void updateEnlargeRoadView(Bitmap baseicImage, Bitmap arrawImage) {
        if (!mbUpdateRasterInfo || mForceRefreshImage) {
            mForceRefreshImage = false;
            if (mEnlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_DIRECT_BOARD) {
                updateDirectBoardView(baseicImage, arrawImage);

            } else if (mEnlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_RASTER) {
                updateSimpleModelView(baseicImage, arrawImage);

            } else if (mEnlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_VECTOR) {
                updateVectorMapView(baseicImage, arrawImage);

            } else if (mEnlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_DEST_STREET_VIEW) {
                updateStreetView(baseicImage, arrawImage);
            }
        }
        updateProgress(baseicImage, arrawImage);
    }

    private void updateProgress(Bitmap baseicImage, Bitmap arrawImage) {
        // 更新剩余距离和进度条
        StringBuffer distance = new StringBuffer();
        StringUtils.formatDistance(mRemDist, UnitLangEnum.ZH, distance);
        mRemainDistTV.setText(distance.toString());
        mProgressBar.setProgress(mProgress);

        if (mEnlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_VECTOR) {
            updateVectorMapCarPos(baseicImage, arrawImage);
        } else if (mEnlargeType == HUDConstants.EnlargeMapType.EXPAND_MAP_DEST_STREET_VIEW) {
            if (null != mCarPosImgView) {
                mCarPosImgView.setVisibility(View.INVISIBLE);
            }
        } else if (null != mCarPosImgView) {
            mCarPosImgView.setVisibility(View.INVISIBLE);
        }
    }

    private void updateVectorMapCarPos(Bitmap baseicImage, Bitmap arrawImage) {
    }

    private synchronized void updateDirectBoardView(Bitmap baseicImage, Bitmap arrawImage) {
        if (null == mEnlargeImageView || null == mNextRoadTV) {
            return;
        }

        releaseImageView(mEnlargeImageView);
        if (!TextUtils.isEmpty(mRoadName)) {
            mNextRoadTV.setText(mRoadName);
            mNextRoadTV.setVisibility(View.VISIBLE);
        } else {
            mNextRoadTV.setVisibility(View.INVISIBLE);
        }

        if (baseicImage != null && arrawImage != null) {
            mEnlargeImageView.setImageBitmap(arrawImage);
            mEnlargeImageView.setBackgroundDrawable(new BitmapDrawable(baseicImage));
        }
        mEnlargeImageView.setVisibility(View.VISIBLE);
    }

    private void updateSimpleModelView(Bitmap baseicImage, Bitmap arrawImage) {
        if (null == mEnlargeImageView || null == mNextRoadTV) {
            return;
        }

        releaseImageView(mEnlargeImageView);

        if (!TextUtils.isEmpty(mRoadName)) {
            mNextRoadTV.setText(mRoadName);
            mNextRoadTV.setVisibility(View.VISIBLE);
        } else {
            mNextRoadTV.setVisibility(View.INVISIBLE);
        }

        if (arrawImage != null && baseicImage != null) {
            mEnlargeImageView.setImageBitmap(arrawImage);
            mEnlargeImageView.setBackgroundDrawable(new BitmapDrawable(baseicImage));
        }

        mEnlargeImageView.setVisibility(View.VISIBLE);
    }

    private void updateVectorMapView(Bitmap baseicImage, Bitmap arrawImage) {
        if (null == mEnlargeImageView || null == mNextRoadTV) {
            return;
        }

        releaseImageView(mEnlargeImageView);

        if (!TextUtils.isEmpty(mRoadName)) {
            mNextRoadTV.setText(mRoadName);
            mNextRoadTV.setVisibility(View.VISIBLE);
        } else {
            mNextRoadTV.setVisibility(View.INVISIBLE);
        }

        if (baseicImage != null) {
            mEnlargeImageView.setImageBitmap(baseicImage);
            mEnlargeImageView.setBackgroundResource(android.R.color.transparent);
        }
        mEnlargeImageView.setVisibility(View.VISIBLE);
    }

    private void updateStreetView(Bitmap baseicImage, Bitmap arrawImage) {
        if (null == mEnlargeImageView || null == mNextRoadTV) {
            return;
        }
        releaseImageView(mEnlargeImageView);

        mNextRoadTV.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(mRoadName)) {
            mNextRoadTV.setText("距离" + mRoadName);
        } else {
            mNextRoadTV.setVisibility(View.INVISIBLE);
        }

        if (baseicImage != null) {
            mEnlargeImageView.setImageBitmap(baseicImage);
            mEnlargeImageView.setBackgroundResource(android.R.color.transparent);
        }

        mEnlargeImageView.setVisibility(View.VISIBLE);
    }

    /**
     * 释放 ImageView
     *
     * @param iv
     */
    public static void releaseImageView(ImageView iv) {
        if (null != iv) {
            iv.setImageBitmap(null);
            iv.setBackgroundResource(android.R.color.transparent);
            iv.setBackgroundDrawable(null);
            iv = null;
        }
    }


    private void initViews() {

        mSimpleRGLayout = findViewById(R.id.simple_route_guide);
        mNaviAfterView = findViewById(R.id.bnavi_rg_after_layout);
        mTurnIcon = (ImageView) findViewById(R.id.bnav_rg_sg_turn_icon);
        mAfterMetersInfoTV = (TextView) findViewById(R.id.bnav_rg_sg_after_meters_info);
        mAfterLabelInfoTV = (TextView) findViewById(R.id.bnav_rg_sg_after_label_info);
        mGoLabelTV = (TextView) findViewById(R.id.bnav_rg_sg_go_label_tv);
        mGoWhereInfoTV = (TextView) findViewById(R.id.bnav_rg_sg_go_where_info);

        mAlongRoadView = findViewById(R.id.bnav_rg_sg_along_layout);
        mCurRoadNameTV = (TextView) findViewById(R.id.bnav_rg_sg_cur_road_name_tv);
        mCurRoadRemainDistTV = (TextView) findViewById(R.id.bnav_rg_sg_cur_road_remain_dist_tv);

        mNewCurRoadTv = (TextView) findViewById(R.id.cur_road_name_tv);

        mRemainInfoLayout = findViewById(R.id.remain_info);
        mTotalDistTV = (TextView) findViewById(R.id.bnav_rg_cp_total_dist);
        mArriveTimeTV = (TextView) findViewById(R.id.bnav_rg_cp_arrive_time);

        mNavilogTv = (TextView) findViewById(R.id.hud_log_tv);
        mServerIPTv = (EditText) findViewById(R.id.server_ip_tv);
        mConnectBtn = (Button) findViewById(R.id.connect_btn);
        mCloseBtn = (Button) findViewById(R.id.close_btn);

        mConnectBtn.setClickable(false);
        mCloseBtn.setClickable(false);

        mEnlargeRoadMapView = findViewById(R.id.bnav_rg_enlarge_road_map);
        mEnlargeImageView = (ImageView) findViewById(R.id.bnav_rg_enlarge_image);

        mRemainDistTV = (TextView) findViewById(R.id.bnav_rg_enlarge_remain_dist);
        mNextRoadTV = (TextView) findViewById(R.id.bnav_rg_enlarge_next_road);
        mProgressBar = (ProgressBar) findViewById(R.id.bnav_rg_enlarge_progress);

        mCarPosLayout = findViewById(R.id.bnav_rg_enlarge_carpos_layout);
        mCarPosImgView = (ImageView) findViewById(R.id.bnav_rg_enlarge_carpos_image);

        bnav_rg_enlarge_image_mask = findViewById(R.id.bnav_rg_enlarge_image_mask);

        mConnectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                String ip = GetIp();
                mProgressDialog = ProgressDialog.show(MainActivity.this, "", "正在连接中...");
                BNRemoteVistor.getInstance().setServerIPAddr(ip);
                BNRemoteVistor.getInstance().open();
            }
        });

        mCloseBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                BNRemoteVistor.getInstance().close(OnConnectCallback.CLOSE_NORMAL, "user closed");
                ;
            }
        });
    }

    public String GetIp() {
        // 获取wifi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = intToIp(ipAddress);
        return ip;
    }

    private String intToIp(int i) {

        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }

    private String getAppVersionName(Context context, String appName) {
        String versionName = "";
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(appName, 0);
            versionName = packageInfo.versionName;
            if (TextUtils.isEmpty(versionName)) {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 根据剩余距离获取格式化的字符串，如 200米后
     *
     * @param nextRemainDist
     * @return
     */
    private String getFormatAfterMeters(int nextRemainDist) {
        StringBuffer distance = new StringBuffer();
        StringUtils.formatDistance(nextRemainDist, UnitLangEnum.ZH, distance);
        return getResources().getString(R.string.nsdk_string_rg_sg_after_meters, distance);
    }

    private String calculateTotalRemainDistString(int nDist) {
        StringBuffer builder = new StringBuffer();
        StringUtils.formatDistance(nDist, UnitLangEnum.ZH, builder);
        String totalRemainDistS = builder.toString();

        return totalRemainDistS;
    }

    private String calculateArriveTime(int remainTime) {
        long mArriveTime = System.currentTimeMillis();
        Date curDate = new Date(mArriveTime);
        mArriveTime += (remainTime * 1000);
        Date arriveDate = new Date(mArriveTime);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String mArriveTimeS = sdf.format(arriveDate);

        GregorianCalendar curCal = new GregorianCalendar();
        curCal.setTime(curDate);
        GregorianCalendar arriveCal = new GregorianCalendar();
        arriveCal.setTime(arriveDate);

        if (curCal.get(GregorianCalendar.DAY_OF_MONTH) == arriveCal.get(GregorianCalendar.DAY_OF_MONTH)) {
            if (0 == arriveCal.get(GregorianCalendar.HOUR_OF_DAY)) {
                mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time_at_wee), mArriveTimeS);
            } else {
                mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time), mArriveTimeS);
            }
        } else {
            int interval = getIntervalDays(curDate, arriveDate);
            if (interval == 1) {
                if (arriveCal.get(GregorianCalendar.HOUR_OF_DAY) >= 0
                        && arriveCal.get(GregorianCalendar.HOUR_OF_DAY) < 4) {
                    mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time),
                            getString(R.string.nsdk_string_rg_wee_hours));
                } else {
                    mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time),
                            getString(R.string.nsdk_string_rg_tomorrow));
                }
            } else if (interval == 2) {
                mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time),
                        getString(R.string.nsdk_string_rg_the_day_after_tomorrow));
            } else if (interval > 2) {
                mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time_after_day), "" + interval);
            } else {
                mArriveTimeS = String.format(getString(R.string.nsdk_string_rg_arrive_time), mArriveTimeS);
            }
        }

        return mArriveTimeS;
    }

    /**
     * 两个日期之间相隔的天数
     *
     * @param fDate
     * @param oDate
     * @return
     */
    private static int getIntervalDays(Date fDate, Date oDate) {
        if (null == fDate || null == oDate) {
            return 0;
        }

        long intervalMilli = oDate.getTime() - fDate.getTime();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            fDate = (Date) sdf.parse(sdf.format(fDate));
            oDate = (Date) sdf.parse(sdf.format(oDate));
            intervalMilli = oDate.getTime() - fDate.getTime();
        } catch (Exception e) {

        }

        return (int) (intervalMilli / (24 * 60 * 60 * 1000));

    }

}
