package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


public class CollectedDrops extends Activity {

    ListView list;
    Firebase firebase;
    ArrayList<String> dropKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_collected_drops);

        dropKeys = new ArrayList<String>();

        firebase = new Firebase("https://dropdatabase.firebaseio.com");

        retrieveDropKeys();


    }

    private void retrieveDropKeys() {
        // Get User UID
        SharedPreferences prefs = getSharedPreferences("drop", MODE_PRIVATE);
        String UID = prefs.getString("uid", null); // User UID

        firebase.child("users").child(UID).child("dropsCollected").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> drops = (Map<String, Object>) snapshot.getValue();
                ArrayList<String> dropsList = new ArrayList(drops.values());
                Collections.sort(dropsList);
                Collections.reverse(dropsList);
                for(String dropKeyString : dropsList) {
                    if(!dropKeys.contains(dropKeyString)) {
                        dropKeys.add(dropKeyString);
                    }
                    if(dropKeys.size() > 10) break; // Dont get more than this many old drops
                }

                CollectedDropsListView adapter = new CollectedDropsListView(CollectedDrops.this, dropKeys);
                list=(ListView)findViewById(R.id.list);
                list.setAdapter(adapter);
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    public void backPressed(View view) {
        super.onBackPressed();
    }

}
