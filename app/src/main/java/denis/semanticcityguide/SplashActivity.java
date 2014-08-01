package denis.semanticcityguide;

/**
 * Created by Denis on 30/07/2014.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.TextView;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity_layout);
        Typeface tf = Typeface.createFromAsset(getAssets(),
                "fonts/MTFToast.ttf");
        TextView tv = (TextView) findViewById(R.id.benvenuto2);
        tv.setTypeface(tf);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isNetworkAvailable()){
                    Intent openMainActivity = new Intent(SplashActivity.this, SearchActivity.class);
                    startActivity(openMainActivity);
                    finish();
                }
                else{
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); //Qui è dove è presente la modalità offline
                    //startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //Per attivare invece le impostazioni wi-fi
                }
            }
        }, 5000);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isNetworkAvailable()){
                    Intent openMainActivity = new Intent(SplashActivity.this, SearchActivity.class);
                    startActivity(openMainActivity);
                    finish();
                }
                else{
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            }
        }, 5000);
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}