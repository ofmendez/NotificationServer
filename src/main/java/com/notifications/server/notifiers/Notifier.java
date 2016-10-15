package com.notifications.server.notifiers;

import com.notifications.server.Registrator;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class Notifier {

    private static final String DEFAULT_CHARSET = "UTF-8";
    static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";

    String encode(String toEncode) {
        try {
            return java.net.URLEncoder.encode(toEncode, DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Can not enconde: " + toEncode, e);
        }
    }

    JSONObject prepareData(int id, String title, String text, String serverMessage, String notificationProfile, int badge) {
        /*
            "data":
			{
				"title":"<Title>",
				"text":"<Text>",
				["id":<int id>,]
				["badge_number":<int badge>,]
				["<User data key 1>":"<User data value 1>",
				...]
			}
		*/

        JSONObject data = new JSONObject();
        if (id >= 0) {
            data.put("id", Integer.toString(id));
        }
        data.put("title", title);
        data.put("text", text);
        data.put("server_message", serverMessage);
        if (notificationProfile != null && !notificationProfile.isEmpty()) {
            data.put("notification_profile", notificationProfile);
        }
        if (badge >= 0) {
            data.put("badge_number", Integer.toString(badge));
        }

        return data;
    }

    String oauth2GetAuthToken(String provider, String url, String scope, String clientId, String clientSecret, boolean forceUpdateAuthToken) {
        if (!forceUpdateAuthToken) {
            String token = Registrator.getOAuth2Token(provider);
            if (token != null) {
                return token;
            }
        }

        //Encode the body of your request, including your clientID and clientSecret values.
        String body = "grant_type=" + encode("client_credentials") + "&" +
                "scope=" + encode(scope) + "&" +
                "client_id=" + encode(clientId) + "&" +
                "client_secret=" + encode(clientSecret);


        String responseContent = doPost(body, createURL(url));

        //Create a new JSONObject to hold the access token and extract
        //the token from the response.
        JSONObject parsedObject = new JSONObject(responseContent);
        String accessToken = parsedObject.getString("access_token");
        int expiresIn = parsedObject.has("expires_in") ? parsedObject.getInt("expires_in") : (Integer.MAX_VALUE / 1000);
        expiresIn = Math.min(10, expiresIn - 10);    //It took some time to deliver the response

        Date tokenExpires = new Date();
        tokenExpires.setTime(tokenExpires.getTime() + expiresIn * 1000);

        Registrator.setOAuth2Token(provider, accessToken, tokenExpires);

        return accessToken;
    }

    private String doPost(String body, URL authUrl) {
        try {
            //Generate the HTTPS connection. You cannot make a connection over HTTP.
            HttpsURLConnection con = openConnection(authUrl);
            con.setDoOutput(true);
            con.setRequestMethod("POST");

            //Set the Content-Type header.
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Charset", DEFAULT_CHARSET);

            //Send the encoded parameters on the connection.
            OutputStream os = con.getOutputStream();
            os.write(body.getBytes(DEFAULT_CHARSET));
            os.flush();
            con.connect();

            //Convert the response into a String object.
            return readStreamAsString(con.getInputStream());
        } catch (IOException ex) {
            throw new IllegalStateException("Can not read response from url" + authUrl, ex);
        }
    }

    /**
     * Create a new URL object with the base URL for the access token request.
     *
     * @param url
     * @return
     */
    URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("malformed url: " + url, e);
        }
    }

    String readStreamAsString(InputStream in) {
        try (InputStreamReader inputStream = new InputStreamReader(in, DEFAULT_CHARSET);
             BufferedReader buff = new BufferedReader(inputStream);) {

            StringBuilder sb = new StringBuilder();
            String line = buff.readLine();
            while (line != null) {
                sb.append(line);
                line = buff.readLine();
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Can not read response ", ex);
        }
    }

    HttpsURLConnection openConnection(URL admUrl) {
        try {
            return (HttpsURLConnection) admUrl.openConnection();
        } catch (IOException e) {
            throw new IllegalStateException("can not open connection to : " + admUrl, e);
        }
    }

    InputStream getInputStream(HttpsURLConnection connection) {
        try {
            return connection.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("can not get InputStream ", e);
        }
    }
}
