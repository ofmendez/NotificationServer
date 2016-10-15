package com.notifications.server.notifiers;


import com.notifications.server.Registrator;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class GoogleNotifier extends Notifier {

    private static final String FIREBASE_SERVER_KEY = "AIzaSyB6jGuOqSovJyyuWWZuSxkZ_9P1AS1dlNQ";

    /// <summary>
    /// Sends a push notification to Google Play featured devices.
    /// </summary>
    /// <remarks>
    /// Note that in order to correctly support Unicode characters, <c>title</c>, <c>text</c> and any other text values should be URL-encoded:
    /// <c>title = java.net.URLEncoder.encode(title);</c>
    /// <c>text = java.net.URLEncoder.encode(text);</c>
    /// <c>serverMessage = java.net.URLEncoder.encode(serverMessage);</c>
    /// See also: https://firebase.google.com/docs/cloud-messaging/http-server-ref#downstream
    /// </remarks>
    public int notifyFCM(int id, List<Registrator.Item> items, String title, String text, String serverMessage, String notificationProfile, int badge) {
        System.out.println(" ENTRA AL FCM");
        if (items == null || items.size() == 0) {
            System.out.println(" ERRORES O NO HAY ITEMS O NO ENCUENTRA EL KEY");
            return 0;
        }

        title = encode(title);
        text = encode(text);
        serverMessage = encode(serverMessage);

        //Request data json by default should look like:
        /*
        {
			"registration_ids":["<id1>", ...], <or "to":"id1",>
			"data":
			{
				"title":"<Title>",
				"text":"<Text>",
				["id":<int id>,]
				["badge_number":<int badge>,]
				["<User data key 1>":"<User data value 1>",
				...]
			}
		}
		*/
        JSONObject requestData = new JSONObject();

        //Multiple ids are sent in "registration_ids" array, single one in "to" string field
        if (items.size() > 1) {
            JSONArray registrationIds = new JSONArray();
            for (Registrator.Item it : items) {
                registrationIds.put(it.getId());
            }
            requestData.put("registration_ids", registrationIds);
        } else {
            requestData.put("to", items.get(0).getId());
        }

        requestData.put("data", prepareData(id, title, text, serverMessage, notificationProfile, badge));
        String requestDataString = requestData.toString();
        String httpsURL = "https://fcm.googleapis.com/fcm/send";
        URL url = createURL(httpsURL);
        HttpsURLConnection connection = openConnection(url);

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-length", String.valueOf(requestDataString.length()));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "key=" + FIREBASE_SERVER_KEY);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(requestDataString);
            output.close();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                String errorContent = readStreamAsString(connection.getErrorStream());
                throw new RuntimeException(String.format("ERROR: The request failed with a %d response code, with the following message: %s", responseCode, errorContent));
            } else {
                System.out.println("    FCM response:");
                System.out.println("    " + readStreamAsString(connection.getInputStream()));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("can not read response from url: " + url, ex);
        }

        return items.size();
    }

}
