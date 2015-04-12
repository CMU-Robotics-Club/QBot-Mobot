package edhyah.com.qbot;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ParameterBar extends LinearLayout {

    private Activity mActivity;
    private String mPrefKey;
    private double mMinRange;
    private double mMaxRange;
    private SeekBar mSeekBar;
    private TextView mValView;
    private double mCurVal;
    private int mMaxBarVal;
    private double mDefaultVal;
    private SharedPreferences mSharedPref;
    private String TAG = "ParameterBar";

    public ParameterBar(Context context, String label, String prefKey, int maxVal, double minRange,
                        double maxRange, double defaultVal) {
        super(context);
        mActivity = (Activity) context; // Can cast as this is called in an activity
        mPrefKey = prefKey;
        mMinRange = minRange;
        mMaxRange = maxRange;
        mMaxBarVal = maxVal;
        mDefaultVal = defaultVal;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.parameter_bar, this, true);

        mCurVal = load();

        TextView labelView = (TextView) findViewById(R.id.param_label);
        labelView.setText(label);

        mValView = (TextView) findViewById(R.id.param_val);
        updateValView();

        mSeekBar = (SeekBar) findViewById(R.id.param_bar);
        mSeekBar.setProgress(valToBarProgress(mCurVal));
        mSeekBar.setMax(maxVal);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateValView(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateValView(seekBar.getProgress());
                save(seekBar.getProgress());
            }
        });
    }

    public double getVal() {
        return mCurVal;
    }

    private int valToBarProgress(double val) {
        double range = mMaxRange - mMinRange;
        double normVal = (val - mMinRange) / range; // Map to 0-1
        return (int)Math.round(normVal * mMaxBarVal);
    }

    private double barProgressToVal(int curVal) {
        double normVal = (1.0 * curVal) / mMaxBarVal; // map to 0-1
        double range = mMaxRange - mMinRange;
        return normVal * range + mMinRange;
    }

    private void updateValView(int progress) {
        mCurVal = barProgressToVal(progress);
        updateValView();
    }

    private void updateValView() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mValView.setText(String.format("%.2f", mCurVal));
            }
        });
    }

    private double load() {
        return mSharedPref.getFloat(mPrefKey, (float)mDefaultVal);
    }

    private void save(int progress) {
        mCurVal = barProgressToVal(progress);
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putFloat(mPrefKey, (float)mCurVal);
        boolean success = editor.commit();
        Log.i(TAG, "Saved " + success + " " + (float)mCurVal + " " + mPrefKey);
    }
}
