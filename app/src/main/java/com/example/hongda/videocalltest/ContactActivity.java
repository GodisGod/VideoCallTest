package com.example.hongda.videocalltest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.callsdk.ILVCallConfig;
import com.tencent.callsdk.ILVCallConstants;
import com.tencent.callsdk.ILVCallListener;
import com.tencent.callsdk.ILVCallManager;
import com.tencent.callsdk.ILVCallNotification;
import com.tencent.callsdk.ILVCallNotificationListener;
import com.tencent.callsdk.ILVIncomingListener;
import com.tencent.callsdk.ILVIncomingNotification;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLoginManager;

import java.util.ArrayList;

/**
 * 联系人界面
 */
public class ContactActivity extends Activity implements View.OnClickListener, ILVIncomingListener, ILVCallListener, ILVCallNotificationListener {

    private static String TAG = "LHD";
    private TextView tvMyAddr;
    private EditText etDstAddr;

    private LinearLayout callView;
    private AlertDialog mIncomingDlg;
    private int mCurIncomingId;

    private String userId = "123456";
    private String password = "123456";
    private String remoteId = "111222abc";

    //帐号类型，由腾讯分配。
    public static final int ACCOUNT_TYPE = 11754;
    //sdk appid 由腾讯分配
    public static final int SDK_APPID = 1400028127;
    public static final String sig = "eJxlz11PgzAUBuB7fgXhVmNKafnwrhBjYMOIuC1eNUi70aFQSmfQxf-uxBmbeG6f9*Q952jZtu08Lsurqq77Q6epfpfcsa9tBziXfyilYLTS1FPsH-JJCsVptdVczehijCEAZkYw3mmxFb8J6CHsGz6yls4lP4xO2zB0YWBGxG7G-OYpSYtkVGFNECrIer8gUZDjJFwRv9583MfP*7ZfMALi8CHwxI6kDckOcmBT2ZOWX5TZtFkO-ioPmwbUL6kakMxGURa3d2WcJUalFq-8fFDkAhi5EBr6xtUo*m4OQODi01Pgexzr0-oCCLVbeQ__";


    private Button btnLogin;
    private Button btnCall;


    // 内部方法
    private void initView() {
        tvMyAddr = (TextView) findViewById(R.id.tv_my_address);
        callView = (LinearLayout) findViewById(R.id.call_view);
        etDstAddr = (EditText) findViewById(R.id.et_dst_address);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnCall = (Button) findViewById(R.id.btn_make_call);
    }


    // 覆盖方法
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_main);
        //TODO 初始化随心播
//        ILiveSDK.getInstance().initSdk(getApplicationContext(), 1400016949, 8002);
        ILiveSDK.getInstance().initSdk(getApplicationContext(), SDK_APPID, ACCOUNT_TYPE);

        ILVCallManager.getInstance().init(new ILVCallConfig()
                .setNotificationListener(this)
                .setAutoBusy(true));

        initView();

        // 设置通话回调
        ILVCallManager.getInstance().addIncomingListener(this);
        ILVCallManager.getInstance().addCallListener(this);


        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginSDK(userId, sig);
            }
        });

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> nums = new ArrayList<>();
                nums.add(remoteId);
                makeCall(ILVCallConstants.CALL_TYPE_VIDEO, nums);
            }
        });
    }


    /**
     * 使用userSig登录iLiveSDK(独立模式下获有userSig直接调用登录)
     */
    private void loginSDK(final String id, String userSig) {
        ILiveLoginManager.getInstance().iLiveLogin(id, userSig, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                Log.i("LHD", "Login CallSDK success:" + id);
                tvMyAddr.setText(ILiveLoginManager.getInstance().getMyUserId());
                callView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                Toast.makeText(ContactActivity.this, "Login failed:" + module + "|" + errCode + "|" + errMsg, Toast.LENGTH_SHORT).show();
                Log.i("LHD", "Login failed:" + module + "|" + errCode + "|" + errMsg);
            }
        });
    }


    /**
     * 发起呼叫
     */
    private void makeCall(int callType, ArrayList<String> nums) {
        Intent intent = new Intent();
        intent.setClass(this, CallActivity.class);
        intent.putExtra("HostId", ILiveLoginManager.getInstance().getMyUserId());
        intent.putExtra("CallId", 0);
        intent.putExtra("CallType", callType);
        intent.putStringArrayListExtra("CallNumbers", nums);
        startActivity(intent);
    }

    private void acceptCall(int callId, String hostId, int callType) {
        Intent intent = new Intent();
        intent.setClass(ContactActivity.this, CallActivity.class);
        intent.putExtra("HostId", hostId);
        intent.putExtra("CallId", mCurIncomingId);
        intent.putExtra("CallType", callType);
        startActivity(intent);
    }

    /**
     * 回调接口 来电
     *
     * @param callId       来电ID
     * @param callType     来电类型
     * @param notification 来电通知
     */
    @Override
    public void onNewIncomingCall(final int callId, final int callType, final ILVIncomingNotification notification) {
        if (null != mIncomingDlg) {  // 关闭遗留来电对话框
            mIncomingDlg.dismiss();
        }
        mCurIncomingId = callId;
        mIncomingDlg = new AlertDialog.Builder(this)
                .setTitle("New Call From " + notification.getSender())
                .setMessage(notification.getNotifDesc())
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        acceptCall(callId, notification.getSponsorId(), callType);
                    }
                })
                .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int ret = ILVCallManager.getInstance().rejectCall(mCurIncomingId);
                    }
                })
                .create();
        mIncomingDlg.setCanceledOnTouchOutside(false);
        mIncomingDlg.show();
    }

    @Override
    public void onCallEstablish(int callId) {
        Log.i("LHD", "Call Establish :" + callId);
    }

    @Override
    public void onCallEnd(int callId, int endResult, String endInfo) {
        if (mCurIncomingId == callId) {
            mIncomingDlg.dismiss();
        }
        Log.e("LHD", "onCallEnd->id: " + callId + "|" + endResult + "|" + endInfo);
    }

    @Override
    public void onException(int iExceptionId, int errCode, String errMsg) {
        Log.i("LHD", "Exception id:" + iExceptionId + ", " + errCode + "-" + errMsg);
    }

    @Override
    public void onRecvNotification(int callid, ILVCallNotification notification) {
        Log.i("LHD", "onRecvNotification->notify id:" + notification.getNotifId() + "|" + notification.getUserInfo() + "/" + notification.getSender());
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onDestroy() {
        ILVCallManager.getInstance().removeIncomingListener(this);
        ILVCallManager.getInstance().removeCallListener(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {

    }
}
