package pe.edu.usat.silviopd.fingerprint;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {

    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private Context mContext;
    private boolean onReadyIdentify = false;
    private boolean isFeatureEnabled_custom = false;
    private Handler mHandler;
    private static final int MSG_AUTH_UI_CUSTOM_TRANSPARENCY = 1010;
    private ArrayList<Integer> designatedFingersDialog = null;
    private boolean isFeatureEnabled_index = false;
    private boolean isFeatureEnabled_fingerprint = false;

    TextView txtMensaje;
    Button btnFingerPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMensaje = (TextView) findViewById(R.id.txtMensaje);

        mContext = this;
        mHandler = new Handler(this);
        mSpassFingerprint = new SpassFingerprint(this);
        mSpass = new Spass();
        try {
            mSpass.initialize(mContext);
        } catch (Exception e) {
            Toast.makeText(this, (CharSequence) e, Toast.LENGTH_LONG).show();
        }
        isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

        if (isFeatureEnabled_fingerprint) {
            mSpassFingerprint = new SpassFingerprint(this);
            Log.i("","Fingerprint Service is supported in the device.");
            Log.i("","SDK version : " + mSpass.getVersionName());
        } else {
            Toast.makeText(this,"Fingerprint Service is not supported in the device.",Toast.LENGTH_LONG).show();
            return;
        }

        btnFingerPrint = (Button) findViewById(R.id.identifyDialogTransparency);
        btnFingerPrint.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.identifyDialogTransparency:
                mHandler.sendEmptyMessage(MSG_AUTH_UI_CUSTOM_TRANSPARENCY);
                break;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AUTH_UI_CUSTOM_TRANSPARENCY:
                setDialogTitleAndTransparency();
                startIdentifyDialog(false);
                break;
        }
        return true;
    }

    private void setDialogTitleAndTransparency() {
        if (isFeatureEnabled_custom) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.setDialogTitle("Customized Dialog With Transparency", 0x000000);
                    mSpassFingerprint.setDialogBgTransparency(0);
                }
            } catch (IllegalStateException ise) {
                Log.i("FeatureEnabled: ", ise.getMessage());
            }
        }
    }

    private void startIdentifyDialog(boolean backup) {
        if (onReadyIdentify == false) {
            onReadyIdentify = true;
            try {
                if (mSpassFingerprint != null) {
                    setIdentifyIndexDialog();
                    mSpassFingerprint.startIdentifyWithDialog(this, mIdentifyListenerDialog, backup);
                }
                if (designatedFingersDialog != null) {
                    Log.i("", "Please identify finger to verify you with " + designatedFingersDialog.toString() + " finger");
                } else {
                    Log.i("", "Please identify finger to verify you");
                }
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
                resetIdentifyIndexDialog();
                Log.i("", "Exception: " + e);
            }
        } else {
            Log.i("", "The previous request is remained. Please finished or cancel first");
        }
    }

    private void resetIdentifyIndexDialog() {
        designatedFingersDialog = null;
    }

    private void setIdentifyIndexDialog() {
        if (isFeatureEnabled_index) {
            if (mSpassFingerprint != null && designatedFingersDialog != null) {
                mSpassFingerprint.setIntendedFingerprintIndex(designatedFingersDialog);
            }
        }
    }

    private SpassFingerprint.IdentifyListener mIdentifyListenerDialog = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            Log.i("", "identify finished : reason =" + getEventStatusName(eventStatus));
            int FingerprintIndex = 0;
            boolean isFailedIdentify = false;
            onReadyIdentify = false;
            try {
                FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
                Log.i("", ise.getMessage());
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                Log.i("", "onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);

                txtMensaje.setText("Bienvenido Papi Chulo :v " + FingerprintIndex);

            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
                Log.i("", "onFinished() : Password authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED
                    || eventStatus == SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE) {
                Log.i("", "onFinished() : User cancel this identify.");
            } else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
                Log.i("", "onFinished() : The time for identify is finished.");
            } else if (!mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_AVAILABLE_PASSWORD)) {
                if (eventStatus == SpassFingerprint.STATUS_BUTTON_PRESSED) {
                    Log.i("", "onFinished() : User pressed the own button");
                    Toast.makeText(mContext, "Please connect own Backup Menu", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.i("", "onFinished() : Authentification Fail for identify");
                isFailedIdentify = true;
            }
            if (!isFailedIdentify) {
                resetIdentifyIndexDialog();
            }
        }

        @Override
        public void onReady() {
            Log.i("", "identify state is ready");
        }

        @Override
        public void onStarted() {
            Log.i("", "User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
            Log.i("", "the identify is completed");
        }
    };

    private static String getEventStatusName(int eventStatus) {
        switch (eventStatus) {
            case SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS:
                return "STATUS_AUTHENTIFICATION_SUCCESS";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS:
                return "STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS";
            case SpassFingerprint.STATUS_TIMEOUT_FAILED:
                return "STATUS_TIMEOUT";
            case SpassFingerprint.STATUS_SENSOR_FAILED:
                return "STATUS_SENSOR_ERROR";
            case SpassFingerprint.STATUS_USER_CANCELLED:
                return "STATUS_USER_CANCELLED";
            case SpassFingerprint.STATUS_QUALITY_FAILED:
                return "STATUS_QUALITY_FAILED";
            case SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
                return "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE";
            case SpassFingerprint.STATUS_BUTTON_PRESSED:
                return "STATUS_BUTTON_PRESSED";
            case SpassFingerprint.STATUS_OPERATION_DENIED:
                return "STATUS_OPERATION_DENIED";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED:
            default:
                return "STATUS_AUTHENTIFICATION_FAILED";
        }
    }
}
