package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    boolean mPreviewRunning = false;
    Camera mCamera;
    Firebase firebase;
    boolean loggedIn = false;
    GoogleMap map;
    Fragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_main);

        runFirebase();
        runCamera();
        runMap();
    }

    //*********************************************************************************************
    //  FIREBASE
    //*********************************************************************************************
    private void runFirebase() {
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://dropdatabase.firebaseio.com");
        firebase.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) { // User is logged in
                    loggedIn = true;
                    // Save UID for use throughout the app
                    SharedPreferences.Editor editor = getSharedPreferences("drop", MODE_PRIVATE).edit();
                    editor.putString("uid", authData.getUid());
                    editor.commit();
                } else { // User is not logged in
                    loggedIn = false;
                    launchLogin();
                }
            }
        });
    }

    private void launchLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    //*********************************************************************************************
    //  MAP
    //*********************************************************************************************

    // TODO finishing implementing map functionality
    private void runMap() {
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mapFragment = getFragmentManager().findFragmentById(R.id.map);

        // Adjust size of map view such that camera view is square
        RelativeLayout topLayout = (RelativeLayout) findViewById(R.id.top_layout);
        ViewTreeObserver viewTreeObserver = topLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    RelativeLayout topLayout = (RelativeLayout) findViewById(R.id.top_layout);
                    ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
                    params.width = topLayout.getWidth();
                    params.height = topLayout.getHeight() - topLayout.getWidth();
                    mapFragment.getView().setLayoutParams(params);
                    topLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
        }
    }

    //*********************************************************************************************
    //  CAMERA
    //*********************************************************************************************

    private void runCamera() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void takePicture() {
        mCamera.takePicture(null, null, mPictureCallback);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            mCamera.setDisplayOrientation(90);
        }
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mPreviewRunning = true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mPreviewRunning = false;
        mCamera.release();
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] imageData, Camera c) {
        }
    };


}
