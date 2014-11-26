package drop.drop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Toast;

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

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Short on 11/23/2014.
 */
public class Toq {

    private DeckOfCardsManager deckOfCardsManager;
    private DeckOfCardsEventListener deckOfCardsEventListener;
    private RemoteDeckOfCards remoteDeckOfCards;
    private RemoteResourceStore remoteResourceStore;
    private CardImage[] cardImages;
    private ListCard listCard;
    private SimpleTextCard simpleTextCard;

    public Toq(Context context) {
        // Get the reference to the deck of cards manager
        deckOfCardsManager = DeckOfCardsManager.getInstance(context);
        // Commented out CardEventListener to Push Code
        //deckOfCardsEventListener = new mDeckOfCardsEventListener();

        // Creates a resource storage for card icons
        remoteResourceStore = new RemoteResourceStore();

        // Get the card images
        try {
            cardImages = new CardImage[2];

            cardImages[0] = new CardImage("card.image.1", getBitmap(context, "drop_icon.png"));
            cardImages[1] = new CardImage("card.image.2", getBitmap(context, "drop_icon_get.png"));

        }
        catch (Exception e) {
            Toast.makeText(context, "Error occurred retrieving card images", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            remoteDeckOfCards = createDeckOfCards(context);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }

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
        onStart();
    }

    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(Context context, String fileName) throws Exception {

        try {
            InputStream in = context.getAssets().open(fileName);
            return BitmapFactory.decodeStream(in);
        }
        catch (Exception e) {
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    // Create Drop cards for receiving notifications to pick up or ignore a Drop
    private RemoteDeckOfCards createDeckOfCards(Context context) {
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

        return new RemoteDeckOfCards(context, listCard);
    }

    // Send notificationCard to prompt user to pick up or ignore Drop
    public void sendNotification(Context context) {
        String[] nameList = {"Ignore", "Pick up"};
        // Initialize string reference for the cardId sent as a notification
        String notificationCardSent = nameList[1];

        String[] messageText = {notificationCardSent};

        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "Drop Found!", messageText);

        notificationCard.setVibeAlert(true);

        RemoteToqNotification notification = new RemoteToqNotification(context, notificationCard);

        try {
            deckOfCardsManager.sendNotification(notification);
        } catch (RemoteDeckOfCardsException e) {
            Toast.makeText(context, "Error sending notification", Toast.LENGTH_SHORT).show();
        }
    }

    // Update the deck of cards
    private void updateDeckOfCards(Context context) {

        try {
            deckOfCardsManager.updateDeckOfCards(remoteDeckOfCards, remoteResourceStore);
        }
        catch (RemoteDeckOfCardsException e) {
            Toast.makeText(context, "Error updating deck of cards", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onStart() {
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

    // Installs Drop applet on Toq
    public void install(View view) {
        try {
            deckOfCardsManager.installDeckOfCards(remoteDeckOfCards, remoteResourceStore);
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            try { // This is mostly for quick debugging purposes
                deckOfCardsManager.uninstallDeckOfCards();
                deckOfCardsManager.installDeckOfCards(remoteDeckOfCards, remoteResourceStore);
            } catch (RemoteDeckOfCardsException e2) {
                e2.printStackTrace();
            }
        }
    }

    // Handle card events triggered by the user interacting with a card in the installed deck of cards
    /*private class mDeckOfCardsEventListener implements DeckOfCardsEventListener {

        public void onCardOpen(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (cardId.equals(notificationCardSent)) {
                        View v = findViewById(R.id.relative_view);
                        v.setVisibility(View.GONE);

                    }
                }
            });
        }


        public void onCardVisible(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }


        public void onCardInvisible(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }


        public void onCardClosed(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }


        public void onMenuOptionSelected(final String cardId, final String menuOption) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }


        public void onMenuOptionSelected(final String cardId, final String menuOption, final String quickReplyOption) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });
        }

    }*/

    // Gets all of the Drops within a certain radius to be added to the current remoteDeckOfCards
    /*private void parseDatabase() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    // Initialize Card object to be added to Toq with the correct image dimensions
                    //final CardImage cardImage = new CardImage("new card",  Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, false));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Add a new card we downloaded from flickr to our remoteDeckOfCards
                            //simpleTextCard = new SimpleTextCard("flickr.card");
                            //simpleTextCard.setHeaderText("Flickr Card");
                            //simpleTextCard.setCardImage(remoteResourceStore, cardImage);

                            //listCard.add(simpleTextCard);
                            updateDeckOfCards();
                        }
                    });
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };
        thread.start();
    }*/

}
