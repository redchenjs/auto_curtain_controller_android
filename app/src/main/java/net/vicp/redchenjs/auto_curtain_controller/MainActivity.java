package net.vicp.redchenjs.auto_curtain_controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    TextView textViewStatus = null;
    ToggleButton toggleButtonStatus = null;
    ToggleButton toggleButtonMode   = null;
    EditText editTextLux = null;
    EditText editTextPos = null;
    Button buttonLux = null;
    Button buttonPos = null;
    SeekBar seekBar = null;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING   = 1;
    public static final int STATE_CONNECTED    = 2;

    BluetoothAdapter btAdapter = null;
    BluetoothDevice btDevice  = null;
    BluetoothSocket btSocket  = null;
    int btState = STATE_DISCONNECTED;
    OutputStream outStream = null;
    String btAddress = "20:16:11:21:11:02";
    UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btState = STATE_DISCONNECTED;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        toggleButtonStatus = (ToggleButton) findViewById(R.id.toggleButtonStatus);
        toggleButtonMode   = (ToggleButton) findViewById(R.id.toggleButtonMode);
        editTextLux = (EditText) findViewById(R.id.editTextLux);
        editTextPos = (EditText) findViewById(R.id.editTextPos);
        buttonLux = (Button) findViewById(R.id.buttonLux);
        buttonPos = (Button) findViewById(R.id.buttonPos);
        seekBar = (SeekBar) findViewById(R.id.seekBarPos);

        toggleButtonStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toggleButtonStatus.isChecked()) {
                    btWriteData("set pos 0\n");
                }
                else {
                    btWriteData("set pos 100\n");
                }
                toggleButtonMode.setChecked(false);
            }
        });

        toggleButtonMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toggleButtonMode.isChecked()) {
                    btWriteData("set mod auto\n");
                }
                else {
                    btWriteData("set mod manual\n");
                }
            }
        });

        editTextLux.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0 || Integer.valueOf(s.toString()) < 0) {
                    editTextLux.setText("0");
                }
                else if (Integer.valueOf(s.toString()) > 65535) {
                    editTextLux.setText("65535");
                }
            }
        });

        editTextPos.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0 || Integer.valueOf(s.toString()) < 0) {
                    editTextPos.setText("0");
                }
                else if (Integer.valueOf(s.toString()) > 100) {
                    editTextPos.setText("100");
                }
                seekBar.setProgress(Integer.valueOf(editTextPos.getText().toString()));
            }
        });

        buttonLux.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lux = editTextLux.getText().toString();
                btWriteData("set lux " + lux + "\n");
            }
        });

        buttonPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pos = editTextPos.getText().toString();
                btWriteData("set pos " + pos + "\n");
                toggleButtonMode.setChecked(false);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editTextPos.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String prog = Integer.toString(seekBar.getProgress());
                btWriteData("set pos " + prog + "\n");
                toggleButtonMode.setChecked(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connection, menu);
        final MenuItem btConnection = menu.findItem(R.id.menu_connect);
        if (btConnection != null) {
            btConnection.setIcon((btState == STATE_CONNECTED) ? R.drawable.ic_action_device_bluetooth_connected :
                                                                R.drawable.ic_action_device_bluetooth);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if (btState == STATE_DISCONNECTED) {
                    if (btConnect() == STATE_CONNECTED) {
                        item.setTitle(R.string.menu_disconnect);
                        textViewStatus.setText(R.string.textViewConnected);
                    }
                }
                else if (btState == STATE_CONNECTED) {
                    if (btDisconnect() == STATE_DISCONNECTED) {
                        item.setTitle(R.string.menu_connect);
                        textViewStatus.setText(R.string.textViewDisconnected);
                    }
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showAlertDialog(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.alert_dialog_tittle));
        alertDialogBuilder.setMessage(message);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public int btConnect() {

        if (!btAdapter.isEnabled()) {
            showAlertDialog(getString(R.string.bluetooth_not_enabled));
            return STATE_DISCONNECTED;
        }
        if (btAdapter == null) {
            showAlertDialog(getString(R.string.bluetooth_not_support));
            return STATE_DISCONNECTED;
        }

        btDevice = btAdapter.getRemoteDevice(btAddress);
        btAdapter.cancelDiscovery();

        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(btUUID);
            btSocket.connect();
            btState = STATE_CONNECTING;
        } catch (IOException e) {
            btState = STATE_DISCONNECTED;
            try {
                btSocket.close();
            } catch (IOException e2) {
                return STATE_DISCONNECTED;
            }
        }
        btState = STATE_CONNECTED;
        return STATE_CONNECTED;
    }

    public void btWriteData(String data) {
        if (btState == STATE_CONNECTED) {
            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                showAlertDialog(getString(R.string.bluetooth_get_handle_error));
            }

            byte[] txBuff = data.getBytes();

            try {
                outStream.write(txBuff);
            } catch (IOException e) {
                showAlertDialog(getString(R.string.bluetooth_send_error));
            }
        }
        else {
            showAlertDialog(getString(R.string.bluetooth_not_connected));
        }
    }

    public int btDisconnect() {
        if (btState == STATE_CONNECTED) {
            try {
                btSocket.close();
                btState = STATE_DISCONNECTED;
            } catch (IOException e) {
                return STATE_CONNECTED;
            }
            return STATE_DISCONNECTED;
        }
        return STATE_CONNECTED;
    }
}
