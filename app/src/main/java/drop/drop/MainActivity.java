package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

import java.util.Map;

public class MainActivity extends Activity {

    Firebase dropFirebase;
    GoogleMap map;

    LocationManager mLocationManager;
    LocationListener mLocationListener;

    Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_main);

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
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
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
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    public void postDropPressed(View view) {
        // TODO: CHECK TO SEE IF USER SIGNED IN BEFORE ALLOWING POST. IF NOT SIGNED IN, LAUNCH SIGN IN.

      //  Intent intent=new Intent(this, FBLogin.class);
      //  startActivity(intent);

        Intent intent = new Intent(this, PostDropActivity.class);
        startActivity(intent);
    }
}
