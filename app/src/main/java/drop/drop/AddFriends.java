package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;


/**
 * Created by Short on 12/8/2014.
 */
public class AddFriends extends Activity {

    private HashSet<String> contactsList;
    private HashMap<String, Object> userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_add_friends);

        // key: user's number
        // values: user's drops, collected drops, username, and profile pic
        //userInfo = GetUserInfoFromFirebase.getUserInfo();

        ContentResolver cr = getContentResolver();
        contactsList = GetUserContactsHelper.getUserContacts(cr);
        //if (contactsList.contains("5412069958")) {
          //  System.out.println("Kyle in my phone contactsList");
        //}

        //if (userInfo.containsKey("5412069958")) {
          //  System.out.println("Kyle's number in our database");
        //}

    }

    // Mimic native back button functionality.
    public void closeActivity(View view) {
        this.finish();
    }

}
