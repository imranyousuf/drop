package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;


public class SignUp extends Activity {

    Firebase firebase;
    EditText email;
    EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        firebase = new Firebase("https://dropdatabase.firebaseio.com");

        setContentView(R.layout.activity_sign_up);

        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);

    }

    public void signUpPressed(View view) {
        // TODO: Filter out unacceptable usernames and passwords
        firebase.createUser(email.getText().toString(), password.getText().toString(), new Firebase.ResultHandler() {
            @Override
            public void onSuccess() { // User was creased successfully
                launchMakeProfile();
            }

            @Override
            public void onError(FirebaseError firebaseError) { // User was not created
                Toast.makeText(getApplicationContext(), "Something went wrong, please try again :/", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchMakeProfile() {
        Intent intent = new Intent(this, MakeProfile.class);
        intent.putExtra("EMAIL", email.getText().toString());
        intent.putExtra("PASSWORD", password.getText().toString());
        finish();
        startActivity(intent);
    }


}
