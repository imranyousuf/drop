package drop.drop;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import java.io.InputStream;
import java.util.Iterator;


public class Toq extends Activity {

    private final static String DROP_PREFS_FILE = "drop_prefs_file";
    private final static String DECK_OF_CARDS_KEY = "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY = "deck_of_cards_version_key";

    private DeckOfCardsManager deckOfCardsManager;
    private DeckOfCardsEventListener deckOfCardsEventListener;
    private RemoteDeckOfCards remoteDeckOfCards;
    private RemoteResourceStore remoteResourceStore;
    private CardImage[] cardImages;
    private ListCard listCard;
    private SimpleTextCard simpleTextCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toq);

       // Intent intent =

        // Get the reference to the deck of cards manager
        deckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        deckOfCardsEventListener = new mDeckOfCardsEventListener();

        init();
    }

    public void init() {
        // Creates a resource storage for card icons
        remoteResourceStore = new RemoteResourceStore();

        DeckOfCardsLauncherIcon dropLogo = null;
        DeckOfCardsLauncherIcon dropButton = null;

        // Get the launcher icons
        try {

            dropLogo = new DeckOfCardsLauncherIcon("drop_logo.icon", getBitmap("card_image1.png"), DeckOfCardsLauncherIcon.WHITE);
            dropButton = new DeckOfCardsLauncherIcon("drop_button.icon", getBitmap("image1.png"), DeckOfCardsLauncherIcon.COLOR);

        } catch (Exception e) {
            Toast.makeText(this, "Error initializing launcher icons", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the card images
        try {
            cardImages = new CardImage[10];

            cardImages[0] = new CardImage("card.image.1", getBitmap("drop_logo.png"));
            cardImages[1] = new CardImage("card.image.2", getBitmap("drop_button.png"));

        }
        catch (Exception e) {
            Toast.makeText(this, "Error occurred retrieving card images", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try to retrieve a stored deck of cards
        try {

            // If there is no stored deck of cards or it is unusable, then create new and store
            if ((remoteDeckOfCards = getStoredDeckOfCards()) == null) {
                remoteDeckOfCards = createDeckOfCards();
                storeDeckOfCards();
            }

        } catch (Throwable th) {
            remoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (remoteDeckOfCards == null) {
            remoteDeckOfCards = createDeckOfCards();
        }

        // Set the custom launcher icons, adding them to the resource store
        remoteDeckOfCards.setLauncherIcons(remoteResourceStore, new DeckOfCardsLauncherIcon[]{dropLogo, dropButton});

        // Re-populate the resource store with any card images being used by any of the cards
        for (Iterator<Card> it = remoteDeckOfCards.getListCard().iterator(); it.hasNext(); ) {

            String cardImageId = ((SimpleTextCard) it.next()).getCardImageId();

            if ((cardImageId != null) && !remoteResourceStore.containsId(cardImageId)) {

                if (cardImageId.equals("card.image.1")) {
                    remoteResourceStore.addResource(cardImages[0]);
                } else if (cardImageId.equals("card.image.2")) {
                    remoteResourceStore.addResource(cardImages[1]);
                }
            }
        }
    }

    /**
     * @see android.app.Activity#onStart()
     */
    protected void onStart() {
        super.onStart();
        // Add the listeners
        deckOfCardsManager.addDeckOfCardsEventListener(deckOfCardsEventListener);

        // If not connected, try to connect
        if (!deckOfCardsManager.isConnected()) {
            try {
                deckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
            }
        }
    }

    // Store deck of cards
    private void storeDeckOfCards() throws Exception {
        SharedPreferences prefs = getSharedPreferences(DROP_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(remoteDeckOfCards));
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        //was editor.commit()
        editor.apply();
    }

    // Get stored deck of cards if one exists
    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception {

        if (!isValidDeckOfCards()) {
            return null;
        }

        SharedPreferences prefs = getSharedPreferences(DROP_PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr = prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null) {
            return null;
        } else {
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards() {

        SharedPreferences prefs = getSharedPreferences(DROP_PREFS_FILE, Context.MODE_PRIVATE);
        int deckOfCardsVersion = prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        if (deckOfCardsVersion < Constants.VERSION_CODE) {
            return false;
        }

        return true;
    }


    // Read an image from assets and return as a bitmap
    public Bitmap getBitmap(String fileName) throws Exception {

        try {
            InputStream in = getAssets().open(fileName);
            return BitmapFactory.decodeStream(in);
        }
        catch (Exception e) {
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    // Create Drop cards for receiving notifications to pick up or ignore a Drop
    public RemoteDeckOfCards createDeckOfCards() {
        listCard = new ListCard();

        // Ignore Drop
        simpleTextCard = new SimpleTextCard("Ignore Drop");
        simpleTextCard.setMessageText(new String[]{"Ignore Drop", "Drop hint: "});
        simpleTextCard.setCardImage(remoteResourceStore, cardImages[0]);
        simpleTextCard.setReceivingEvents(true);
        listCard.add(simpleTextCard);

        // Pick up Drop
        simpleTextCard = new SimpleTextCard("Get Drop");
        simpleTextCard.setMessageText(new String[]{"Get Drop", "Drop hint: "});
        simpleTextCard.setCardImage(remoteResourceStore, cardImages[1]);
        simpleTextCard.setReceivingEvents(true);
        listCard.add(simpleTextCard);

        return new RemoteDeckOfCards(this, listCard);
    }

    // Send notificationCard to prompt user to pick up or ignore Drop
    public void sendNotification() {
        String[] nameList = {"Ignore", "Pick up"};
        // Initialize string reference for the cardId sent as a notification
        String notificationCardSent = nameList[1];

        String[] messageText = {notificationCardSent};

        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "Drop Found!", messageText);

        notificationCard.setVibeAlert(true);

        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            deckOfCardsManager.sendNotification(notification);
        } catch (RemoteDeckOfCardsException e) {
            Toast.makeText(this, "Error sending notification", Toast.LENGTH_SHORT).show();
        }
    }

    // Update the deck of cards
    public void updateDeckOfCards() {

        try {
            deckOfCardsManager.updateDeckOfCards(remoteDeckOfCards, remoteResourceStore);
        }
        catch (RemoteDeckOfCardsException e) {
            Toast.makeText(this, "Error updating deck of cards", Toast.LENGTH_SHORT).show();
        }
    }

    // Installs Drop applet on Toq
    public void install(View view) {
        try {
            deckOfCardsManager.installDeckOfCards(remoteDeckOfCards, remoteResourceStore);
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            try { // This is mostly for quick debugging purposes
                //deckOfCardsManager.uninstallDeckOfCards();
                deckOfCardsManager.installDeckOfCards(remoteDeckOfCards, remoteResourceStore);
            } catch (RemoteDeckOfCardsException e2) {
                e2.printStackTrace();
            }
        }
    }

    // Handle card events triggered by the user interacting with a card in the installed deck of cards
    private class mDeckOfCardsEventListener implements DeckOfCardsEventListener {
        /**
         * Removes Install button from main activity view and brings DrawingView to focus
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardOpen(java.lang.String)
         */
        public void onCardOpen(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(Toq.this, "You opened a Drop", Toast.LENGTH_SHORT).show();
                }
            });
        }
        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardVisible(java.lang.String)
         */
        public void onCardVisible(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardInvisible(java.lang.String)
         */
        public void onCardInvisible(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(Toq.this, "Card invisible", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardClosed(java.lang.String)
         */
        public void onCardClosed(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
}
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption, final String quickReplyOption) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }

    }

}
