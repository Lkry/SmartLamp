package nexuslink.charon.smartlamp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences spre;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 200;
    private static final int REQUEST_ENABLE_BT = 0;
    private Button button1;
    private RelativeLayout relativeLayout;
    private TextView name;
    private BluetoothAdapter mBluetoothAdapter;
    SharedPreferences mySharedPreferences ;

    private BluetoothLeService mBluetoothLeService = new BluetoothLeService();


    @Override
    protected void onResume() {
        super.onResume();
        name.setText(spre.getString("Name","暂未添加"));
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        spre= getSharedPreferences("myPref", MODE_PRIVATE);

        name = (TextView) findViewById(R.id.layout_blename);




        mySharedPreferences = getSharedPreferences("test", Activity.MODE_PRIVATE);
        //初始化蓝牙
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //打开蓝牙
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Toast.makeText(MainActivity.this, "请确定蓝牙您的蓝牙已打开", Toast.LENGTH_SHORT).show();

                if (!name.getText().equals("暂未添加")){
                    Intent intent = new Intent();
                    Bundle bundle1 = new Bundle();
                    bundle1.putString("Name", spre.getString("Name", ""));
                    bundle1.putString("Address", spre.getString("Address", ""));
                    intent.putExtras(bundle1);
                    intent.setClass(MainActivity.this, Main2Activity.class);
                    startActivity(intent);
                }
            }
        });
        relativeLayout = (RelativeLayout) findViewById(R.id.layout_search);
        relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        initPermission();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_CONTACTS)) {
                showMessageOKCancel("你必须允许这个权限，否则无法搜索到BLE设备", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                return;
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("确定", okListener)
                .setNegativeButton("关闭", okListener)
                .create()
                .show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
            } else {
                //这里进行权限被拒绝的处理
                Toast.makeText(MainActivity.this, "请开启位置权限", Toast.LENGTH_SHORT).show();
                Intent i = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");

                String pkg = "com.android.settings";
                String cls = "com.android.settings.applications.InstalledAppDetails";

                i.setComponent(new ComponentName(pkg, cls));
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入");    //设置对话框标题
        final EditText edit = new EditText(this);
        builder.setView(edit);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "你输入的是: " + edit.getText().toString(), Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putString("address",edit.getText().toString()).apply();
                Log.d("123",edit.getText().toString());
                if (!mBluetoothLeService.connect(edit.getText().toString())) {
                    Toast.makeText(mBluetoothLeService, "未能成功连接", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "你点了取消", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }



}

