package com.notifications.server;

import java.io.*;
import java.util.*;

/// <summary>
/// Very simple registration of devices on a server side (storing pairs of push notifications system provider & registrationId).
/// </summary>
/// <remarks>
/// You should use some database instead in production.
/// </remarks>
public class Registrator {

    private static String DB_FILE_NAME;
    private static final Map<String, Item> m_registration;
    private static final Map<String, OAuth2Token> m_oath2Tokens;

    static {
        Map<String, Item> registration = new HashMap<>();
        Map<String, OAuth2Token> oath2Tokens = new HashMap<>();

        try (FileInputStream fileStream = new FileInputStream(DB_FILE_NAME = "utnotifications_reg.db");
             ObjectInputStream stream = new ObjectInputStream(fileStream)) {

            registration = readObjectAsHashMap(stream);
            oath2Tokens = readObjectAsHashMap(stream);
        } catch (FileNotFoundException e) {
            //It's OK!
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            m_registration = registration != null ? registration : new HashMap<>();
            m_oath2Tokens = oath2Tokens != null ? oath2Tokens : new HashMap<>();
        }
    }

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        final String provider;
        private String m_id;

        Item(String provider, String id) {
            this.provider = provider;
            m_id = id;
        }

        public String getId() {
            return m_id;
        }

        public void setId(String id) {
            m_id = id;
            save();
        }
    }

    static void register(String uid, String provider, String id) {
        synchronized (m_registration) {
            m_registration.put(uid, new Item(provider, id));
            save();
        }
    }

    static List<Item> items() {
        synchronized (m_registration) {
            return new ArrayList<>(m_registration.values());
        }
    }

    public static String getOAuth2Token(String provider) {
        if (m_oath2Tokens.containsKey(provider)) {
            return m_oath2Tokens.get(provider).getToken();
        } else {
            return null;
        }
    }

    public static void setOAuth2Token(String provider, String token, Date tokenExpires) {
        m_oath2Tokens.put(provider, new OAuth2Token(token, tokenExpires));
        save();
    }


    private static void save() {
        ObjectOutputStream stream = null;
        try {
            FileOutputStream fileStream = new FileOutputStream(DB_FILE_NAME);
            stream = new ObjectOutputStream(fileStream);

            stream.writeObject(m_registration);
            stream.writeObject(m_oath2Tokens);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private static class OAuth2Token implements Serializable {
        private static final long serialVersionUID = 1L;

        final String token;
        final Date tokenExpires;

        OAuth2Token(String token, Date tokenExpires) {
            this.token = token;
            this.tokenExpires = tokenExpires;
        }

        String getToken() {
            if (tokenExpires != null && tokenExpires.after(new Date())) {
                return token;
            } else {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> readObjectAsHashMap(ObjectInputStream o) {
        try {
            return (HashMap<K, V>) o.readObject();
        } catch (ClassNotFoundException e) {
            return new HashMap<>();
        } catch (IOException e) {
            throw new IllegalStateException("can not read object: ", e);
        }
    }
}
