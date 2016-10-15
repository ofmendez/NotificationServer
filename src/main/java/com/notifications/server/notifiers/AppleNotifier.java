package com.notifications.server.notifiers;

import com.notifications.server.Registrator;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;

public class AppleNotifier extends Notifier {

    private static ApnsService m_apnsService = null;
    private static final String APN_CERT_PATH = null;
    private static final String APN_CERT_PASSWORD = null;

    /// <summary>
    /// Sends a push notification to iOS devices. com.notnoop.apns library is used for it (its source code is provided).
    /// </summary>
    /// <remarks>
    /// Note that iOS Registration Ids are considered as Base64- or HEX- encoded APNS tokens (which are originally binary buffers).
    /// <seealso cref="UTNotifications.Manager.OnSendRegistrationId"/>
    /// See also: https://developer.apple.com/library/ios/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/Chapters/CommunicatingWIthAPS.html.
    /// </remarks>
    public int notifyAPNS(int id, List<Registrator.Item> items, String title, String text, String serverMessage, String notificationProfile, int badge) {
        if (items.size() == 0 || APN_CERT_PATH == null || APN_CERT_PASSWORD == null) {
            return 0;
        }

        if (m_apnsService == null) {
            try {
                //Creates and sets up the ApnsService instance. Note that you don't need to use withSandboxDestination() in production.
                m_apnsService = APNS.newService().withCert(APN_CERT_PATH, APN_CERT_PASSWORD).withSandboxDestination().build();
            } catch (Throwable e) {
                System.err.println("Unable to initialize ApnsService. Please check if APN_CERT_PATH & APN_CERT_PASSWORD values are correct");
                throw e;
            }
        }

        List<byte[]> tokens = new ArrayList<>(items.size());
        for (Registrator.Item item : items) {
            tokens.add(decodeToken(item.getId()));
        }

        String sound;
        if (notificationProfile != null && !notificationProfile.isEmpty()) {
            sound = "Data/Raw/" + notificationProfile;
        } else {
            sound = "default";
        }

        PayloadBuilder payload = APNS.newPayload()
                .alertBody(title + "\n" + text)
                .actionKey(title)
                .sound(sound)
                .customField("server_message", serverMessage)
                .badge(badge);

        if (id >= 0) {
            payload.customField("id", Integer.toString(id));
        }

        if (badge >= 0) {
            payload.badge(badge);
        }

        m_apnsService.push(tokens, payload.buildBytes());

        return items.size();
    }

    private static byte[] decodeToken(String token) {
        if (token == null) {
            return null;
        } else if (token.endsWith("=")) {
            return DatatypeConverter.parseBase64Binary(token);
        } else {
            return hexStringToByteArray(token);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
