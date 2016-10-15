package com.notifications.server;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

class SocketProcessor implements Runnable {

    SocketProcessor(Socket socket) throws Throwable {
        this.socket = socket;
        this.is = socket.getInputStream();
        this.os = socket.getOutputStream();
    }

    public void run() {
        try {
            read();
            process();
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                writeResponse("500 ERROR", t.toString());
            } catch (Throwable tt) {
                tt.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            socket = null;
        }
    }

    private void writeResponse(String status, String content) throws Throwable {
        if (content == null) {
            content = "EMPTY";
        }

        String response = "HTTP/1.1 " + status + "\r\n" +
                "Server: UTNotificationsDemoServer\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "Connection: close\r\n\r\n";
        String result = response + content;
        os.write(result.getBytes());
        os.flush();

        System.out.println("  >> " + result.replace("\n", "\n  >> "));
    }

    private void read() throws Throwable {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String requestString = br.readLine();
        if (requestString == null || !requestString.contains(" ")) {
            System.out.println("Unexpected request: " + requestString);
            writeResponse("500 ERROR", "Unexpected request: " + requestString);
            return;
        }

        request = requestString.split("\\ ")[1];

        int contentLength = 0;

        while (true) {
            String s = br.readLine();

            if (s == null || s.trim().length() == 0) {
                break;
            } else if (s.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(s.substring(15).trim());
            }
        }

        if (contentLength > 0) {
            char[] buff = new char[contentLength];
            int offset = 0;
            while (contentLength > 0) {
                int read = br.read(buff, offset, contentLength);
                if (read < 0) {
                    break;
                }

                offset += read;
                contentLength -= read;
            }

            content = new String(buff);
        }

        if (request.contains("?")) {
            String[] pair = request.split("\\?");
            request = pair[0];

            if (pair[1] != null && !pair[1].isEmpty()) {
                if (content != null) {
                    content += "&" + pair[1];
                } else {
                    content = pair[1];
                }
            }
        }
    }

    private void process() throws Throwable {
        if (request == null) {
            return;
        }

        System.out.println("\n<< " + request + " " + content);

        switch (request) {
            case "/register": {
                HashMap<String, String> argsMap = conentAsArgumentsMap();
                Registrator.register(argsMap.get("uid"), argsMap.get("provider"), argsMap.get("id"));
                writeResponse("200 OK", "Registered!");
            }
            break;

            case "/notify": {
                HashMap<String, String> argsMap = conentAsArgumentsMap();
                int id = -1;
                if (argsMap.containsKey("id")) {
                    try {
                        id = Integer.parseInt(argsMap.get("id"));
                    } catch (Throwable e) {
                        throw new IllegalStateException("error: ", e);
                    }
                }

                String title = argsMap.get("title");
                String text = argsMap.get("text");
                String notificationProfile = argsMap.containsKey("notification_profile") ? argsMap.get("notification_profile") : null;

                int badge = -1;
                if (argsMap.containsKey("badge")) {
                    try {
                        badge = Integer.parseInt(argsMap.get("badge"));
                    } catch (Throwable e) {
                        throw new IllegalStateException("error: ", e);
                    }
                }

                int count = PushNotificator.notifyAll(id, title, text, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), notificationProfile, badge);
                writeResponse("200 OK", "Notified " + count + " clients!" + (count > 0 ? "" : "\nHave you really initialized push notifications using UTNotifications.Manager.Instance.InitializePushNotifications()?"));
            }
            break;

            default: {
                String href = "/notify?id=1&title=Sample Title&text=Sample Text&badge=1";
                writeResponse("200 OK", "Demo UTNotifications server.<br>Use <a href=\"" + href + "\">" + href + "</a> to send notifications for all registered clients");
            }
        }
    }

    private HashMap<String, String> conentAsArgumentsMap() throws UnsupportedEncodingException {
        HashMap<String, String> resultMap = new HashMap<>();

        if (content != null && content.contains("=")) {
            String[] args = content.split("&");
            for (String arg : args) {
                String[] pair = arg.split("=");
                resultMap.put(java.net.URLDecoder.decode(pair[0], "UTF-8"), java.net.URLDecoder.decode(pair[1], "UTF-8"));
            }
        }

        return resultMap;
    }

    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private String request;
    private String content;
}
