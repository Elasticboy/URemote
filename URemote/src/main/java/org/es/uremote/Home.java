package org.es.uremote;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import org.es.uremote.components.ActionArrayAdapter;
import org.es.uremote.computer.ServerListActivity;
import org.es.uremote.device.ServerSetting;
import org.es.uremote.objects.ActionItem;

import java.util.ArrayList;
import java.util.List;

import static android.view.HapticFeedbackConstants.VIRTUAL_KEY;
import static android.widget.Toast.LENGTH_SHORT;
import static org.es.uremote.utils.IntentKeys.ACTION_SELECT;
import static org.es.uremote.utils.IntentKeys.EXTRA_SERVER_DATA;

/**
 * The dashboard class that leads everywhere in the application.
 *
 * @author Cyril Leroux
 *         Created on 11/09/10.
 */
public class Home extends ListActivity implements OnItemClickListener {

    // The request codes of ActivityForResults
    private static final int RC_SELECT_SERVER = 0;
    private static final int RC_ENABLE_BT     = 1;
    private static final int RC_ENABLE_WIFI   = 2;

    private static final int ACTION_COMPUTER = 0;
    private static final int ACTION_NAO      = 1;
    private static final int ACTION_LIGHTS   = 2;
    private static final int ACTION_TV       = 3;
    private static final int ACTION_ROBOTS   = 4;
    private static final int ACTION_HIFI     = 5;

    private List<ActionItem> mActionList;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final Typeface typeface = Typeface.createFromAsset(getAssets(), getString(R.string.action_title_font));
        initActionList();

        final ActionArrayAdapter adapter = new ActionArrayAdapter(getApplicationContext(), mActionList, typeface);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
    }

    private void initActionList() {
        if (mActionList != null) {
            return;
        }
        mActionList = new ArrayList<>(6);
        mActionList.add(ACTION_COMPUTER, new ActionItem(getString(R.string.title_computer), R.drawable.home_computer));
        mActionList.add(ACTION_NAO, new ActionItem(getString(R.string.title_nao), R.drawable.home_nao));
        mActionList.add(ACTION_LIGHTS, new ActionItem(getString(R.string.title_lights), R.drawable.home_light));
        mActionList.add(ACTION_TV, new ActionItem(getString(R.string.title_tv), R.drawable.home_tv));
        mActionList.add(ACTION_ROBOTS, new ActionItem(getString(R.string.title_robots), R.drawable.home_robot));
        mActionList.add(ACTION_HIFI, new ActionItem(getString(R.string.title_hifi), R.drawable.home_hifi));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.performHapticFeedback(VIRTUAL_KEY);

        switch (position) {

            case ACTION_COMPUTER:
                startActivityForResult(new Intent(getApplicationContext(), ServerListActivity.class).setAction(ACTION_SELECT), RC_SELECT_SERVER);
                //                serverDialog();
                break;

            case ACTION_NAO:
                startNaoActivity();
                break;

            case ACTION_LIGHTS:
                Toast.makeText(Home.this, getString(R.string.msg_light_control_not_available), Toast.LENGTH_SHORT).show();
                break;

            case ACTION_ROBOTS:
                startRobotControl();
                break;

            case ACTION_TV:
                startActivity(new Intent(getApplicationContext(), TvDialer.class));
                break;

            case ACTION_HIFI:
                Toast.makeText(Home.this, getString(R.string.msg_hifi_control_not_available), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), HexHome.class));
                break;

            default:
                break;
        }
    }

    //    private  void serverDialog() {
    //        FragmentManager fragmentManager = getC getSupportFragmentManager();
    //        ServerListDialogFragment fragment = new CustomDialogFragment();
    //
    //        // 1. Instantiate an AlertDialog.Builder with its constructor
    //        AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
    //
    //        // 2. Chain together various setter methods to set the dialog characteristics
    //        builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
    //
    //        // 3. Get the AlertDialog from create()
    //        AlertDialog dialog = builder.create();
    //    }

    private void startComputerRemote(final ServerSetting server) {
        // If Wifi is disabled, ask for activation
        final WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiMgr.isWifiEnabled()) {
            //Intent enableIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            Toast.makeText(Home.this, "Wifi is not enable. You are using 3G remote control.", Toast.LENGTH_SHORT).show();
            //startActivityForResult(enableIntent, RC_ENABLE_WIFI);
        } else {

            final Intent computerIntent = new Intent(getApplicationContext(), Computer.class);
            computerIntent.putExtra(EXTRA_SERVER_DATA, server);
            startActivity(computerIntent);
        }
    }

    private void startRobotControl() {

        BluetoothAdapter bluetoothAdapter = null;

        // TODO update for backward compatibility
        //if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //} else {
        //	BluetoothManager manager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        //	bluetoothAdapter = manager.getAdapter();
        //}

        // If Bluetooth is disabled, ask for activation
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, RC_ENABLE_BT);
        } else {
            startActivity(new Intent(getApplicationContext(), RobotControl.class));
        }
    }

    private void startNaoActivity() {
        startActivity(new Intent(getApplicationContext(), Nao.class));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case RC_SELECT_SERVER:
                if (resultCode == Activity.RESULT_OK) {
                    final ServerSetting server = data.getParcelableExtra(EXTRA_SERVER_DATA);
                    if (server == null) {
                        Toast.makeText(getApplicationContext(), R.string.no_server_configured, LENGTH_SHORT).show();
                        return;
                    }
                    startComputerRemote(server);
                }
                break;

            // Return from bluetooth activation
            case RC_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // Start robot control activity if bluetooth is enable
                    startActivity(new Intent(getApplicationContext(), RobotControl.class));
                }
                break;

            // Return from wifi activation
            case RC_ENABLE_WIFI:
                // Start computer control activity
                startActivity(new Intent(getApplicationContext(), Computer.class));
                break;
        }
    }
}