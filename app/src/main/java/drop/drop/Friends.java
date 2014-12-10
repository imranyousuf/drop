package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class Friends extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_friends);
    }

    // Mimic native back button functionality. Seems a little slow right now.
    public void closeActivity(View view) {
        this.finish();
    }

    public void startAddFriendsActivity(View view) {
        Intent intent = new Intent(this, AddFriends.class);
        startActivity(intent);
    }
}
