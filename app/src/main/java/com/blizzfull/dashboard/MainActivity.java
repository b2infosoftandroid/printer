package com.blizzfull.dashboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daasuu.ei.Ease;
import com.daasuu.ei.EasingInterpolator;

import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    protected SharedPreferences sharedPref;
    protected Boolean isManagedDevice = false;

    private static int SPLASH_TIMEOUT = 4000;

    @Override
    public void onBackPressed() {
        if ( webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction("no_add_user")) {
            isManagedDevice = true;
        } else {
            isManagedDevice = false;
        }

        String storedRestaurantName = sharedPref.getString("restaurantName", "");
        TextView restaurantNameTextView = findViewById(R.id.restaurantNameText);
        restaurantNameTextView.setText(storedRestaurantName);

        getSupportActionBar().hide();

        webView = (WebView) findViewById(R.id.webView);
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
        TextView text = (TextView) findViewById(R.id.versionText);
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
        TextView text = (TextView) findViewById(R.id.versionText);
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
}
