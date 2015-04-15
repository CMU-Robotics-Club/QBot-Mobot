package edhyah.com.qbot;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

    public static final String PREF_MAX_SPEED = "edhyah.com.qbot.pref.MAX_SPEED";
    public static final String PREF_TUNNING = "edhyah.com.qbot.pref.TUNNING";
    public static final String PREF_THRESHOLD = "edhyah.com.qbot.pref.THRESHOLD";
    public static final String PREF_PID_P = "edhyah.com.qbot.pref.PID_P";
    public static final String PREF_PID_I = "edhyah.com.qbot.pref.PID_I";
    public static final String PREF_PID_D = "edhyah.com.qbot.pref.PID_D";
    public static final String PREF_DIMENSION = "edhyah.com.qbot.pref.DIMENSION";
    public static final String PREF_STD_THRESHOLD = "edhyah.com.qbot.pref.STD_THRESHOLD";
    public static final String PREF_STD_TURN = "edhyah.com.qbot.pref.STD_TURN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = (LinearLayout) findViewById(R.id.main_main);

        // Add Bars
        ParameterBar maxSpeedBar = new ParameterBar(this, "MS", PREF_MAX_SPEED, 30, 0.0, 30.0, 20.0);
        layout.addView(maxSpeedBar);
        ParameterBar tuneBar = new ParameterBar(this, "Tn", PREF_TUNNING, 200, -1.0, 1.0, 0.0);
        layout.addView(tuneBar);
        ParameterBar threshBar = new ParameterBar(this, "Th", PREF_THRESHOLD, 300, 0.0, 3.0, 1.0);
        layout.addView(threshBar);
        ParameterBar pidPBar = new ParameterBar(this, "P", PREF_PID_P, 100, 0.0, 1.0, 1.0);
        layout.addView(pidPBar);
        ParameterBar pidIBar = new ParameterBar(this, "I", PREF_PID_I, 100, 0.0, 0.1, 0.0);
        layout.addView(pidIBar);
        ParameterBar pidDBar = new ParameterBar(this, "D", PREF_PID_D, 100, 0.0, 1.0, 0.0);
        layout.addView(pidDBar);
        ParameterBar dimensionBar = new ParameterBar(this, "RD",PREF_DIMENSION,4,1.0,5.0,2.0);
        layout.addView(dimensionBar);
        ParameterBar stdThreshBar = new ParameterBar(this,"SdTh",PREF_STD_THRESHOLD,100,0.0,100.0,25.0);
        layout.addView(stdThreshBar);
        ParameterBar turnBar = new ParameterBar(this,"Turn",PREF_STD_TURN,1,0.0,1.0,0.0);
        layout.addView(turnBar);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void startMobotActivity(View view) {
        Intent intent = new Intent(this, MobotActivity.class);
        startActivity(intent);
    }
}
