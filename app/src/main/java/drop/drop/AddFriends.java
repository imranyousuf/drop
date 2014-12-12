package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Created by Short on 12/8/2014.
 */
public class AddFriends extends Activity {

    private Firebase firebase;
    private ArrayList<UserInfo> userInfoList;
    private HashSet<String> contactsList;
    private ContentResolver cr;
    private HashSet<String> addFriendlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_add_friends);
        cr = getContentResolver();
        contactsList = GetUserContactsHelper.getUserContacts(cr);

        populateUserInfoList();

    }

    private void populateUserInfoList() {
        // Create list of items to be stored in our ListView from the database
        // This is really slow right now :(
        userInfoList = new ArrayList<UserInfo>();
        firebase = new Firebase("https://dropdatabase.firebaseio.com");
        firebase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                HashMap<String, Object> users = (HashMap<String, Object>) snapshot.getValue();
                for (Object user : users.values()) {
                    HashMap<String, Object> userMap = (HashMap<String, Object>) user;
                    String userNumber = (String) userMap.get("number");
                    if (contactsList.contains(userNumber)) {
                        if (!userInfoList.contains(userNumber)) {
                            String name = (String) userMap.get("username");
                            String pic = (String) userMap.get("profile_picture");
                            UserInfo info = new UserInfo(userNumber, name, pic);
                            userInfoList.add(info);
                        }
                    }
                }
                Collections.addAll(userInfoList);
                populateFriendsListView();
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                String message = "Server error. Refresh page";
                Toast.makeText(AddFriends.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFriendsListView() {
        // Build the adapter
        ArrayAdapter<UserInfo> adapter = new MyListAdapter();

        // Configure the list view
        ListView listView = (ListView) findViewById(R.id.add_friends_listview);
        listView.setAdapter(adapter);

        // Not important for Friends, but it will be for adding friends which basically mimics this implementation
        registerClickCallBack();
    }

    private class MyListAdapter extends ArrayAdapter<UserInfo> {
        public MyListAdapter() {
            super(
                    AddFriends.this,               // Context for the activity
                    R.layout.add_friends_view,      // Layout to use (we created it)
                    userInfoList                // Items (user photo & user name) to be displayed
            );
        }

        /**
         * @param position - position of which user in userInfoList we are displaying
         * @param convertView - a view we "might" want to work with
         * @param parent
         * @return
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Make sure we have a view to work with (may have been given null)
            View friendsView = convertView;
            if (friendsView == null) {
                friendsView = getLayoutInflater().inflate(R.layout.add_friends_view, parent, false);
            }

            // Find the user to work with for list view population
            UserInfo curUserInfo = userInfoList.get(position);

            // Find the user's profile picture and set it in the ImageView
            ImageView imageView = (ImageView) friendsView.findViewById(R.id.user_profile_picture);
            imageView.setImageBitmap(curUserInfo.getPicture());

            //imageView.setImageResource(curUserInfo.getPicture());

            // Find the user's name and set it in the TextView next to the user's profile picture
            TextView makeText = (TextView) friendsView.findViewById(R.id.user_name);
            makeText.setText(curUserInfo.getName());

            imageView = (ImageView) friendsView.findViewById((R.id.add_friend_button));
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.plus_icon));

            return friendsView;
        }
    }

    private void registerClickCallBack() {
        ListView listView = (ListView) findViewById(R.id.add_friends_listview);
        // setOnItemClickListener() listens for a click event for a particular item in our list view
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {

                UserInfo user = userInfoList.get(position);

                sendFriendRequest(user);

                String message = "Sending request to... " + user.getName();
                Toast.makeText(AddFriends.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFriendRequest(UserInfo user) {

        View friendsView = findViewById(R.id.add_friends_listview);


        ImageView imageView = (ImageView) friendsView.findViewById(R.id.add_friend_button);
        imageView.setImageDrawable(getResources().getDrawable(R.drawable.checkmark_icon));

    }


    // Mimic native back button functionality.
    public void closeActivity(View view) {
        this.finish();
    }

}
