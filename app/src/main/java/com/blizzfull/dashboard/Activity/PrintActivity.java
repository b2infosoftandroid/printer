package com.blizzfull.dashboard.Activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import com.blizzfull.dashboard.R;

import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.dialogBluetooth;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.isBluetoothHeadsetConnected;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mDevice;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mOutputStream;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mSocket;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.printBitmap;


public class PrintActivity extends AppCompatActivity {


    ImageView imgLogo;
    Button btnPrint;

    Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        mContext=PrintActivity.this;
        imgLogo = findViewById(R.id.imgLogo);
        btnPrint = findViewById(R.id.btnPrint);
       /* if (!isBluetoothHeadsetConnected()) {
            enableBluetooth();
            dialogBluetooth(mContext);
        }*/
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            printImage();
            }
        });


    }


    public void printImage() {

        if (isBluetoothHeadsetConnected()) {
            if (mDevice == null || mSocket == null || mOutputStream == null) {
                dialogBluetooth(mContext);
            }else {

                Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.logo);
                printBitmap(icon);

            }

        } else {

            showAlert(mContext, "Please enable your device  bluetooth");
            enableBluetooth();
            dialogBluetooth(mContext);

        }

    }
    public boolean enableBluetooth() {
        try {
            BluetoothAdapter badapter = BluetoothAdapter.getDefaultAdapter();
            if (badapter != null) {
                badapter.enable();
                if (!badapter.isEnabled()) {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, 0);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    public static void showAlert( Context context,String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setMessage(message).setTitle(context.getString(R.string.bluetooth_connection))
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });

        if (!((Activity) context).isFinishing()) {
            android.app.AlertDialog alert = builder.create();
            alert.show();
        }

    }

}
