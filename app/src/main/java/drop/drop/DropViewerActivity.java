package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;


public class DropViewerActivity extends Activity {

    String dropKey;
    ImageView image;
    TextView text;
    Drop drop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        Bundle extras = getIntent().getExtras();
        dropKey = extras.getString("dropKey");

        drop = new Drop();

        setContentView(R.layout.activity_drop_viewer);

        image = (ImageView) findViewById(R.id.drop_photo);
        text = (TextView) findViewById(R.id.drop_text);

        pullDropFromDatabase(dropKey);
    }

    private void pullDropFromDatabase(String key) {
        Firebase.setAndroidContext(this);
        Firebase dropFirebase = new Firebase("https://dropdatabase.firebaseio.com/");
        Firebase dropRef = dropFirebase.child("drops").child(key);

        dropRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                if(snapshot.getKey().equals("key")) drop.setKey((String)snapshot.getValue());
                else if(snapshot.getKey().equals("image")) {
                    drop.setImage((String)snapshot.getValue());
                    image.setImageBitmap(ImageHelper.StringToBitMap(drop.getImage()));
                }
                else if(snapshot.getKey().equals("lat")) drop.setLat((Double)snapshot.getValue());
                else if(snapshot.getKey().equals("lon")) drop.setLon((Double)snapshot.getValue());
                else if(snapshot.getKey().equals("tags")) drop.setTags((String)snapshot.getValue());
                else if(snapshot.getKey().equals("text")) {
                    drop.setText((String)snapshot.getValue());
                    text.setText(drop.getText());
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_drop_viewer, menu);
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
}
