package com.skobbler.sdkdemo.smartbicycle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.skobbler.sdkdemo.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class lights extends Activity
{
    TextView myLabel;
    TextView statuslights;
    TextView stats;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    private Switch mySwitch1;
    private Switch mySwitch2;
    volatile boolean stopWorker;
    public static final String Status = "stati";
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);
       // ActionBar actionBar = getSupportActionBar();
// Enabling Up / Back navigation
        //actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.GRAY));
        statuslights = (TextView)findViewById(R.id.statuslights);
        stats = (TextView)findViewById(R.id.stats);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
       final SharedPreferences.Editor editor = pref.edit();
        if (pref.contains(Status)) {
            stats.setText(pref.getString(Status,""));
        }
     //   Boolean status;
        Button openButton = (Button)findViewById(R.id.open);
// Button sendButton = (Button)findViewById(R.id.send);

        Button closeButton = (Button)findViewById(R.id.close);
        Button onButton = (Button)findViewById(R.id.onButton);
        Button offButton = (Button)findViewById(R.id.offButton);
        myLabel = (TextView)findViewById(R.id.label);


// myTextbox = (EditText)findViewById(R.id.entry);
//Open Button
      openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });
        openButton.setOnClickListener(new View.OnClickListener()
    {
        public void onClick(View v)
        {
            try
            {
                findBT();
                openBT();
            }
            catch (IOException ex) { }
        }
    });
//Send Button
/*sendButton.setOnClickListener(new View.OnClickListener()
{
public void onClick(View v)
{
try
{
sendData();
}
catch (IOException ex) { }
}
});*/
/*
        mySwitch1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                    try {
                        onButton();
                    } catch (Exception e) {
// TODO: handle exception
                    }
                }else{
                    try {
                    offButton();
                } catch (Exception e) {
// TODO: handle exception
                }

                }

            }
        });

        mySwitch2.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                    try
                    {
                        findBT();
                        openBT();
                    }
                    catch (IOException ex) { }
                }else{
                    try
                    {
                        closeBT();
                    }
                    catch (IOException ex) { }

                }

            }
        });

*/


//ON SWITCH
        onButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {


                    onButton();
                    editor.putString(Status, "ON");
                    editor.commit();
                    stats.setText("ON");
                //    closeBT();
                } catch (Exception e) {
// TODO: handle exception
                }
            }
        });
//OFF SWITCH
        offButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {

                    offButton();
                    editor.putString(Status,"OFF");
                    editor.commit();
                    stats.setText("OFF");
                  //  closeBT();
                } catch (Exception e) {
// TODO: handle exception
                }
            }
        });


//Close button

        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) {
                return;}
            }
        });

    }
/*
    @Override
    public void onBackPressed() {
// do something on back.
        Intent i=new Intent(getApplicationContext(),jason.smartbicycle.MainActivity.class);
        startActivity(i);
        super.onBackPressed();
    }
*/


    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set <BluetoothDevice>pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) //this name have to be replaced with your bluetooth device name
                {
                    mmDevice = device;
                    Log.v("ArduinoBT", "findBT found device named " + mmDevice.getName());
                    Log.v("ArduinoBT", "device address is " + mmDevice.getAddress());
                    break;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
    }
    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();
        myLabel.setText("Bluetooth Opened");
    }
    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            myLabel.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }
    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "";
//mmOutputStream.write(msg.getBytes());
//mmOutputStream.write(msg.getBytes());
//mmOutputStream.flush();
//mmOutputStream.close();
//mmSocket.close();
        myLabel.setText("Data Sent"+msg);
    }
    void onButton() throws IOException
    {
        mmOutputStream.write("99".getBytes());
    }
    void offButton() throws IOException
    {
        mmOutputStream.write("100".getBytes());
    }
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
    /*
    @Override
    public void onBackPressed() {
        try {
            closeBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    }
