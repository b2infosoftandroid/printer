package com.blizzfull.dashboard.BluetoothPrinter;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.blizzfull.dashboard.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/* Shoukin Choudhary
 9166900279*/
public class BluetoothClass {
    public static UUID uuId = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static byte[] arrayOfByte1 = {27, 33, 0};

    public static byte[] format = {27, 33, 20};
    public static byte[] arrayOfByte = {27, 33, 0};
    // 0- normal size text
    public static byte[] normatfontBt = new byte[]{0x1B, 0x21, 0x00};  // 0- normal size text
    public static byte[] boldfontBt = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
    public static byte[] mediumfontBt = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
    public static byte[] largefontBt = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
    public static byte[] center = new byte[]{0x1b, 0x61, 0x01};
    public static byte[] formatLeft = {0x1B, 'a', 0x00};
    public static byte[] formatSmall = {0x1, formatLeft[2]};
    public static byte[] formatRight = {0x1B, 'a', 0x02};
    public static byte[] SELECT_BIT_IMAGE_MODE = {0x1B, 0x2A, 33, (byte) 255, 3};
    static Context mContext;
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothDevice mDevice = null;
    private static String btname = "Bluetooth";
    public static BluetoothSocket mSocket = null;
    public static OutputStream mOutputStream = null;
    public static InputStream mInputStream = null;
    public static Thread workerThread;


    public static byte[] readBuffer;
    public static int readBufferPosition;
    public static volatile boolean stopWorker;
    static Button btn_connect;
    static Dialog dialog;

    static List<String> loadbtpname() {
        List<String> list = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                list.add(device.getName());
            }
        }
        return list;
    }

    public static void dialogBluetooth(Context Context) {
        mContext = Context;

        dialog = new Dialog(mContext, R.style.Theme_AppCompat_Dialog_Alert);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.setContentView(R.layout.dialog_bluetooth);
        isBluetoothHeadsetConnected();
        final Spinner sp_pname;
        final ImageView imgClosed;

        // set the custom dialog components - text, image and button
        btn_connect = dialog.findViewById(R.id.btn_connect);
        imgClosed = dialog.findViewById(R.id.imgClosed);
        sp_pname = dialog.findViewById(R.id.sp_pname);
        List<String> list = new ArrayList<String>(loadbtpname());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, R.layout.support_simple_spinner_dropdown_item, list);
        sp_pname.setAdapter(adapter);
        // if button is clicked, close the custom dialog

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn_connect.getTag().toString().trim().equals("0")) {
                    if (sp_pname.getCount() > 0) {
                        btname = sp_pname.getSelectedItem().toString();
                        new ConnectBT().execute();
                    }
                } else {
                    try {
                        closeBT();
                        btn_connect.setTag("0");
                        btn_connect.setText("Connect");
                        toast("Printer DisConnected");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        imgClosed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

      static BluetoothDevice findBT(Context context) {
        mContext = context;

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                toast(mContext.getString(R.string.NoBluetooth_AdapterAvailable));
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String deviceBTMajorClass = getBTMajorDeviceClass(device.getBluetoothClass().getMajorDeviceClass());
                    Log.d("printer", "deviceBTMajorClass===" + deviceBTMajorClass);
                    // we got this name from the list of paired devices
                    Log.d("BluetoothDevice Name===", device.getName());
                    if (device.getName().equalsIgnoreCase(btname)) {

                        mDevice = device;
                        openBT();
                        break;

                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mDevice;
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

      static OutputStream openBT() throws IOException {
        try {
            if (mDevice != null) {
                Log.d("openBT===", "connected device===" + mDevice.getName());
                mSocket = mDevice.createRfcommSocketToServiceRecord(uuId);
                mSocket.connect();
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
                beginListenForData();

            } else {
                toast(mContext.getString(R.string.Bluetooth_device_is_not_Connected));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mOutputStream;
    }

    static void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // This is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted()
                            && !stopWorker) {

                        try {

                            int bytesAvailable = mInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0,
                                                encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static void closeBT() throws IOException {
        try {
            stopWorker = true;
            if (mOutputStream != null) {
                mOutputStream.close();
                mInputStream.close();
                mSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void resetConnection() {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (Exception e) {
            }
            mInputStream = null;
        }

        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (Exception e) {
            }
            mOutputStream = null;
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (Exception e) {
            }
            mSocket = null;
        }

    }

      static String getBTMajorDeviceClass(int major) {
        switch (major) {
            case android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "AUDIO_VIDEO";
            case android.bluetooth.BluetoothClass.Device.Major.COMPUTER:
                return "COMPUTER";

            case android.bluetooth.BluetoothClass.Device.Major.HEALTH:
                return "HEALTH";
            case android.bluetooth.BluetoothClass.Device.Major.IMAGING:
                return "IMAGING";
            case android.bluetooth.BluetoothClass.Device.Major.MISC:
                return "MISC";
            case android.bluetooth.BluetoothClass.Device.Major.NETWORKING:
                return "NETWORKING";
            case android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL:
                return "PERIPHERAL";
            case android.bluetooth.BluetoothClass.Device.Major.PHONE:
                return "PHONE";
            case android.bluetooth.BluetoothClass.Device.Major.TOY:
                return "TOY";
            case android.bluetooth.BluetoothClass.Device.Major.UNCATEGORIZED:
                return "UNCATEGORIZED";
            case android.bluetooth.BluetoothClass.Device.Major.WEARABLE:
                return "AUDIO_VIDEO";
            default:
                return "unknown!";
        }
    }


    static class ConnectBT extends AsyncTask<Void, Void, String> {

        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(mContext);
            pDialog.setCancelable(false);

            pDialog.setMessage(mContext.getString(R.string.bluetooth_connection) + "...");
            pDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            findBT(mContext);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (mSocket != null) {
                if (mSocket.isConnected()) {
                    btn_connect.setTag("1");
                    btn_connect.setText("disconnect");
                    dialog.dismiss();
                } else {
                    toast(mContext.getString(R.string.Printer_Not_Connect));
                }
            } else {
                toast(mContext.getString(R.string.Printer_Not_Connect));
            }
        }
    }

    static void toast(String msg) {
        Toast toast2 = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        toast2.setGravity(Gravity.CENTER, 0, 0);
        toast2.show();
    }

    public static void LineFeed() {
        try {
            String msg = "     " + "\n" + "     ";
            msg = msg + "\n";
            mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static  void  PrintInvoice() {
        String msg = "";

        try {
            if (mSocket != null && mOutputStream != null && mInputStream != null) {
                /*Title*/
                StringBuilder sb = new StringBuilder("");
                msg = "My Venue\n";
                format = new byte[]{27, 33, 20};
                center = new byte[]{0x1b, 0x61, 0x01};
                arrayOfByte = new byte[]{27, 33, 0};
                format[2] = ((byte) (0x8 | arrayOfByte[2]));

                mOutputStream.write(center);
                mOutputStream.write(format);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);
                  sb = new StringBuilder("");

                sb.append("4th Street ").append("\n");
                sb.append("Portland Oregon").append("\n");
                sb.append("United State,97205").append("\n");
                sb.append("Tel:555-342-1234").append("\n");
                sb.append("Printed ").append("June 21,2019 at 02:30 PM").append("\n");
                msg = sb.toString();
                mOutputStream.write(center);
                mOutputStream.write(normatfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);
                sb = new StringBuilder("");
                sb.append("------------------------------").append("\n");
                sb.append("June 21,2019 at 02:30 PM").append("   "  +"Order #:767").append("\n");
                sb.append("Admin:Admin").append("\n");
                sb.append("Food Tax #:R0123456789").append("\n");
                sb.append("Note: Please add some ketchup packets.").append("\n");
                sb.append("------------------------------").append("\n");


                msg = sb.toString();
                mOutputStream.write(formatLeft);
                mOutputStream.write(normatfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                sb = new StringBuilder("");
                sb.append("2x Chicken And Artichoke Panini").append("  "  +"$14.50").append("\n");
                sb.append("  +$0.50 : Eggplant ").append("\n");
                sb.append("  +$0.50 : Onion").append("\n");
                sb.append("  +Sauce on the side.").append("\n");
                sb.append("------------------------------").append("\n");
                msg = sb.toString();
                mOutputStream.write(formatLeft);
                mOutputStream.write(normatfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                sb = new StringBuilder("");
                sb.append("Food Total").append("  "+"$14.50").append("\n");
                sb.append("          ").append("  "+"------").append("\n");
                sb.append("Sub Total").append("  "+"$14.50").append("\n");
                sb.append("Food Tax").append("   "+"$1.89").append("\n");
                sb.append("        ").append("   "+"-----").append("\n");
                msg = sb.toString();
                mOutputStream.write(formatRight);
                mOutputStream.write(normatfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                msg = "Total  $16.39"+"\n"+"\n"+"\n";
                mOutputStream.write(formatRight);
                mOutputStream.write(boldfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);


                sb = new StringBuilder("");
                sb.append("Thank you").append("\n");
                sb.append("Please Come Again!").append("\n");
                sb.append("Tip Guide :").append("\n");
                sb.append("10%=$1.45").append("15%=$2.18").append("20%=$2.90").append("\n");
                sb.append("------------------------------").append("\n");

                msg = sb.toString();
                mOutputStream.write(center);
                mOutputStream.write(normatfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                /*Bill*/
                msg = "Printed from tablet by Blizzfull Dashboard";

                mOutputStream.write(center);
                mOutputStream.write(boldfontBt);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

               /* *//*Bill number and Datetime*//*
                msg = "B.No:1  26-12-2017 09:10" + "\n";
                format = new byte[]{27, 33, 0};
                arrayOfByte = new byte[]{27, 33, 0};
                format[2] = ((byte) (0x8 | arrayOfByte[2]));
                mOutputStream.write(center);
                mOutputStream.write(format);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                *//*Bill*//*
                sb = new StringBuilder("");
                sb.append("------------------------").append("\n");
                sb.append("ITEM   ").append("QTY").append(" RATE ").append("   AMT  ").append("\n");
                sb.append("------------------------").append("\n");
                sb.append("Flashing LED Bulb       ").append("\n");
                sb.append("       ").append("10 ").append("120.00").append(" 1200.00").append("\n");
                sb.append("Portable LH0021K        ").append("\n");
                sb.append("       ").append(" 5 ").append(" 60.00").append("  300.00").append("\n");
                sb.append("Film Capacitor          ").append("\n");
                sb.append("       ").append(" 8 ").append(" 80.00").append("  640.00").append("\n");
                sb.append("G4 LED Bulb             ").append("\n");
                sb.append("       ").append("20 ").append("  5.00").append("  100.00").append("\n");
                sb.append("------------------------").append("\n");
                sb.append("Sub.Total        2240.00").append("\n");
                sb.append("GST (5%)          112.00").append("\n");
                sb.append("------------------------");
                msg = sb.toString() + "\n";
                Log.e("MSG : ", msg);
                format = new byte[]{27, 33, 0};
                mOutputStream.write(format);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                msg = "Total Rs.  2352.00" + "\n";
                format = new byte[]{27, 33, 20};
                mOutputStream.write(center);
                mOutputStream.write(format);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);

                msg = "THANK YOU" + "\n";
                arrayOfByte = new byte[]{27, 33, 0};
                format[2] = ((byte) (0x8 | arrayOfByte[2]));
                mOutputStream.write(center);
                mOutputStream.write(format);
                mOutputStream.write(msg.getBytes(), 0, msg.getBytes().length);*/
                LineFeed();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean printBitmap(Bitmap bitmap) {

        if (mSocket != null) {
            if (mSocket.isConnected() && mOutputStream != null && mInputStream != null) {
                byte[] center = new byte[]{0x1b, 0x61, 0x01};
                bitmap = BitmapUtil.decodeSampledBitmapFromBitmap(bitmap, 384);
                try {
                    byte[] pixels = BitmapUtil.convert(bitmap);
                    byte xH = (byte) ((((bitmap.getWidth() - 1) / 8) + 1) / AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
                    byte yL = (byte) (bitmap.getHeight() % AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
                    byte yH = (byte) (bitmap.getHeight() / AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
                    byte xL = (byte) ((((bitmap.getWidth() - 1) / 8) + 1) % AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
                    byte[] command = new byte[]{(byte) 29, (byte) 118, (byte) 48, (byte) 0, xL, xH, yL, yH};

                    mOutputStream.write(center);
                    mOutputStream.write(command);
                    mOutputStream.write(pixels);

                    LineFeed();
                    toast(mContext.getString(R.string.Printing) + mContext.getString(R.string.Success));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                toast(mContext.getString(R.string.Printing) + mContext.getString(R.string.failed));
            }
        }
        return true;
    }
}
