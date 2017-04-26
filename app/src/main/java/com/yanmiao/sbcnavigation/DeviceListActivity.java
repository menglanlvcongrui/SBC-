package com.yanmiao.sbcnavigation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceListActivity extends Activity {
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加入圆形进度条
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
        // 设置关闭当前Activity的返回值
        setResult(Activity.RESULT_CANCELED);
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        // 初始化用于保存已配对的蓝牙设备的ArrayAdapter
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        // 初始化用于保存新搜索到的蓝牙设备的ArrayAdapter
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        // 注册用一个蓝牙设备被搜索到广播接收器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // 注册扫描完的接收器
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获取当前已经配对的设备
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // 如果当前已经有配对的设备，则显示
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            String noDevices = "没有可配对的蓝牙设备";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }
    private void doDiscovery()
    {
        setProgressBarIndeterminateVisibility(true);
        setTitle("正在扫描蓝牙设备...");

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if (mBtAdapter.isDiscovering())
        {
            mBtAdapter.cancelDiscovery();
        }

        mBtAdapter.startDiscovery();
    }
    // 处理搜索到的蓝牙设备以及搜索完成动作
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 没发现一个设备，会执行下面代码
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 获取BluetoothDevice对象
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //如果已经配对，则忽略设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //设备未配对，则加入数组中
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            //搜索完成时代码
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("选择一个蓝牙设备");
                //如果未搜索到任何设备，在当前界面显示提示信息
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices ="未发现蓝牙设备";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}