package com.notifications.server;


import com.notifications.server.notifiers.AmazonNotifier;
import com.notifications.server.notifiers.AppleNotifier;
import com.notifications.server.notifiers.GoogleNotifier;
import com.notifications.server.notifiers.WindowsNotifier;

import java.util.LinkedList;
import java.util.List;

/**
 * The sample class showing how you can send push notifications for different "providers", such as APNS, FCM, ADM and WNS.
 */
class PushNotificator {

    /**
     * Sends a push notification to every registered device.
     */
    static int notifyAll(int id, String title, String text, String serverMessage, String notificationProfile, int badge) throws Throwable {
        return notifyItems(id, Registrator.items(), title, text, serverMessage, notificationProfile, badge);
    }

    /**
     * Sends a push notification to every device in <c>items</c> list.
     */
    private static int notifyItems(int id, List<Registrator.Item> items, String title, String text, String serverMessage, String notificationProfile, int badge) {
        int notified = 0;

        LinkedList<Registrator.Item> fcmItems = new LinkedList<>();
        LinkedList<Registrator.Item> admItems = new LinkedList<>();
        LinkedList<Registrator.Item> apnsItems = new LinkedList<>();
        LinkedList<Registrator.Item> wnsItems = new LinkedList<>();

        for (Registrator.Item item : items) {
            System.out.println(" --- RECORRIENDO ITEM REGISTRADO-------");
            if ("FCM".equals(item.provider) || "GooglePlay".equals(item.provider)) {
                fcmItems.add(item);
            } else if ("ADM".equals(item.provider) || "Amazon".equals(item.provider)) {
                admItems.add(item);
            } else if ("APNS".equals(item.provider) || "iOS".equals(item.provider)) {
                apnsItems.add(item);
            } else if ("WNS".equals(item.provider) || "Windows".equals(item.provider)) {
                wnsItems.add(item);
            }
        }

        notified += new GoogleNotifier().notifyFCM(id, fcmItems, title, text, serverMessage, notificationProfile, badge);
        notified += new AmazonNotifier().notifyADM(id, admItems, title, text, serverMessage, notificationProfile, badge);
        notified += new AppleNotifier().notifyAPNS(id, apnsItems, title, text, serverMessage, notificationProfile, badge);
        notified += new WindowsNotifier().notifyWNS(id, wnsItems, title, text, serverMessage, notificationProfile, badge);

        return notified;
    }

}
