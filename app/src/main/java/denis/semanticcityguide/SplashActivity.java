package denis.semanticcityguide;

/**
 * Created by Denis on 30/07/2014.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

public class SplashActivity extends Activity {

    private Handler handler;
    AnimationDrawable Anim;
    ImageView iv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity_layout);
        iv1 = (ImageView) findViewById(R.id.loading);
            BitmapDrawable frame1 = (BitmapDrawable) getResources().getDrawable(
                    R.drawable.rdf1);
            BitmapDrawable frame2 = (BitmapDrawable) getResources().getDrawable(
                    R.drawable.rdf2);
            BitmapDrawable frame3 = (BitmapDrawable) getResources().getDrawable(
                    R.drawable.rdf3);
            BitmapDrawable frame4 = (BitmapDrawable) getResources().getDrawable(
                    R.drawable.rdf4);
        BitmapDrawable frame5 = (BitmapDrawable) getResources().getDrawable(
                R.drawable.rdf5);
        BitmapDrawable frame6 = (BitmapDrawable) getResources().getDrawable(
                R.drawable.rdf6);
        BitmapDrawable frame7 = (BitmapDrawable) getResources().getDrawable(
                R.drawable.rdf7);
        BitmapDrawable frame8 = (BitmapDrawable) getResources().getDrawable(
                R.drawable.rdf8);
        BitmapDrawable frame9 = (BitmapDrawable) getResources().getDrawable(
                R.drawable.rdf9);
        BitmapDrawable frame10 = (BitmapDrawable) getResources().getDrawable(
                R.drawable.rdf10);

            Anim = new AnimationDrawable();
            Anim.addFrame(frame1, 100);
            Anim.addFrame(frame2, 100);
            Anim.addFrame(frame3, 100);
            Anim.addFrame(frame4, 100);
            Anim.addFrame(frame5, 100);
            Anim.addFrame(frame6, 100);
            Anim.addFrame(frame7, 100);
            Anim.addFrame(frame8, 100);
            Anim.addFrame(frame9, 100);
            Anim.addFrame(frame10, 100);
            Anim.setOneShot(false);
            iv1.setImageDrawable(Anim);
            Anim.start();
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(isNetworkAvailable()){
                    Intent openMainActivity = new Intent(SplashActivity.this, SearchActivity.class);
                    startActivity(openMainActivity);
                    Anim.stop();
                    finish();
                }
                else{
                    Anim.stop();
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); //Qui è dove è presente la modalità offline
                    //startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //Per attivare invece le impostazioni wi-fi
                }
            }
        }, 5000);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Anim.start();
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isNetworkAvailable()){
                    Intent openMainActivity = new Intent(SplashActivity.this, SearchActivity.class);
                    startActivity(openMainActivity);
                    Anim.stop();
                    finish();
                }
                else{
                    Anim.stop();
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            }
        }, 9000);
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}