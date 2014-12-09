package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    double TRIGGER_RADIUS = 25; // Drop pick up trigger radius in meters

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    boolean mPreviewRunning = false;
    Camera mCamera;
    Firebase firebase;
    boolean loggedIn = false;
    GoogleMap map;
    Fragment mapFragment;
    LocationManager mLocationManager;
    LocationListener mLocationListener;
    Location currentLocation;
    ImageView photo;
    ImageButton friends;
    GestureDetectorCompat gDetect;
    boolean photoBeingPreviewed = false;
    boolean usingFrontFacingCamera = false;

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

    //********************************************************************* ************************
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
    //  MAP and LOCATION
    //*********************************************************************************************

    // TODO finishing implementing map functionality
    private void runMap() {
        // Init location
        currentLocation = null;
        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if(currentLocation == null) {
                    zoomMapToLocation(location);
                }
                currentLocation = location;
                //checkForFoundDrop(currentLocation); TODO implement this
                // if current location is within proximity of drop in database sendNotification

                // TODO Gunna want to move this somewhere else... Dont want to call every time

                // make a call to handleNotification()
                //toq.sendNotification(getApplicationContext());
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

        // Init map
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mapFragment = getFragmentManager().findFragmentById(R.id.map);
        map.getUiSettings().setZoomControlsEnabled(false); // Disable zoom buttons
        map.setMyLocationEnabled(true);
        if (currentLocation != null)
        {
            zoomMapToLocation(currentLocation);
        }
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

    private void zoomMapToLocation(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
        // TODO play with other camera positions?
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    //*********************************************************************************************
    //  CAMERA
    //*********************************************************************************************

    private void runCamera() {
        //photo = (ImageView) findViewById(R.id.photo);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mSurfaceView.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
            }
            public void onSwipeRight() {
                if(photoBeingPreviewed) {
                    removeImage(true);
                }
            }
            public void onSwipeLeft() {
                if(photoBeingPreviewed) {
                    removeImage(false);
                }
            }
            public void onSwipeBottom() {
                if(photoBeingPreviewed) {
                    dropImage();
                }
            }
            public void onTap() {
                Toast.makeText(getApplicationContext(), "tap", Toast.LENGTH_LONG).show();
                if(!photoBeingPreviewed) {
                    takePicture();
                }
            }

            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void removeImage(boolean swipeRight) {
        photoBeingPreviewed = false;
        mCamera.startPreview();

        //TODO use boolean to animate away the image in the proper direction.
    }

    private void dropImage() {
        photoBeingPreviewed = false;
        //TODO this with animation;

    }

    private void takePicture() {
        photoBeingPreviewed = true;
        mCamera.takePicture(null, null, mPictureCallback);
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0, data .length);
            if(bitmap != null) {
                Toast.makeText(getApplicationContext(), "How's that look? Swipe to the side to redo, or swipe down to drop.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Hmm that didn't work... please try again.", Toast.LENGTH_LONG).show();
            }
        }
    };

    public void switchCamerasPressed(View view) {
        // Close existing camera
        mCamera.stopPreview();
        photoBeingPreviewed = false;
        mCamera.release();

        // Open new camera
        if(!usingFrontFacingCamera) {
            usingFrontFacingCamera = true;
            mCamera = Camera.open(getFrontCameraIndex());
        }
        else {
            usingFrontFacingCamera = false;
            mCamera = Camera.open(); // defaults to rear cam
        }
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            updatePreview();
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getFrontCameraIndex() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        int camIdx;
        for (camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                break;
            }
        }
        return camIdx;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        updatePreview();
    }

    private void updatePreview() { // Parametarize the preview properly
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
            mCamera.setPreviewDisplay(mSurfaceHolder);
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

    // Launch Friends activity
    public void launchFriendsActivity(View view) {
        friends = (ImageButton) findViewById(R.id.friends_button);

        Intent intent = new Intent(this, Friends.class);
        startActivity(intent);
    }

}
