package chat;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chat Server
 * ===========
 * Listens on PORT, spawns a ClientHandler thread per connection,
 * and broadcasts messages to all connected clients.
 */
public class Server {

    public static final int PORT = 12345;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("===========================================");
        System.out.println("  Chat Server started on port " + PORT);
        System.out.println("===========================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[+] New client: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, clients);
                clients.add(handler);

                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();

                System.out.println("[i] Active clients: " + clients.size());
            }
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) c.sendMessage(message);
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[-] Client left. Active: " + clients.size());
    }
}
