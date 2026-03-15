package chat;

import java.io.*;
import java.net.*;
import java.util.List;

/**
 * ClientHandler — one instance per connected client, runs in its own thread.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final List<ClientHandler> clients;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // First line sent by GUI client is the username
            username = in.readLine();
            if (username == null || username.isBlank()) username = "Anonymous";

            System.out.println("[+] User joined: " + username);
            Server.broadcast("SYSTEM:" + username + " joined the chat", this);
            out.println("SYSTEM:Welcome, " + username + "!");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) break;
                System.out.println("[MSG] " + username + ": " + message);
                Server.broadcast("MSG:" + username + ":" + message, this);
            }
        } catch (IOException e) {
            System.out.println("[!] Error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void disconnect() {
        Server.removeClient(this);
        if (username != null) {
            Server.broadcast("SYSTEM:" + username + " left the chat", this);
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public String getUsername() { return username; }
}
