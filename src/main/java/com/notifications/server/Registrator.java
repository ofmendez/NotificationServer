package com.notifications.server;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/// <summary>
/// Very simple registration of devices on a server side (storing pairs of push notifications system provider & registrationId).
/// </summary>
/// <remarks>
/// You should use some database instead in production.
/// </remarks>
class Registrator {

    private static String DB_FILE_NAME;
    private static final HashMap<String, Item> m_registration;
    private static HashMap<String, OAuth2Token> m_oath2Tokens;

    static class Item implements Serializable {
        final String provider;
        private String m_id;
        private static final long serialVersionUID = 1L;

        Item(String provider, String id) {
            this.provider = provider;
            m_id = id;
        }

        String getId() {
            return m_id;
        }

        void setId(String id) {
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

    static String getOAuth2Token(String provider) {
        if (m_oath2Tokens.containsKey(provider)) {
            return m_oath2Tokens.get(provider).getToken();
        } else {
            return null;
        }
    }

    static void setOAuth2Token(String provider, String token, Date tokenExpires) {
        m_oath2Tokens.put(provider, new OAuth2Token(token, tokenExpires));
        save();
    }

    //private
    static {
        ObjectInputStream stream = null;
        HashMap<String, Item> registration = null;
        HashMap<String, Registrator.OAuth2Token> oath2Tokens = null;

        try {
            FileInputStream fileStream = new FileInputStream(DB_FILE_NAME = "utnotifications_reg.db");
            stream = new ObjectInputStream(fileStream);

            registration = (HashMap<String, Item>) stream.readObject();
            oath2Tokens = (HashMap<String, Registrator.OAuth2Token>) stream.readObject();
        } catch (FileNotFoundException e) {
            //It's OK!
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            m_registration = registration != null ? registration : new HashMap<>();
            m_oath2Tokens = oath2Tokens != null ? oath2Tokens : new HashMap<>();

            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
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

        private static final long serialVersionUID = 1L;
    }

}
