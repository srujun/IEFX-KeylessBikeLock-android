package com.srujun.iefx.app;

import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.srujun.iefx.app.utils.BlendMicroGattAttributes;

import java.util.*;
import java.util.concurrent.locks.Lock;

public class MainLockActivity extends Activity {
    public static final int REQUEST_CODE = 30;
    private static final int REQUEST_ENABLE_BT = 1;
    private final static String TAG = MainLockActivity.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private LockService lockService;
    private BluetoothGattCharacteristic characteristicTx = null;

    public static List<BluetoothDevice> device = new ArrayList<BluetoothDevice>();
    private String deviceName;
    private String deviceAddress;

    private Button lockButton;
    private Button connectLockButton;
    private TextView connectionStatusText;

    private boolean isBluetoothSupported = true;
    private boolean isConnected = false;

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        {
            Log.i(TAG, "Inside serviceConnection...");
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "Trying to initialize lockService.");
            lockService = ((LockService.LocalBinder) service).getService();
            if (!lockService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.i(TAG, "onServiceConnected is done!");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "lockService is now disconnected.");
            lockService = null;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (LockService.ACTION_GATT_CONNECTED.equals(action)) {
				isConnected = true;

				Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                updateConnectionStatus(deviceAddress);
				startReadRssi();
			} else if (LockService.ACTION_GATT_DISCONNECTED.equals(action)) {
				isConnected = false;

				Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
				updateConnectionStatus("");
			} else if (LockService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected & Discovered", Toast.LENGTH_SHORT).show();

                getGattService(lockService.getSupportedGattService());
            }
		}
	};

	private void startReadRssi() {
		new Thread() {
			public void run() {

				while(isConnected) {
					lockService.readRssi();
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_lock);

        // Initialize the Button objects
        lockButton = (Button) findViewById(R.id.lock_button);
        connectLockButton = (Button) findViewById(R.id.connect_to_lock_button);
        connectionStatusText = (TextView) findViewById(R.id.connection_status_text);

        // Set up the Status TextView
        updateConnectionStatus("");

        // Check for Bluetooth LE Support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            isBluetoothSupported = false;
            Toast.makeText(this, "Bluetooth LE not supported.", Toast.LENGTH_SHORT).show();
        }

        // Check for Bluetooth Support
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null) {
            isBluetoothSupported = false;
            Toast.makeText(this, "Bluetooth not supported.", Toast.LENGTH_SHORT).show();
        }

        // Add click listeners to buttons if Bluetooth is supported.
        if(isBluetoothSupported) {
            lockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    byte[] buf = new byte[] { (byte) 0x01};
                    characteristicTx.setValue(buf);
                    lockService.writeCharacteristic(characteristicTx);
                }
            });

            connectLockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isConnected) {
                        lockService.disconnect();
                        lockService.close();
                        updateConnectionStatus("");
                    }

                    Log.i(TAG, "Trying scanLeDevice()");
                    scanLeDevice();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
                    startActivityForResult(intent, REQUEST_CODE);
                }
            });
        }

        Log.i(TAG, "gattServiceIntent stuff...");
        Intent gattServiceIntent = new Intent(this, LockService.class);
        this.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "gattServiceIntent stuff done!");
    }

    private void scanLeDevice() {
        new Thread() {
            @Override
            public void run() {
                bluetoothAdapter.startLeScan(leScanCallback);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bluetoothAdapter.stopLeScan(leScanCallback);
            }
        }.start();
        Log.i(TAG, "Started scanLeDevice()");
    }

    private void updateConnectionStatus(String deviceAddress) {
        if(!isConnected || deviceAddress.equals("")) {
            connectionStatusText.setText(getString(R.string.status_disconnected));
            connectionStatusText.setTextColor(Color.RED);
            lockButton.setEnabled(false);
        } else {
            connectionStatusText.setText(getString(R.string.status_connected) + ": " + deviceAddress);
            connectionStatusText.setTextColor(Color.GREEN);
            lockButton.setEnabled(true);
        }
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        startReadRssi();

        characteristicTx = gattService.getCharacteristic(LockService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(LockService.UUID_BLE_SHIELD_RX);
        lockService.setCharacteristicNotification(characteristicRx, true);
        lockService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(LockService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(LockService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(LockService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(LockService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(LockService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device != null) {
                        if (MainLockActivity.device.indexOf(device) == -1)
                            MainLockActivity.device.add(device);
                    }
                }
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else if (requestCode == REQUEST_CODE && resultCode == DeviceScanActivity.RESULT_CODE) {
            deviceAddress = data.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS);
            deviceName = data.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_NAME);
            Log.i(TAG, "Got from intent: " + deviceName + ", " + deviceAddress);
            Log.i(TAG, "Is lockService null? " + (lockService == null));
            lockService.connect(deviceAddress);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null)
            unbindService(serviceConnection);
        System.exit(0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_lock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
