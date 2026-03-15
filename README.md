# Distributed Chat System

Multi-client chat application built with Java TCP Sockets and Multithreading, featuring a Java Swing GUI.

## Features
- Real-time messaging between multiple clients
- Java Swing GUI with chat bubbles and online users sidebar
- One thread per client (ClientHandler implements Runnable)
- Broadcast messages to all connected users simultaneously
- Login dialog with username entry

## Project Structure
```
├── Server.java         # TCP server, manages client connections
├── ClientHandler.java  # One thread per client, handles I/O
└── ChatClient.java     # Swing GUI client
```

## How to Run
```bash
# 1. Compile
javac -d out Server.java ClientHandler.java ChatClient.java

# 2. Start server (Terminal 1)
java -cp out chat.Server

# 3. Launch client (Terminal 2, 3...)
java -cp out chat.ChatClient
```

## Tech Stack
Java · TCP Sockets · Multithreading · Java Swing · CopyOnWriteArrayList
