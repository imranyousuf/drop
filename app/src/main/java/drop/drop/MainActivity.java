package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {

    Firebase dropFirebase;
    GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        actionBar.hide();

        Firebase.setAndroidContext(this);
        // I made the database under my account, ask me and I'll add your account as a developer on it. -Kyle
        dropFirebase = new Firebase("https://dropdatabase.firebaseio.com/");
        dropFirebase.child("drops").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                System.out.println(snapshot.getValue()); // Simple reading data test.
            }
            @Override public void onCancelled(FirebaseError error) { }
        });

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        Location location = new Location("");
        location.setLatitude(37.875558);
        location.setLongitude(-122.258689);
        if (location != null)
        {
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
        map.getUiSettings().setZoomControlsEnabled(false); // Disable zoom buttons

        // Hard coded markers for
        map.addMarker(new MarkerOptions().position(new LatLng(37.875558, -122.258689)).title("#DANGEROUS #DROP"));
        map.addMarker(new MarkerOptions().position(new LatLng(37.875567, -122.258962)).title("#DIS #DROP #THO"));
        map.addMarker(new MarkerOptions().position(new LatLng(37.875397, -122.259890)).title("#Youll #wanna #see #this"));
        map.addMarker(new MarkerOptions().position(new LatLng(37.874925, -122.260121)).title("#Great #food"));
        map.addMarker(new MarkerOptions().position(new LatLng(37.875315, -122.257774)).title("#Stanfurd #SUX"));
        map.addMarker(new MarkerOptions().position(new LatLng(37.875433, -122.259147)).title("#Crazy #Stuff"));
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
}
