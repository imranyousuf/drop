package drop.drop;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends FragmentActivity {

    double TRIGGER_RADIUS = 100; // Drop pick up trigger radius in meters

    Firebase dropFirebase;
    GoogleMap map;

    LocationManager mLocationManager;
    LocationListener mLocationListener;

    Location currentLocation;

    View shadowView;

    private FBFragment fbFragment;

    ArrayList<Drop> drops; // Holds all the drops
    ArrayList<Drop> notifications; // Holds all active notifications

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drops = new ArrayList<Drop>();
        notifications = new ArrayList<Drop>();

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_main);

        shadowView = (View) findViewById(R.id.shadow);
        shadowView.getBackground().setAlpha(0); // Dont show shadow initially until popover view is shown.

        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            fbFragment = new FBFragment();
            fbFragment.parentActivity = this; // give it a ref to communicate with this activity
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, fbFragment).commit();
        } else {
            // Or set the fragment from restored state info
            fbFragment = (FBFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        }
        getSupportFragmentManager().beginTransaction().hide(fbFragment).commit(); // Dont show facebook login button until needed.

        initLocation();
        initMap();
        initDatabase();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void initLocation() {
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
                checkForFoundDrop(currentLocation);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    // Called every time the location of user changes.
    public void checkForFoundDrop(Location userLocation) {
        double minDistance = Double.MAX_VALUE;
        Drop closestDropInRadius = null; // May be more than one drop in trigger radius
        for (Drop drop : drops) {
            Location dropLocation = new Location("");
            dropLocation.setLatitude(drop.getLat());
            dropLocation.setLongitude(drop.getLon());
            double distanceInMeters =  userLocation.distanceTo(dropLocation);
            if(distanceInMeters < TRIGGER_RADIUS && distanceInMeters < minDistance) {
                minDistance = distanceInMeters;
                closestDropInRadius = drop;
            }
        }

        if(closestDropInRadius != null) { // a drop was found in range
            pushNotification(closestDropInRadius);
        }
    }

    public void pushNotification(Drop drop) { // Push a notification to the user notifying them of the drop they found
        if(notifications.contains(drop)) {
            return; // if a notification has already been posted for this drop, dont do it again.
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("You've found a Drop! Tap here to pick it up.")
                        .setContentText(drop.getTags());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, DropViewerActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(DropViewerActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Remember the drops that have been notified about...
        notifications.add(drop); // remember this notification
        mNotificationManager.notify(notifications.size()-1, mBuilder.build()); // use index to remember
    }

    private void initMap() {
        // Init map
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
        if (currentLocation != null)
        {
            zoomMapToLocation(currentLocation);
        }
        map.getUiSettings().setZoomControlsEnabled(false); // Disable zoom buttons

    }

    private void zoomMapToLocation(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void initDatabase() {
        Firebase.setAndroidContext(this);
        dropFirebase = new Firebase("https://dropdatabase.firebaseio.com/");
        dropFirebase.child("drops").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                Map<String, Object> drop = (Map<String, Object>) snapshot.getValue();
                map.addMarker(new MarkerOptions()
                        .position(new LatLng((Double) drop.get("lat"), ((Double) drop.get("lon"))))
                        .title((String) drop.get("tags"))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.drop_message_icon)));

                // Save drops for use later
                Drop dropObj = new Drop(snapshot.getKey(),              // key in database
                                        "",                             // image (not storing this for now)
                                        (Double) drop.get("lat"),
                                        (Double) drop.get("lon"),
                                        (String) drop.get("tags"),
                                        (String)drop.get("text"));
                drops.add(dropObj);

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

    public void showFacebook() {
        // Make sure to make darken background when showing popover
        fbFragment.SHOWN = true; // tell the fragment it is visible.
        getSupportFragmentManager().beginTransaction().show(fbFragment).commit();
        shadowView.getBackground().setAlpha(200); // Max val 255
    }

    public void hideFacebook() {
        fbFragment.SHOWN = false;
        getSupportFragmentManager().beginTransaction().hide(fbFragment).commit();
        shadowView.getBackground().setAlpha(0); // Max val 255
    }

    public void composeDrop() {
        Intent intent = new Intent(this, PostDropActivity.class);
        startActivity(intent);
    }

    public void postDropPressed(View view) {
        if(fbFragment.LOGGED_IN) {
            composeDrop();
        } else {
            showFacebook();
        }
    }

    public void settingsPressed(View view) {
        // TEMPORARILY A DEBUG BUTTON
        showFacebook();
    }
}
