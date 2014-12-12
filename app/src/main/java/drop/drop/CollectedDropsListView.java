package drop.drop;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;

import java.util.Date;

/**
 * Created by kyledillon on 12/11/14.
 */
public class CollectedDropsListView extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> dropKeys;
    Firebase firebase;
    private ArrayList<Drop> drops;

    public CollectedDropsListView(Activity context, ArrayList<String> dropKeys) {
        super(context, R.layout.collected_drop, dropKeys);

        this.context = context;
        this.dropKeys = dropKeys;

        firebase = new Firebase("https://dropdatabase.firebaseio.com");

        // Init array of drops
        drops = new ArrayList<Drop>();
        for(int i = 0; i < dropKeys.size() ; i++) {
            Drop drop = new Drop();
            drops.add(drop);
        }

        retrieveDrops();
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.collected_drop, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.collected_text);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.collected_photo);
        ImageView profileView = (ImageView) rowView.findViewById(R.id.collected_profile_photo);
        ProgressBar spinner = (ProgressBar) rowView.findViewById(R.id.collected_spinner);
        TextView date = (TextView) rowView.findViewById(R.id.collected_date);

        final Drop drop = drops.get(position);
        if(drop.getImageBitmap() != null) {
            imageView.setImageBitmap(drop.getImageBitmap());

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy hh:mm a");
            String dateString =sdf.format(new Date(drop.getEpoch()));
            date.setText(dateString);
        }
        if(drop.getUsername() != "") {
            txtTitle.setText(drop.getUsername());
        }
        if(drop.getProfileBitmap() != null) {
            profileView.setImageBitmap(drop.getProfileBitmap());
        }

        // Spin the spinner if there is still more content
        if(drop.getImageBitmap() != null &&
                drop.getUsername() != "" &&
                drop.getProfileBitmap() != null) {
            spinner.setVisibility(View.INVISIBLE);
        } else {
            spinner.animate();
            spinner.setVisibility(View.VISIBLE);
        }

        return rowView;
    }

    private void retrieveDrops() {
        // Get User UID
        SharedPreferences prefs = context.getSharedPreferences("drop", context.MODE_PRIVATE);
        String UID = prefs.getString("uid", null); // User UID

        for(final String dropKey : dropKeys) {
            firebase.child("drops").child(dropKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Map<String, Object> dropMap = (Map<String, Object>) snapshot.getValue();
                    Drop drop = drops.get(dropKeys.indexOf(dropKey)); // keep the index of the two arrays lined up
                    drop.setEpoch((Long) dropMap.get("epoch"));
                    drop.setPublic((Boolean) dropMap.get("public"));
                    drop.setDropperUID((String) dropMap.get("dropperUID"));
                    drop.setLat((Double) dropMap.get("lat"));
                    drop.setLon((Double) dropMap.get("lon"));
                    drop.setImageKey((String) dropMap.get("imageKey"));

                    drops.set(dropKeys.indexOf(dropKey), drop);

                    retrieveProfilePhoto(drop, dropKeys.indexOf(dropKey));
                    retrieveUsername(drop, dropKeys.indexOf(dropKey));
                    retrievePhoto(drop, dropKeys.indexOf(dropKey));
                }
                @Override
                public void onCancelled(FirebaseError firebaseError) {}
            });
        }
    }

    private void retrievePhoto(Drop drop, final int index) {
        firebase.child("photos").child(drop.getImageKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String imageString = (String) snapshot.getValue();
                Drop drop1 = drops.get(index);
                drop1.setImageBitmap(ImageHelper.StringToBitMap(imageString));
                drops.set(index, drop1);
                updateListView();
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void retrieveUsername(Drop drop, final int index) {
        firebase.child("users").child(drop.getDropperUID()).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String username = (String) snapshot.getValue();
                Drop drop1 = drops.get(index);
                drop1.setUsername(username);
                drops.set(index, drop1);
                updateListView();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    private void retrieveProfilePhoto(Drop drop, final int index) {
        firebase.child("users").child(drop.getDropperUID()).child("profile_picture").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String profilePhoto = (String) snapshot.getValue();
                Bitmap profileBitmap = ImageHelper.StringToBitMap(profilePhoto);
                Drop drop1 = drops.get(index);
                drop1.setProfileBitmap(profileBitmap);
                drops.set(index, drop1);
                updateListView();
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void updateListView() {
        this.notifyDataSetChanged();
    }
}
