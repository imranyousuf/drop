package drop.drop;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.Exception;import java.lang.Integer;import java.lang.Override;import java.lang.Runnable;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.ResourceStoreException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import java.io.InputStream;
import java.lang.String;import java.lang.System;import java.lang.Throwable;import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import drop.drop.ToqBroadcastReceiver;


public class ToqActivity extends Activity {

    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    static DeckOfCardsManager mDeckOfCardsManager;
    static RemoteDeckOfCards mRemoteDeckOfCards;
    static RemoteResourceStore mRemoteResourceStore;
    ListCard listCard;
    SimpleTextCard simpleTextCard;
    private CardImage[] mCardImages;
    private ToqBroadcastReceiver toqReceiver;
    HashMap<String,String> fsmCardImages;
    private DeckOfCardsManagerListener deckOfCardsManagerListener;
    private DeckOfCardsEventListener deckOfCardsEventListener = new DeckOfCardsEventListenerImpl();
    private ToqAppStateBroadcastReceiver toqAppStateReceiver;
    private ViewGroup notificationPanel;
    private ViewGroup deckOfCardsPanel;
    View installDeckOfCardsButton;
    View uninstallDeckOfCardsButton;
    private TextView statusTextView;
    private Context mContext;


    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception{

        try{
            //Context context = getApplicationContext();
            // AssetManager assetManager = context.getAssets();


            InputStream is= mContext.getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            e.printStackTrace();
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = ToqActivity.this;
        //getApplicationContext();
        setContentView(R.layout.activity_main);
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new ToqBroadcastReceiver();
        init();
        setupUI();
    }

    /**
     * @see android.app.Activity#onStart()
     * This is called after onCreate(Bundle) or after onRestart() if the activity has been stopped
     */
    protected void onStart(){
        super.onStart();

        Log.d(Constants.TAG, "ToqApiDemo.onStart");
        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }





    public void setupUI() {

        install();

        /*

        findViewById(R.id.flash_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                install();

            }
        });

        */
    }

    private void sendNotification() {

        //First find the location
        Set<String> keys = fsmCardImages.keySet();
        String[] keysArray = new String[0];
        keysArray = keys.toArray(keysArray);

        // get a random key to get a random value
        Random rand = new Random();
        String randomKey = keysArray[rand.nextInt(keysArray.length)];
        String myRandomValues = fsmCardImages.get(randomKey);
        System.out.println(myRandomValues);

        //then send notification
        String[] message = new String[2];

        message[0] = randomKey + " welcomes you at Sproul!";
        message[1] = "Find me on your watch's FSM Poster app and see further instructions.";
        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "FSM Poster App", message);

        // Draw divider between lines of text
        notificationCard.setShowDivider(true);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
            Toast.makeText(this, "Sent Notification", Toast.LENGTH_SHORT).show();
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send Notification", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Installs applet to Toq watch if app is not yet installed
     */
    public void install() {
        boolean isInstalled = true;

        //try {

            //XXX
            //isInstalled = mDeckOfCardsManager.isInstalled();
            // Get the launcher icons
        DeckOfCardsLauncherIcon whiteIcon = null;
        DeckOfCardsLauncherIcon colorIcon = null;
        try {
            whiteIcon = new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
            colorIcon = new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't get launcher icon");
            return;
        }

        mCardImages = new CardImage[6];
        try {
            //mCardImages[0]= new CardImage("card.image.1", getBitmap("image1.png"));
            mCardImages[0] = new CardImage("card.image.1", getBitmap("baez.png"));
            mCardImages[1] = new CardImage("card.image.2", getBitmap("goldberg.png"));
            mCardImages[2] = new CardImage("card.image.3", getBitmap("jgoldberg.png"));
            mCardImages[3] = new CardImage("card.image.4", getBitmap("rossman.png"));
            mCardImages[4] = new CardImage("card.image.5", getBitmap("savio.png"));
            mCardImages[5] = new CardImage("card.image.6", getBitmap("weinberg.png"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't get picture icon");
            return;
        }
        // Try to retrieve a stored deck of cards
        try {
            // If there is no stored deck of cards or it is unusable, then create new and store
            /**  if ((mRemoteDeckOfCards = getStoredDeckOfCards()) == null){
             mRemoteDeckOfCards = createDeckOfCards();
             storeDeckOfCards();
             }**/
        } catch (Throwable th) {
            th.printStackTrace();
            mRemoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (mRemoteDeckOfCards == null) {
            mRemoteDeckOfCards = createDeckOfCards();
        }

            // Set the custom launcher icons, adding them to the resource store
            mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});



        //XXX
        /*catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }*/

        try {
            mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            Log.v("RR", "I am here!");



        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
        }
/*
        if (!isInstalled) {
            try {
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
                Log.v("RR", "I am here!");


            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }
*/
        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void uninstall() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
           // sendNotification();
        }

        if (isInstalled) {
            try{
                mDeckOfCardsManager.uninstallDeckOfCards();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, "error uninstalling cards", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "already uninstalled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds a deck of cards to the applet
     */
    private void addSimpleTextCard() {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        int currSize = listCard.size();

        // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
        SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(currSize+1));

        simpleTextCard.setHeaderText("Header: " + Integer.toString(currSize+1));
        simpleTextCard.setTitleText("Title: " + Integer.toString(currSize+1));
        String[] messages = {"Message: " + Integer.toString(currSize+1)};
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(false);
        try{
            CardImage cardImage= simpleTextCard.getCardImage(mRemoteResourceStore);

        }
        catch (ResourceStoreException e){
            Log.w(Constants.TAG, "ToqApiDemo.initUI - an error occurred retrieving the card image", e);
        }
        simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[2]);

        simpleTextCard.setShowDivider(true);

        listCard.add(simpleTextCard);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to Create SimpleTextCard", Toast.LENGTH_SHORT).show();
        }
    }


    private void removeDeckOfCards() {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        if (listCard.size() == 0) {
            return;
        }

        listCard.remove(0);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to delete Card from ListCard", Toast.LENGTH_SHORT).show();
        }

    }

    //
    private CardImage parseCardImageFromUI(int selectedItemPos){

        switch (selectedItemPos){

            case 1:
                return mCardImages[0];

            case 2:
                return mCardImages[1];

            case 3:
                return mCardImages[2];
            case 4:
                return mCardImages[3];

            default:
                return null;

        }

    }
    // Initialise
    private void init() {

        fsmCardImages = new HashMap<String, String>();
        Log.v("RR", "init function!");

        fsmCardImages.put("Mario Savio", "Express your view of free speech in a drawing");
        fsmCardImages.put("Joan Baez", "Draw a megaphone");
        fsmCardImages.put("Art Goldberg", "Draw Now");
        fsmCardImages.put("Michael Rossman", "Draw Free Speech");
        fsmCardImages.put("Jack Weinberg", "Draw FSM");
        fsmCardImages.put("Jackie Goldberg", "Draw SLATE");

        // Create the resource store for icons and images
        mRemoteResourceStore = new RemoteResourceStore();


        // Panels
        notificationPanel= (ViewGroup)findViewById(R.id.notification_panel);
        deckOfCardsPanel= (ViewGroup)findViewById(R.id.doc_panel);

        setChildrenEnabled(deckOfCardsPanel, false);
        setChildrenEnabled(notificationPanel, false);
        installDeckOfCardsButton = findViewById(R.id.flash_button);
        // Create the state receiver
        toqAppStateReceiver= new ToqAppStateBroadcastReceiver();
        // Register toq app state receiver
        deckOfCardsManagerListener= new DeckOfCardsManagerListenerImpl();
        deckOfCardsEventListener= new DeckOfCardsEventListenerImpl();
        mDeckOfCardsManager.addDeckOfCardsManagerListener(deckOfCardsManagerListener);
        mDeckOfCardsManager.addDeckOfCardsEventListener(deckOfCardsEventListener);


        // Status
        Log.v("RR", "get status!");
        statusTextView= (TextView)findViewById(R.id.status_text);
        statusTextView.setText("Initialised");

        registerToqAppStateReceiver();

        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){

            setStatus(getString(R.string.status_connecting));

            Log.d(Constants.TAG, "ToqApiDemo.onStart - not connected, connecting...");

            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, getString(R.string.error_connecting_to_service), Toast.LENGTH_SHORT).show();
                Log.e(Constants.TAG, "ToqApiDemo.onStart - error connecting to Toq app service", e);
            }

        }
        else{
            Log.d(Constants.TAG, "ToqApiDemo.onStart - already connected");
            setStatus(getString(R.string.status_connected));
            refreshUI();
        }



    }
    // Set status bar message
    private void setStatus(String msg){
        statusTextView.setText(msg);
    }

    // Register state receiver
    private void registerToqAppStateReceiver(){
        IntentFilter intentFilter= new IntentFilter();
        intentFilter.addAction(Constants.BLUETOOTH_ENABLED_INTENT);
        intentFilter.addAction(Constants.BLUETOOTH_DISABLED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_PAIRED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_UNPAIRED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_CONNECTED_INTENT);
        intentFilter.addAction(Constants.TOQ_WATCH_DISCONNECTED_INTENT);
        getApplicationContext().registerReceiver(toqAppStateReceiver, intentFilter);
    }
    // Connected to Toq app service, so refresh the UI
    private void refreshUI(){

        try{

            // If Toq watch is connected
            if (mDeckOfCardsManager.isToqWatchConnected()){

                // If the deck of cards applet is already installed
                if (mDeckOfCardsManager.isInstalled()){
                    Log.d(Constants.TAG, "ToqApiDemo.refreshUI - already installed");
                    updateUIInstalled();
                }
                // Else not installed
                else{
                    Log.d(Constants.TAG, "ToqApiDemo.refreshUI - not installed");
                    updateUINotInstalled();
                }

            }
            // Else not connected to the Toq app
            else{
                Log.d(Constants.TAG, "ToqApiDemo.refreshUI - Toq watch is disconnected");
                Toast.makeText(ToqActivity.this, getString(R.string.intent_toq_watch_disconnected), Toast.LENGTH_SHORT).show();
                disableUI();
            }

        }
        catch (RemoteDeckOfCardsException e){
            Toast.makeText(this, getString(R.string.error_checking_status), Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "ToqApiDemo.refreshUI - error checking if Toq watch is connected or deck of cards is installed", e);
        }

    }

    // Disable all UI components
    private void disableUI(){
        // Disable everything
        setChildrenEnabled(deckOfCardsPanel, false);
        setChildrenEnabled(notificationPanel, false);
    }
    // Set up UI for when deck of cards applet is already installed
    private void updateUIInstalled(){

//        // Panels
//        notificationPanel= (ViewGroup)findViewById(R.id.notification_panel);
//        deckOfCardsPanel= (ViewGroup)findViewById(R.id.doc_panel);

        // Enable everything
        setChildrenEnabled(deckOfCardsPanel, true);
        setChildrenEnabled(notificationPanel, true);

        // Install disabled; update, uninstall enabled
        installDeckOfCardsButton.setEnabled(false);

        uninstallDeckOfCardsButton.setEnabled(true);


    }

    // Set up UI for when deck of cards applet is not installed
    private void updateUINotInstalled(){

        // Disable notification panel
        setChildrenEnabled(notificationPanel, false);

        // Enable deck of cards panel
        setChildrenEnabled(deckOfCardsPanel, true);



        // Install enabled; update, uninstall disabled
        installDeckOfCardsButton.setEnabled(true);

        uninstallDeckOfCardsButton.setEnabled(false);

        // Focus
        installDeckOfCardsButton.requestFocus();
    }







    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{

        if (!isValidDeckOfCards()){
            Log.w(Constants.TAG, "Stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    /**
     * Uses SharedPreferences to store the deck of cards
     * This is mainly used to
     */
    private void storeDeckOfCards() throws Exception{
        Log.v("RR", "storeDeckofCards!");
        // Retrieve and hold the contents of PREFS_FILE, or create one when you retrieve an editor (SharedPreferences.edit())
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Create new editor with preferences above
        SharedPreferences.Editor editor = prefs.edit();
        // Store an encoded string of the deck of cards with key DECK_OF_CARDS_KEY
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(mRemoteDeckOfCards));
        // Store the version code with key DECK_OF_CARDS_VERSION_KEY
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        // Commit these changes
        editor.commit();
    }

    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Return 0 if DECK_OF_CARDS_VERSION_KEY isn't found
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        return deckOfCardsVersion >= Constants.VERSION_CODE;
    }

    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        //create simpletextcard here
        //listcard must have 6 diff cards (header and image)
        listCard= new ListCard();
        int i=0;


        /*----remove the first two blank cards


        simpleTextCard= new SimpleTextCard("card");
        listCard.add(simpleTextCard);

        simpleTextCard= new SimpleTextCard("card1");
        listCard.add(simpleTextCard);

        return new RemoteDeckOfCards(this, listCard);*/

        for (Map.Entry<String,String> entry : fsmCardImages.entrySet()) {


            String key = entry.getKey();
            String value = entry.getValue();
            simpleTextCard = new SimpleTextCard(Integer.toString(i));
            try {

                mRemoteResourceStore.addResource(mCardImages[i]);

                simpleTextCard.setCardImage(mRemoteResourceStore,mCardImages[i]);

            }
            catch (Exception e){
                e.printStackTrace();
            }
            simpleTextCard.setHeaderText(key);
            simpleTextCard.setTitleText(value);
            simpleTextCard.setReceivingEvents(true);
            simpleTextCard.setShowDivider(true);

            listCard.add(simpleTextCard);
            Log.v("RR", "card added!");

            i++;
        }


        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards,mRemoteResourceStore);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Failed to Create SimpleTextCard", Toast.LENGTH_SHORT).show();
        }
        return new RemoteDeckOfCards(this, listCard);

    }




    // Enable/Disable a view group's children and nested children
    private void setChildrenEnabled(ViewGroup viewGroup, boolean isEnabled){

        for (int i = 0; i < viewGroup.getChildCount();  i++){

            View view= viewGroup.getChildAt(i);

            if (view instanceof ViewGroup){
                setChildrenEnabled((ViewGroup)view, isEnabled);
            }
            else{
                view.setEnabled(isEnabled);
            }

        }

    }
    // Toq app state receiver
    private class ToqAppStateBroadcastReceiver extends BroadcastReceiver {


        /**
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        public void onReceive(Context context, Intent intent){
            Log.v("RR", "in broadcastreceiver!");

            String action= intent.getAction();

            if (action == null){
                Log.w(Constants.TAG, "ToqApiDemo.ToqAppStateBroadcastReceiver.onReceive - action is null, returning");
                return;
            }

            Log.d(Constants.TAG, "ToqApiDemo.ToqAppStateBroadcastReceiver.onReceive - action: " + action);

            // If watch is now connected, refresh UI
            if (action.equals(Constants.TOQ_WATCH_CONNECTED_INTENT)){
                Toast.makeText(ToqActivity.this, getString(R.string.intent_toq_watch_connected), Toast.LENGTH_SHORT).show();
                refreshUI();
            }
            // Else if watch is now disconnected, disable UI
            else if (action.equals(Constants.TOQ_WATCH_DISCONNECTED_INTENT)){
                Toast.makeText(ToqActivity.this, getString(R.string.intent_toq_watch_disconnected), Toast.LENGTH_SHORT).show();
                disableUI();
            }

        }

    }
    // Handle service connection lifecycle and installation events
    private class DeckOfCardsManagerListenerImpl implements DeckOfCardsManagerListener{

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onConnected()
         */
        public void onConnected(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_connected));
                    refreshUI();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onDisconnected()
         */
        public void onDisconnected(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_disconnected));
                    disableUI();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onInstallationSuccessful()
         */
        public void onInstallationSuccessful(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_installation_successful));
                    updateUIInstalled();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onInstallationDenied()
         */
        public void onInstallationDenied(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_installation_denied));
                    updateUINotInstalled();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onUninstalled()
         */
        public void onUninstalled(){
            runOnUiThread(new Runnable(){
                public void run(){
                    setStatus(getString(R.string.status_uninstalled));
                    updateUINotInstalled();
                }
            });
        }

    }


    // Handle card events triggered by the user interacting with a card in the installed deck of cards
    private class DeckOfCardsEventListenerImpl implements DeckOfCardsEventListener {



        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardOpen(java.lang.String)
         */
        public void onCardOpen(final String cardId){
            System.out.println("RR in card open event!");
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, "card opened" + cardId, Toast.LENGTH_SHORT).show();

                    //Switch to drawing activity --
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                    startActivity(intent);


                }
            });
        }
        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardVisible(java.lang.String)
         */
        public void onCardVisible(final String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, "card visible" + cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardInvisible(java.lang.String)
         */
        public void onCardInvisible(final String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, "card invisible" + cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardClosed(java.lang.String)
         */
        public void onCardClosed(final String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, "card closed" + cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, "menu option selected" + cardId + " [" + menuOption + "]", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption, final String quickReplyOption){
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ToqActivity.this, "menu option selected" + cardId + " [" + menuOption + ":" + quickReplyOption +
                            "]", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

}


