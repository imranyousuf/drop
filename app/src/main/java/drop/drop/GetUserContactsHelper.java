package drop.drop;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.HashSet;

/**
 * Created by Short on 12/10/2014.
 */
public class GetUserContactsHelper {

    final static String DELIMITERS = "\\+|\\(|\\)|-|\\s";

    // Get the user contacts and format them to match the format used by our database
    public static HashSet<String> getUserContacts(ContentResolver cr) {
        HashSet<String> contactsList = new HashSet<String>();
        //ContentResolver cr = getContentResolver();
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
                            String formattedPhoneNo = phoneNo.replaceAll(DELIMITERS, "");
                            // Remove the '1' in front of the number
                            if (formattedPhoneNo.charAt(0) == '1') {
                                formattedPhoneNo = formattedPhoneNo.substring(1);
                            }
                            contactsList.add(formattedPhoneNo);
                        }
                    }
                    pCur.close();
                }
            }
        }
        return contactsList;
    }

}
