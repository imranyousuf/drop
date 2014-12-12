package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    double TRIGGER_RADIUS = 25; // Drop pick up trigger radius in meters

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    boolean mPreviewRunning = false;
    Camera mCamera;
    Firebase firebase;
    boolean loggedIn = true;
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
    Switch public_switch;
    ArrayList<Drop> drops; // Holds all the drops
    ArrayList<Drop> collectedDrops; // Holds all the collected
    ArrayList<Drop> notifiedDrops; // Holds all the drops that have triggered a notification
    ProgressBar spinner;
    boolean activityInBackground = false;
    ToqActivity toq = new ToqActivity();
    String abc;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_main);

        drops = new ArrayList<Drop>();
        collectedDrops = new ArrayList<Drop>();
        notifiedDrops = new ArrayList<Drop>();

        spinner = (ProgressBar) findViewById(R.id.spinner);
        spinner.animate();
        spinner.setVisibility(View.GONE);

        runFirebase();

        checkNotification();

        runCamera();

        install_app();

        runMap();



    }

    @Override
    protected void onResume() {
        super.onResume();

        if(loggedIn == false) {
            launchLogin();
        }
    }

    private void install_app() {
        Intent intent = new Intent(getApplicationContext(), ToqActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityInBackground = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        activityInBackground = true;
    }

    private boolean isDropCollected(Drop dropUnderTest) {
        for (Drop drop : collectedDrops) {
            if(drop.getKey().equals(dropUnderTest.getKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDropInDrops(Drop dropUnderTest) {
        for (Drop drop : drops) {
            if(drop.getKey().equals(dropUnderTest.getKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDropNotified(Drop dropUnderTest) {
        for (Drop drop : notifiedDrops) {
            if(drop.getKey().equals(dropUnderTest.getKey())) {
                return true;
            }
        }
        return false;
    }

    private void checkNotification() {
        final Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if(extras.getString("dropKey") == null || extras.getString("id") == null ) return;

            Toast.makeText(getApplicationContext(), "Picking up your drop...", Toast.LENGTH_SHORT).show();
            final String dropKey = extras.getString("dropKey");

            // Cancel the notification
            final int notifID = Integer.valueOf(extras.getString("id"));
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
            nMgr.cancel(notifID);

            pickUpDrop(dropKey);
         }
    }

    private void pushDropFoundNotification(Drop drop) { // Push a notification to the user notifying them of the drop they found
        int marker_resource;
        if(drop.getPostIsPublic() == true) {
            marker_resource = R.drawable.public_drop_message;
        } else {
            marker_resource = R.drawable.friend_drop_message;
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(marker_resource)
                        .setContentTitle("Tap here to pick up your drop!")
                        .setContentText(drop.getTags());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("dropKey", drop.getKey());
        Random r = new Random();
        int id = r.nextInt(1000 - 0) + 0; // make random id for notif
        resultIntent.putExtra("id", Integer.toString(id));
        //toq1.sendNotification(this);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Remember the drops that have been notified about...
        //notifiedDrops.add(drop); // this is done prior to method call
        mNotificationManager.notify(id, mBuilder.build()); // use index to remember
    }

    public void collectedDropsPressed(View view) {
        Intent intent = new Intent(this, CollectedDrops.class);
        startActivity(intent);
    }

    //*********************************************************************************************
    //  FIREBASE
    //*********************************************************************************************
    private void runFirebase() {
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://dropdatabase.firebaseio.com");
        runAuthenticationListener();

        runDropListener();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runCollectedDropListener();
            }
        }, 1000);
    }

    private void runAuthenticationListener() {
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

    private void runDropListener() {
        firebase.child("drops").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {

                Map<String, Object> drop = (Map<String, Object>) snapshot.getValue();
                // Save drops for use later
                Drop dropObj = new Drop();
                dropObj.setKey(snapshot.getKey());
                dropObj.setLat((Double) drop.get("lat"));
                dropObj.setLon((Double) drop.get("lon"));
                dropObj.setPublic(((Boolean) drop.get("public")).booleanValue());
                dropObj.setImageKey((String) drop.get("imageKey"));
                dropObj.setEpoch((Long)drop.get("epoch"));
                dropObj.setDropperUID((String)drop.get("dropperUID"));
                if(!isDropInDrops(dropObj)) {
                    drops.add(dropObj);
                    addDropToMap(dropObj);
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }


    private void runCollectedDropListener() {
        // Get User UID
        SharedPreferences prefs = getSharedPreferences("drop", MODE_PRIVATE);
        String UID = prefs.getString("uid", null); // User UID

        if(UID == null) return;

        firebase.child("users").child(UID).child("dropsCollected").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                String keyString = (String) snapshot.getValue();
                for(Drop drop : drops) { // Check to see if the drop has been collected before.
                    if(drop.getKey().equals(keyString)) {
                        if(!isDropCollected(drop)) {
                            collectedDrops.add(drop);
                        }
                        updateDropsOnMap();
                    }
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void launchLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    private void uploadDrop() {
        // Convert image to string
        String imageString = "";
        if(rotatedBitmap != null) {
            imageString = ImageHelper.BitMapToString(rotatedBitmap);
        }

        // Upload image first to image database and retrieve the image uid.
        Firebase newImage = firebase.child("photos").push();
        newImage.setValue(imageString, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError != null) {
                    Toast.makeText(getApplicationContext(), "There was a problem uploading the photo =/", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Get User UID
        SharedPreferences prefs = getSharedPreferences("drop", MODE_PRIVATE);
        String UID = prefs.getString("uid", null); // User UID

        // Upload to database
        Map<String, Object> drop = new HashMap<String, Object>();
        drop.put("epoch", System.currentTimeMillis());
        drop.put("imageKey", newImage.getKey());
        drop.put("lat", currentLocation.getLatitude());
        drop.put("lon", currentLocation.getLongitude());
        drop.put("public", public_switch.isChecked());
        drop.put("dropperUID", UID);
        Firebase newDrop = firebase.child("drops").push();
        newDrop.setValue(drop, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError == null) {
                    Toast.makeText(getApplicationContext(), "Your drop has been dropped!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Something went wrong... please try again.", Toast.LENGTH_LONG).show();
                }

                updateDropsOnMap();
            }
        });

        // Add key to user profile of drops
        String dropKey = newDrop.getKey(); // Drop Key
        Firebase userDrops = firebase.child("users").child(UID).child("drops");
        Firebase newUserDrop = userDrops.push(); // Keys dont matter for these
        newUserDrop.setValue(dropKey, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError != null) {
                    Toast.makeText(getApplicationContext(), "Something went wrong... please try again.", Toast.LENGTH_LONG).show();
                }
                updateDropsOnMap();
            }
        });
    }

    private void addToCollectedDrops(Drop newDrop) {
        // Get User UID
        SharedPreferences prefs = getSharedPreferences("drop", MODE_PRIVATE);
        String UID = prefs.getString("uid", null); // User UID

        // Add key to user profile of drops
        String dropKey = newDrop.getKey(); // Drop Key
        Firebase userDrops = firebase.child("users").child(UID).child("dropsCollected");
        Firebase newUserDrop = userDrops.push(); // Keys dont matter for these
        newUserDrop.setValue(dropKey, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError != null) {
                    Toast.makeText(getApplicationContext(), "Something went wrong... please try again.", Toast.LENGTH_LONG).show();
                }
                updateDropsOnMap();
            }
        });
    }

    private void pickUpDrop (final String dropKey) {
        final Drop dropObj = new Drop();
        // First look up the drop meta data with the dropKey
        firebase.child("drops").child(dropKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> drop = (Map<String, Object>) snapshot.getValue();
                // Save drops for use later
                dropObj.setKey(snapshot.getKey());
                dropObj.setLat((Double) drop.get("lat"));
                dropObj.setLon((Double) drop.get("lon"));
                dropObj.setPublic(((Boolean) drop.get("public")).booleanValue());
                dropObj.setImageKey((String) drop.get("imageKey"));
                dropObj.setEpoch((Long)drop.get("epoch"));
                dropObj.setDropperUID((String)drop.get("dropperUID"));

                // Then. use the drop meta data to find the drop image data
                firebase.child("photos").child(dropObj.getImageKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Finally, collect the drop once the image has been retrieved
                        String image = (String) snapshot.getValue();
                        Bitmap imageBitmap = ImageHelper.StringToBitMap(image);
                        animatePickUpDrop(imageBitmap);
                        addToCollectedDrops(dropObj);
                        if(!isDropCollected(dropObj)) {
                            collectedDrops.add(dropObj);
                            updateDropsOnMap();
                        }
                    }
                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        Toast.makeText(getApplicationContext(), "There was an error picking up your drop =/", Toast.LENGTH_LONG).show();
                    }
                });

            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void animatePickUpDrop(Bitmap bitmap) {
        photo.setImageBitmap(bitmap);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setVisibility(View.VISIBLE);
        // Animate the image up to the collected drops buttons
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.found_drop);
        animation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {}
            @Override
            public void onAnimationRepeat(Animation arg0) {}
            @Override
            public void onAnimationEnd(Animation arg0) {
                photo.setVisibility(View.INVISIBLE);
                photo.setScaleType(ImageView.ScaleType.FIT_XY);
            }
        });
        Toast.makeText(getApplicationContext(), "You found a drop! It'll be saved for you in your Collected Drops.", Toast.LENGTH_LONG).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                photo.startAnimation(animation);
            }
        }, 5000);
    }

    //*********************************************************************************************
    //  MAP and LOCATION
    //*********************************************************************************************
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
                checkForFoundDrop(currentLocation); //TODO implement this

                // TODO Gunna want to move this somewhere else... Dont want to call every time
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        // Hack to get best GPS reception but not before data arrives.
        spinner.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                // If no GPS link after 5 seconds then switch providers.
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        spinner.setVisibility(View.GONE);
                        if(currentLocation == null) {
                            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
                        }
                    }
                }, 5000);

            }
        }, 3000);

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

    // Called every time the location of user changes.
    Drop previouslyFound;
    public void checkForFoundDrop(Location userLocation) {
        for (Drop drop : drops) {
            Location dropLocation = new Location("");
            dropLocation.setLatitude(drop.getLat());
            dropLocation.setLongitude(drop.getLon());
            double distanceInMeters =  userLocation.distanceTo(dropLocation);
            // Make sure the drop meets criteria to be picked up
            if((distanceInMeters < TRIGGER_RADIUS) &&   // Its in range
                    (public_switch.isChecked() == drop.getPostIsPublic()) && //Its the correct mode
                    (!isDropCollected(drop)) &&
                    (!isDropNotified(drop))){ // It has no not been collected before
                //DROP WAS FOUND
                notifiedDrops.add(drop);
                if(activityInBackground) {
                    pushDropFoundNotification(drop);
                } else {
                    pickUpDrop(drop.getKey());
                }
            }
        }
    }

    private void updateDropsOnMap() {
        map.clear();
        for (Drop drop : drops) {
            addDropToMap(drop);
        }
    }

    private void addDropToMap(final Drop drop) {
        if( (drop.getPostIsPublic() != public_switch.isChecked()) ||
                isDropCollected(drop)) {
            return; // Bail is the drops should not be on the map based on the switch current state.
        }

        spinner.setVisibility(View.VISIBLE);
        // Get the username of the drop for now, will probably get image and other data later
        firebase.child("users").child(drop.getDropperUID()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                spinner.setVisibility(View.GONE);
                String username = (String) snapshot.getValue();
                int marker_resource;
                if(drop.getPostIsPublic() == true) {
                    marker_resource = R.drawable.public_drop_message;
                } else {
                    marker_resource = R.drawable.friend_drop_message;
                }
                map.addMarker(new MarkerOptions()
                        .position(new LatLng(drop.getLat(), drop.getLon()))
                        .title(username)
                        .icon(BitmapDescriptorFactory.fromResource(marker_resource)));
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private void zoomMapToLocation(Location location) {
        if(location == null) {
            // Switch providers
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            return;
        }

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
        updateDropsOnMap();
    }

    //*********************************************************************************************
    //  CAMERA
    //*********************************************************************************************

    private void runCamera() {
        photo = (ImageView) findViewById(R.id.photo);
        photo.setVisibility(View.INVISIBLE);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        public_switch = (Switch) findViewById(R.id.public_switch);
        public_switch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateDropsOnMap();
            }
        });

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
                //Toast.makeText(getApplicationContext(), "tap", Toast.LENGTH_LONG).show();
                if(!photoBeingPreviewed) {
                    mCamera.autoFocus(null);
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

        // Animate the image the proper directions
        Animation animation;
        if(!swipeRight) {
            animation = AnimationUtils.loadAnimation(this, R.anim.swipe_left);
        } else {
            animation = AnimationUtils.loadAnimation(this, R.anim.swipe_right);
        }
        animation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {}
            @Override
            public void onAnimationRepeat(Animation arg0) {}
            @Override
            public void onAnimationEnd(Animation arg0) {
                photo.setVisibility(View.INVISIBLE);
            }
        });
        photo.startAnimation(animation);
    }

    private void dropImage() {
        photoBeingPreviewed = false;
        mCamera.startPreview();

        // Animate map to current location
        zoomMapToLocation(currentLocation);

        // Animate the image the proper directions
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.swipe_down);
        animation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {}
            @Override
            public void onAnimationRepeat(Animation arg0) {}
            @Override
            public void onAnimationEnd(Animation arg0) {
                photo.setVisibility(View.INVISIBLE);
            }
        });
        photo.startAnimation(animation);

        // Upload image to database in background
        Toast.makeText(getApplicationContext(), "Droping...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                uploadDrop();
            }
        }).start();
    }

    Bitmap rotatedBitmap;
    private void takePicture() {
        photoBeingPreviewed = true;
        mCamera.takePicture(null, null, null, mPictureCallback);
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0, data.length);
            if(bitmap != null){
                // TODO: use the jpeg data array to determine orientation (these values are hardcoded for my phone).
                Matrix matrix = new Matrix();
                if(usingFrontFacingCamera){
                    matrix.postRotate(-90);
                    matrix.postScale(-1,1);
                }
                else {
                    matrix.postRotate(90);
                }
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                photo.setImageBitmap(rotatedBitmap);
                photo.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "How's that look? Swipe to the side to redo, or swipe down to drop.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Hmm that didn't work... please try again.", Toast.LENGTH_LONG).show();
            }
        }
    };

    public void switchCamerasPressed(View view) {
        // Bail if they are previewing an image
        if(photoBeingPreviewed) return;

        // Close existing camera
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        photoBeingPreviewed = false;
        mCamera.release();
        mCamera = null;

        // Open new camera
        try {
            if(!usingFrontFacingCamera) {
                usingFrontFacingCamera = true;
                mCamera = Camera.open(getFrontCameraIndex());
            }
            else {
                usingFrontFacingCamera = false;
                mCamera = Camera.open(); // defaults to rear cam
            }
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(), "Camera unavailable", Toast.LENGTH_LONG).show();
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
        try {
            mCamera = Camera.open();
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(), "Camera unavailable", Toast.LENGTH_LONG).show();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        updatePreview();
    }

    private void updatePreview() { // Parametarize the preview properly
        if(mCamera == null) {
            return;
        }
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> photoSizes = parameters.getSupportedPictureSizes();
        previewSizes.retainAll(photoSizes);
        Camera.Size previewSize = getOptimalPreviewSize(previewSizes, mSurfaceView.getWidth(), mSurfaceView.getHeight());
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPictureSize(previewSize.width, previewSize.height);
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
        usingFrontFacingCamera = false;
        photoBeingPreviewed = false;
        mCamera.release();
        mCamera = null;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    // Launch Friends activity
    public void launchFriendsActivity(View view) {
        friends = (ImageButton) findViewById(R.id.friends_button);

        Intent intent = new Intent(this, Friends.class);
        startActivity(intent);
    }

}
