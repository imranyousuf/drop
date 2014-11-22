package drop.drop;


import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class PostDropActivity extends Activity {

    Firebase dropFirebase;
    ImageView imageView;
    Bitmap imageBitmap;

    EditText textView;

    LocationManager mLocationManager;
    Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        Firebase.setAndroidContext(this);
        dropFirebase = new Firebase("https://dropdatabase.firebaseio.com/");

        currentLocation = null;
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                currentLocation = location;
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        setContentView(R.layout.activity_post_drop);
        imageView = (ImageView) findViewById(R.id.image);
        textView = (EditText) findViewById(R.id.text);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_post_drop, menu);
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

    public void checkPressed(View view) {
        Log.e("DEBUG", "Uploading drop...");

        // Present loading screen
        final ProgressDialog ringProgressDialog = ProgressDialog.show(PostDropActivity.this, "Just a sec...", "Dropping your drop...", true);
        ringProgressDialog.setCancelable(false);

        // Parse out hashtags
        String []tags = textView.getText().toString().split("\\s+");
        String tagsString = "";
        for (int i = 0; i < tags.length; i++) {
            if(tags[i].contains("#")) {
                tagsString = tagsString + " " + tags[i];
            }
        }

        // Convert image to string
        String imageString = "";
        if(imageBitmap != null) {
            imageString = BitMapToString(imageBitmap);
        }

        // Upload to database
        Map<String, Object> drop = new HashMap<String, Object>();
        drop.put("image", imageString);
        drop.put("lat", currentLocation.getLatitude());
        drop.put("lon", currentLocation.getLongitude());
        drop.put("tags", tagsString);
        drop.put("text", textView.getText().toString());

        dropFirebase.child("drops").push().setValue(drop, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                ringProgressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Your drop has been dropped!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    public void imagePressed(View view) {
        Log.e("DEBUG", "Launching camera...");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }


    // BITMAP SERIALIZATION HELPERS

    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream ByteStream=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, ByteStream);
        byte [] b=ByteStream.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    public Bitmap StringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

}
