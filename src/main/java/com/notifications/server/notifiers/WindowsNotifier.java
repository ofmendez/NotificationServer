package com.notifications.server.notifiers;

import com.notifications.server.Registrator;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class WindowsNotifier extends Notifier {

    private static final String WINDOWS_PACKAGE_SID = null;
    private static final String WINDOWS_CLIENT_SECRET = null;

    /// <summary>
    /// Sends a push notification to Windows/Windows Phone devices.
    /// </summary>
    /// <remarks>
    /// Note that in order to correctly support Unicode characters, <c>title</c>, <c>text</c> and any other text values should be URL-encoded:
    /// <c>title = java.net.URLEncoder.encode(title);</c>
    /// <c>text = java.net.URLEncoder.encode(text);</c>
    /// <c>serverMessage = java.net.URLEncoder.encode(serverMessage);</c><br><br>
    /// See also: https://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx
    /// </remarks>
    public int notifyWNS(int id, List<Registrator.Item> items, String title, String text, String serverMessage, String notificationProfile, int badge) {
        if (items == null || items.size() == 0) {
            return 0;
        }

        title = encode(title);
        text = encode(text);
        serverMessage = encode(serverMessage);

        String token = oauth2GetAuthToken("Windows", "https://login.live.com/accesstoken.srf", "notify.windows.com", WINDOWS_PACKAGE_SID, WINDOWS_CLIENT_SECRET, false);

        boolean tokenUpdated = false;
        int notifiedCount = 0;
        for (Registrator.Item it : items) {
            try {
                String regId = notifyWindows(token, it.getId(), id, title, text, serverMessage, notificationProfile, badge);

                if (!tokenUpdated && TOKEN_EXPIRED.equals(Integer.toString(id))) {
                    token = oauth2GetAuthToken("Windows", "https://login.live.com/accesstoken.srf", "notify.windows.com", WINDOWS_PACKAGE_SID, WINDOWS_CLIENT_SECRET, true);
                    tokenUpdated = true;

                    regId = notifyWindows(token, it.getId(), id, title, text, serverMessage, notificationProfile, badge);
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

    //Returns updated registrationId if changed (never happens in the current version of WNS though), TOKEN_EXPIRED if expired and null otherwise
    private String notifyWindows(String accessToken, String registrationId, int id, String title, String text, String serverMessage, String notificationProfile, int badge) {
        URL url = createURL(registrationId);

        if (!url.getHost().endsWith(".notify.windows.com")) {
            throw new SecurityException("Unexpected WNS channel URI: " + registrationId);
        }

        String requestDataString = prepareData(id, title, text, serverMessage, notificationProfile, badge).toString();
        HttpsURLConnection connection = openConnection(url);
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-length", String.valueOf(requestDataString.length()));
            connection.setRequestProperty("X-WNS-Type", "wns/raw");
            connection.setRequestProperty("X-WNS-Cache-Policy", "cache");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(requestDataString);
            output.close();

        } catch (IOException ex) {
            throw new IllegalStateException("can not read response from url: " + url, ex);
        }

        int responseCode = getResponseCode(connection);
        if (responseCode != 200) {
            if (responseCode == 401 || responseCode == 410) {
                //If a 401 response code was received, the access token has expired. The token should be refreshed
                //and this request may be retried.
                return TOKEN_EXPIRED;
            }

            String errorContent = readStreamAsString(connection.getErrorStream() != null ? connection.getErrorStream() : getInputStream(connection));
            throw new RuntimeException(String.format("ERROR: The request failed with a %d response code, with the following message: %s", responseCode, errorContent));
        } else {
            //Success! Current version of WNS never sends the changed registrationId, so no need to check a response content.
            System.out.println("    WNS response:");
            System.out.println("    " + readStreamAsString(getInputStream(connection)));

            return null;
        }
    }


    private int getResponseCode(HttpsURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            throw new IllegalStateException("can not read response code: ", e);
        }
    }
}
