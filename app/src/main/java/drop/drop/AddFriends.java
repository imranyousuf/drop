package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;


import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.client.core.SyncTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Short on 12/8/2014.
 */
public class AddFriends extends Activity {

    Firebase firebase;
    HashSet<String> contactList;
    String formattedPhoneNo;
    final String DELIMITERS = "\\+|\\(|\\)|-|\\s";
    ArrayList<Map<String, Object>> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_add_friends);

        userList = new ArrayList<Map<String, Object>>();

        initFirebase();
        getUserContacts();
        // get our database contacts if key (number) in contactsList then do something




    }

    // Get the user contacts and format them to match the format used by our database
    private void getUserContacts() {
        contactList = new HashSet<String>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                // We could store names and numbers in a dictionary if we ever needed that
                // String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        // If the number stored isn't long enough to be in our database then don't bother.
                        if (phoneNo.length() >= 10) {
                            // Use regex to remove '+', spaces, '(', and ')' and '-' form user contacts to match database format
                            formattedPhoneNo = phoneNo.replaceAll(DELIMITERS, "");
                            // Remove the '1' in front of the number
                            if (formattedPhoneNo.charAt(0) == '1') {
                                formattedPhoneNo = formattedPhoneNo.substring(1);
                            }
                            contactList.add(formattedPhoneNo);
                        }
                    }
                    pCur.close();
                }
            }
        }
    }

    // Mimic native back button functionality.
    public void closeActivity(View view) {
        this.finish();
    }

    private void initFirebase() {
        firebase = new Firebase("https://dropdatabase.firebaseio.com");
        firebase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object value = snapshot.getValue();
                Map<String, Object> users = (Map<String, Object>) snapshot.getValue();
                for(Object user : users.values()) {
                    Map<String, Object> userMap = (Map<String, Object>) user; // save this
                    if(!userList.contains(userMap)) {
                        userList.add(userMap);
                    }
                }
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }
}
