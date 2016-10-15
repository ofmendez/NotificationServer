package com.notifications.server.notifiers;

import com.notifications.server.Registrator;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class AmazonNotifier extends Notifier {

    private static final String AMAZON_CLIENT_ID = null;
    private static final String AMAZON_CLIENT_SECRET = null;

    /// <summary>
    /// Sends a push notification to Amazon android devices.
    /// </summary>
    /// <remarks>
    /// Note that in order to correctly support Unicode characters, <c>title</c>, <c>text</c> and any other text values should be URL-encoded:
    /// <c>title = java.net.URLEncoder.encode(title);</c>
    /// <c>text = java.net.URLEncoder.encode(text);</c>
    /// <c>serverMessage = java.net.URLEncoder.encode(serverMessage);</c>
    /// See also: https://developer.amazon.com/public/apis/engage/device-messaging/tech-docs/06-sending-a-message
    /// </remarks>
    public int notifyADM(int id, List<Registrator.Item> items, String title, String text, String serverMessage, String notificationProfile, int badge) {
        if (items.size() == 0 || AMAZON_CLIENT_ID == null || AMAZON_CLIENT_SECRET == null) {
            return 0;
        }

        title = encode(title);
        text = encode(text);
        serverMessage = encode(serverMessage);

        String token = oauth2GetAuthToken("Amazon", "https://api.amazon.com/auth/O2/token", "messaging:push", AMAZON_CLIENT_ID, AMAZON_CLIENT_SECRET, false);

        boolean tokenUpdated = false;
        int notifiedCount = 0;
        for (Registrator.Item it : items) {
            try {
                String regId = amazonSendMessageToDevice(it.getId(), token, id, title, text, serverMessage, notificationProfile, badge);

                if (!tokenUpdated && TOKEN_EXPIRED.equals(Integer.toString(id))) {
                    token = oauth2GetAuthToken("Amazon", "https://api.amazon.com/auth/O2/token", "messaging:push", AMAZON_CLIENT_ID, AMAZON_CLIENT_SECRET, true);
                    tokenUpdated = true;

                    regId = amazonSendMessageToDevice(it.getId(), token, id, title, text, serverMessage, notificationProfile, badge);
                }

                ++notifiedCount;
                if (regId != null) {
                    it.setId(regId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return notifiedCount;
    }

    //Returns updated registrationId if changed, TOKEN_EXPIRED if expired and null otherwise
    private String amazonSendMessageToDevice(String registrationId, String accessToken, int id, String title, String text, String serverMessage, String notificationProfile, int badge) {
        //JSON payload representation of the message.
        JSONObject payload = new JSONObject();

        //Define the key/value pairs for your message content and add them to the
        //message payload.
        payload.put("data", prepareData(id, title, text, serverMessage, notificationProfile, badge));

        //Convert the message from a JSON object to a string.
        String payloadString = payload.toString();

        //Establish the base URL, including the section to be replaced by the registration
        //ID for the desired app instance. Because we are using String.format to create
        //the URL, the %1$s characters specify the section to be replaced.
        String admUrlTemplate = "https://api.amazon.com/messaging/registrations/%1$s/messages";

        URL admUrl = createURL(String.format(admUrlTemplate, registrationId));
        HttpsURLConnection connection = openConnection(admUrl);
        int responseCode = connectToUrlAndGetResponseCode(accessToken, payloadString, connection);

        //Check if we received a failure response, and if so, get the reason for the failure.
        if (responseCode != 200) {
            if (responseCode == 401) {
                //If a 401 response code was received, the access token has expired. The token should be refreshed
                //and this request may be retried.
                return TOKEN_EXPIRED;
            }

            String errorContent = readStreamAsString(connection.getErrorStream());
            throw new RuntimeException(String.format("ERROR: The request failed with a %d response code, with the following message: %s", responseCode, errorContent));
        } else {
            //The request was successful. The response contains the canonical Registration ID for the specific instance of your
            //app, which may be different that the one used for the request.

            String responseContent = readStreamAsString(getInputStream(connection));

            System.out.println("    ADM response:");
            System.out.println("    " + responseContent);

            JSONObject parsedObject = new JSONObject(responseContent);
            String canonicalRegistrationId = parsedObject.getString("registrationID");

            //Check if the two Registration IDs are different.
            if (!canonicalRegistrationId.equals(registrationId)) {
                //At this point the data structure that stores the Registration ID values should be updated
                //with the correct Registration ID for this particular app instance.
                return canonicalRegistrationId;
            }
        }

        return null;
    }

    private int connectToUrlAndGetResponseCode(String accessToken, String payloadString, HttpsURLConnection connection) {
        int responseCode;
        try {
            //Generate the HTTPS connection for the POST request. You cannot make a connection
            //over HTTP.
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            //Set the content type and accept headers.
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("X-Amzn-Type-Version ", "com.amazon.device.messaging.ADMMessage@1.0");
            connection.setRequestProperty("X-Amzn-Accept-Type", "com.amazon.device.messaging.ADMSendResult@1.0");

            //Add the authorization token as a header.
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            //Obtain the output stream for the connection and write the message payload to it.
            OutputStream os = connection.getOutputStream();
            os.write(payloadString.getBytes(), 0, payloadString.getBytes().length);
            os.flush();
            connection.connect();

            //Obtain the response code from the connection.
            responseCode = connection.getResponseCode();
        } catch (IOException ex) {
            throw new IllegalStateException("error processing request: ", ex);

        }
        return responseCode;
    }

}
