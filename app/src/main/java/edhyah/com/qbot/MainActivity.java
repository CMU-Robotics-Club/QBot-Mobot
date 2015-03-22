package edhyah.com.qbot;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


import hanqis.com.qbot.Hanqis_activity;

public class MainActivity extends Activity {

    private Button mHanqisButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void start_Hanqis_activity(View v) {
        Intent i = new Intent(this, Hanqis_activity.class);
        startActivity(i);
    }

    public void startactivity_edward_activity(View view) {
        Intent intent = new Intent(this, edward_activity.class);
        startActivity(intent);
    }

    public void startSpencerActivity(View view) {
        Intent intent = new Intent(this, SpencerActivity.class);
        startActivity(intent);
    }
}
