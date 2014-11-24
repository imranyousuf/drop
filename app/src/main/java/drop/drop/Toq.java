package drop.drop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Toast;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;

import java.io.InputStream;
import java.util.Iterator;

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



    public Toq() {
        // Get the reference to the deck of cards manager
        deckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        deckOfCardsEventListener = new mDeckOfCardsEventListener();

        // Creates a resource storage for card icons
        remoteResourceStore = new RemoteResourceStore();

        // Get the card images
        try {
            cardImages = new CardImage[2];

            cardImages[0] = new CardImage("card.image.1", getBitmap("drop_icon.png"));
            cardImages[1] = new CardImage("card.image.2", getBitmap("drop_icon_get.png"));

        }
        catch (Exception e) {
            Toast.makeText(this, "Error occurred retrieving card images", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            remoteDeckOfCards = createDeckOfCards();
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

    }

    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception {

        try {
            InputStream in = getAssets().open(fileName);
            return BitmapFactory.decodeStream(in);
        }
        catch (Exception e) {
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    // Create Drop cards for receiving notifications to pick up or ignore a Drop
    private RemoteDeckOfCards createDeckOfCards() {
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
    private class mDeckOfCardsEventListener implements DeckOfCardsEventListener {

        public void onCardOpen(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // if current location is within proximity of drop in database?

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
            });}


        public void onMenuOptionSelected(final String cardId, final String menuOption) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });}

        public void onMenuOptionSelected(final String cardId, final String menuOption, final String quickReplyOption) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // Do nothing
                }
            });}

    }



}
