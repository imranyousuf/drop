package drop.drop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;


public class FBFragment extends android.support.v4.app.Fragment {

    private UiLifecycleHelper uiHelper;

    public boolean LOGGED_IN = false;
    public boolean SHOWN = false; // Fragment is visible on screen.

    LoginButton authButton;
    TextView text;
    RelativeLayout topView;

    public MainActivity parentActivity; // Get reference to main activity to communicate with it.

    public static FBFragment newInstance() {
        FBFragment fragment = new FBFragment();

        return fragment;
    }

    public FBFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LOGGED_IN = false;

        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();

        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_fblogin, container, false);
        authButton = (LoginButton) view.findViewById(R.id.authButton);
        text = (TextView) view.findViewById(R.id.loginText);
        topView = (RelativeLayout) view.findViewById(R.id.loginView);
        authButton.setFragment(this);

        authButton.setUserInfoChangedCallback(new UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if(user != null) {
                    Log.e("DEBUG", "USER INFO CHANGED: User data exists");
                    if((SHOWN == true) && (LOGGED_IN == false)) {
                        // They just pressed Login Button.
                        parentActivity.hideFacebook();
                        parentActivity.composeDrop();
                    }
                    LOGGED_IN = true;
                } else {
                    Log.e("DEBUG", "USER INFO CHANGED: User data is null");
                    LOGGED_IN = false;
                }
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    public void loginPressed(View view) {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // null objects here
    }


    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            //Log.e("DEBUG", "SESSION STATE CHANGED: Logged in...");
        } else if (state.isClosed()) {
            //Log.e("DEBUG", "SESSION STATE CHANGED: Logged out...");
        }
    }

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

}
