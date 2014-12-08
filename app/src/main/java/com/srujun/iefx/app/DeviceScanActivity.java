package com.srujun.iefx.app;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DeviceScanActivity extends Activity implements AdapterView.OnItemClickListener {

    private ArrayList<BluetoothDevice> devices;
    private List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();
    private SimpleAdapter adapter;
    private Map<String, String> map = null;
    private ListView deviceScanListView;
    private String DEVICE_NAME = "name";
    private String DEVICE_ADDRESS = "address";
    public static final int RESULT_CODE = 31;
    public final static String EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS";
    public final static String EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.title_activity_device_scan);

        deviceScanListView = (ListView) findViewById(R.id.device_scan_list);

        devices = (ArrayList<BluetoothDevice>) MainLockActivity.device;
        for (BluetoothDevice device : devices) {
            map = new HashMap<String, String>();
            map.put(DEVICE_NAME, device.getName());
            map.put(DEVICE_ADDRESS, device.getAddress());
            listItems.add(map);
        }

        adapter = new SimpleAdapter(getApplicationContext(), listItems,
                R.layout.device_scan_list, new String[] { "name", "address" },
                new int[] { R.id.list_device_name, R.id.list_device_address });
        deviceScanListView.setAdapter(adapter);
        deviceScanListView.setOnItemClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        HashMap<String, String> hashMap = (HashMap<String, String>) listItems.get(position);
        String addr = hashMap.get(DEVICE_ADDRESS);
        String name = hashMap.get(DEVICE_NAME);
        System.out.println("Intent data: " + name + ", " + addr);

        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, addr);
        intent.putExtra(EXTRA_DEVICE_NAME, name);
        setResult(RESULT_CODE, intent);
        finish();
    }
}
