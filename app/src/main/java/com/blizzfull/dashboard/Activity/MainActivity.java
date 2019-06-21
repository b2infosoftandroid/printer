package com.blizzfull.dashboard.Activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blizzfull.dashboard.BuildConfig;
import com.blizzfull.dashboard.R;
import com.daasuu.ei.Ease;
import com.daasuu.ei.EasingInterpolator;

import java.io.IOException;
import java.util.Calendar;

import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.PrintInvoice;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.dialogBluetooth;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.isBluetoothHeadsetConnected;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mDevice;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mInputStream;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mOutputStream;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.mSocket;
import static com.blizzfull.dashboard.BluetoothPrinter.BluetoothClass.printBitmap;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    protected SharedPreferences sharedPref;
    protected Boolean isManagedDevice = false;
    RelativeLayout rlview;
    ImageView imglogo;
    Button btnPrint;
    private static int SPLASH_TIMEOUT = 4000;
    TextView restaurantNameTextView;
    String storedRestaurantName;
    Context mContext;
    Drawable drawable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext=MainActivity.this;
        rlview = findViewById(R.id.rlview);
        imglogo = findViewById(R.id.logo);
        btnPrint = findViewById(R.id.btnPrint);
        webView = findViewById(R.id.webView);
        restaurantNameTextView = findViewById(R.id.restaurantNameText);
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printImage();
            }
        });
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction("no_add_user")) {
            isManagedDevice = true;
        } else {
            isManagedDevice = false;
        }

          storedRestaurantName = sharedPref.getString("restaurantName", "");
          restaurantNameTextView.setText(storedRestaurantName);



        final RelativeLayout splashScreen = findViewById(R.id.splashView);

        if(isManagedDevice)
        {
            final ViewTreeObserver observer = splashScreen.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Boolean wallpaperSet = sharedPref.getBoolean("wallpaperSet", false);
                            if(!wallpaperSet)
                            {
                                setWallpaper(splashScreen);
                            }
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean("wallpaperSet",true);
                        }
                    });
        }

        String versionName = "v " + BuildConfig.VERSION_NAME;
        TextView text =findViewById(R.id.versionText);
        text.setText(versionName);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;

        new Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        splashScreen.animate().alpha(0).translationY(height).setDuration(1000).setInterpolator(new EasingInterpolator(Ease.QUART_OUT)).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                splashScreen.setVisibility(View.GONE);
                            }
                        });
                    }
                },
                4000);


        webView.getSettings().setJavaScriptEnabled(true);
        JavaScriptInterface jsInterface = new JavaScriptInterface(this);
        webView.addJavascriptInterface(jsInterface, "blizzfullInterface");
        webView.loadUrl("http://dashboard.blizzfull.com");



        webView.setWebViewClient(new WebViewClient() {


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(request.getUrl().toString().equals("file:///android_asset/android---app/restart"))
                {
                    webView.loadUrl("http://dashboard.blizzfull.com");
                    return true;
                }
                else if(request.getUrl().toString().equals("file:///android_asset/android---app/wifi"))
                {
                    startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                    return true;
                }
                else
                {
                    view.loadUrl(request.getUrl().toString());
                }
                return false;
            }

            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                view.loadUrl("file:///android_asset/error.html");
            }

        });

        final Handler h = new Handler();
        h.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Calendar rightNow = Calendar.getInstance();
                int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
                if(currentHour == 4) webView.reload();//Reload every day @ 4am
                h.postDelayed(this, 900000);
            }
        }, 900000); // 15 minutes in ms
    }

    public void setWallpaper(View v)
    {
        Bitmap b = Bitmap.createBitmap( v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        TextView text = findViewById(R.id.versionText);
        text.setVisibility(View.INVISIBLE);
        v.draw(c);
        text.setVisibility(View.VISIBLE);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        WallpaperManager myWallpaperManager = WallpaperManager.getInstance(getApplicationContext());

        try {

            myWallpaperManager.setBitmap(Bitmap.createScaledBitmap(b,width,height,true), null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);

        } catch (IOException e) {
            // TODO Auto-generated catch block
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onBackPressed() {
        if ( webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    public class JavaScriptInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public void print()
        {

        }

        @android.webkit.JavascriptInterface
        public void setRestaurant(String shortName, String name)
        {

            SharedPreferences.Editor editor = sharedPref.edit();
            String storedShortName = sharedPref.getString("shortName", "");
            if(!storedShortName.equals(shortName))
            {
                editor.putString("shortName",shortName);
                editor.putString("restaurantName",name);
                final String fName = name;
                editor.apply();

                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {

                        final RelativeLayout splashScreen = findViewById(R.id.splashView);
                        TextView restaurantNameTextView = findViewById(R.id.restaurantNameText);
                        restaurantNameTextView.setText(fName);

                        if(isManagedDevice) setWallpaper(splashScreen);
                    }
                });


            }


        }
    }


    public void printImage() {

        if (isBluetoothHeadsetConnected()) {
            if (mDevice == null || mSocket == null || mInputStream == null||mOutputStream == null) {
                dialogBluetooth(mContext);
            }else {

              Bitmap icon =BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.logo);
                printBitmap(icon);
                PrintInvoice();
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
        builder.setMessage(message).setTitle("Bluetooth Device")
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
