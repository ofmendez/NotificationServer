package com.notifications.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Very simple HTTP server that manages devices registration and push notifications requests.
 * <p>
 * You will use your game or specific notifications server instead in production.
 */
public class HttpServer {

    private static int PORT = 8080;

    public static void main(String[] args) throws Throwable {
        System.out.println("The demo server is running as http://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new SocketProcessor(socket)).start();
            }
        }
    }

}