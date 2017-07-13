package nexuslink.charon.smartlamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Main2Activity extends AppCompatActivity implements View.OnClickListener {
    private Handler mHandler;
    private Toolbar mToolbar;
    private Button mode1,mode2,mode3,mode4;

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int DISCONNECTED = 0;
    private static final int CONNECTED = 1;
    private static final int CONNECTING = 2;
    private int connectFlag = 0;
    public BluetoothManager mBluetoothManager;//蓝牙管理器
    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    private BluetoothGatt mBluetoothGatt;
    private BroadcastReceiver mBroadcastReceiver;
    private BluetoothLeService mBluetoothLeService = new BluetoothLeService();
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private String bleName,bleAddress;
    @Override
    protected void onResume() {
        super.onResume();
        initService();
        registerReceiver(mGattUpdateReceiver,makeGattUpdateIntentFilter());
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout2);


        initDate();

        initView();
        initBle();


    }

    private void initDate() {
        final Intent intent = getIntent();
        bleName = intent.getStringExtra("Name");
        bleAddress = intent.getStringExtra("Address");
    }
    private void initService() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean bll = bindService(gattServiceIntent, mServiceConnection,//2
                BIND_AUTO_CREATE);
        if (bll) {
            Log.d("123", "绑定成功");
        } else {
            Log.d("123", "绑定失败");
        }
    }
    private void initView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(bleName);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mode1 = (Button) findViewById(R.id.mode1);
        mode2 = (Button) findViewById(R.id.mode2);
        mode3 = (Button) findViewById(R.id.mode3);
        mode4 = (Button) findViewById(R.id.mode4);

        mode1.setOnClickListener(this);
        mode2.setOnClickListener(this);
        mode3.setOnClickListener(this);
        mode4.setOnClickListener(this);

    }

    private void initBle() {
        //初始化蓝牙
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //打开蓝牙
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        mHandler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.control, menu);
        if (connectFlag == DISCONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else if (connectFlag == CONNECTING) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        } else if (connectFlag == CONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (connectFlag) {
            case DISCONNECTED:
                //连接
                //isClickDisconnect = false;
                connectFlag = CONNECTING;
                invalidateOptionsMenu();
                mBluetoothLeService.connect(bleAddress);
                break;
            case CONNECTING:
                connectFlag = DISCONNECTED;
                invalidateOptionsMenu();
                mBluetoothLeService.disconnect();
            case CONNECTED:
                //断开
                //isClickDisconnect = true;
                connectFlag = DISCONNECTED;
                invalidateOptionsMenu();
                mBluetoothLeService.disconnect();
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED );
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED );
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED );
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("123", "广播为" + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connectFlag = CONNECTED;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                //displayConnectState("未连接");
                //加入是否可以点击

//                Log.d("123", isClickDisconnect+"isClickDisconnect");
//                if (!isClickDisconnect){
//                    vibrator.vibrate(new long[]{100,2000,500,2500},-1);
//                    mp.start();
//                }
                connectFlag = DISCONNECTED;
                invalidateOptionsMenu();
//                closeUi();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // 搜索需要的uuid
                Log.d("123", "发现新设备1");
                initGattCharacteristics(mBluetoothLeService
                        .getSupportedGattServices());
//                writeDate(true);
                Log.d("123", "发现新设备2");
                Toast.makeText(Main2Activity.this, "发现新services", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.READ_RSSI.equals(action)) {

            }
        }
    };


    public void initGattCharacteristics(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
            }
            mGattCharacteristics.add(charas);
        }
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {//2

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            Log.d("123", "LeService" + mBluetoothLeService.toString());
            if (!mBluetoothLeService.initialize()) {//3
                Log.e("123", "Unable to initialize Bluetooth");
                finish();
            } else Log.d("123", "能初始化");
            // 自动连接to the device upon successful start-up
            // 初始化.
            /*int currentNum = mViewPager.getCurrentItem();
            int currentBleNum = currentNum + 1;
            mBluetoothLeService.connect(spre.getString("Address"+currentBleNum,"error"));//4*/
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("123", "没有连接");
            mBluetoothLeService = null;
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    public void onClick(View v) {
        Log.d("123", "点击了button");
        switch (v.getId()) {
            case R.id.mode1:
                Log.d("123", "点击了button1");
                writeDate(1);
                break;
            case R.id.mode2:
                writeDate(2);
                break;
            case R.id.mode3:
                writeDate(3);
                break;
            case R.id.mode4:
                writeDate(4);
                break;

        }
    }
    private void writeDate(final int mode ) {
        Log.d("123", mode + "");
        BluetoothGattCharacteristic characteristic;
        Log.d("123", "1");
        if (mGattCharacteristics != null) {
            Log.d("123", "2");
            for (int i = 0; i < mGattCharacteristics.size(); i++) {
                Log.d("123", "3");
                for (int j = 0; j < mGattCharacteristics.get(i).size(); j++) {
                    Log.d("123", "4");
                    if (mGattCharacteristics.get(i).get(j).getUuid().toString().equals("0000ffe1-0000-1000-8000-00805f9b34fb")) {//对应的uuid  92BF01A5-0681-453A-8016-D44DD3E7100B   0000fff1-0000-1000-8000-00805f9b34fb
                        Log.d("123", "即将发送");
                        characteristic = mGattCharacteristics.get(i).get(j);
                        switch (mode) {
                            case 1:
                                Toast.makeText(mBluetoothLeService, "发送成功1", Toast.LENGTH_SHORT).show();
                                write(characteristic,new byte[]{0x01});//打开
                                break;
                            case 2:
                                Toast.makeText(mBluetoothLeService, "发送成功2", Toast.LENGTH_SHORT).show();
                                write(characteristic,new byte[]{0x02});//调亮度
                                break;
                            case 3:
                                Toast.makeText(mBluetoothLeService, "发送成功3", Toast.LENGTH_SHORT).show();
                                write(characteristic,new byte[]{0x03});//写入的数据
                                break;
                            case 4:
                                Toast.makeText(mBluetoothLeService, "发送成功4", Toast.LENGTH_SHORT).show();
                                write(characteristic,new byte[]{0x04});//写入的数据
                                break;
                        }
                        mBluetoothLeService.writeCharacteristic(characteristic);
                        Log.d("123", "发送数据成功");
                    } else Log.d("123", "发送失败");
                }
            }

        } else Log.d("123", "发送数据失败");
    }
    private void write(BluetoothGattCharacteristic characteristic, byte byteArray[]) {
        characteristic.setValue(byteArray);
    }

    private void write(BluetoothGattCharacteristic characteristic, String string) {
        characteristic.setValue(string);
    }
}
