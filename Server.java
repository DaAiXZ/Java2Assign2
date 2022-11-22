package application.server;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Server {

  private static final int PORT = 8888;
  public static List<ServerThread> clients = new ArrayList<>();
  public static List<List<ServerThread>> rooms = new ArrayList<>();

  public static void main(String[] args) throws Exception {

    ServerSocket serverSocket = new ServerSocket(PORT);
    while (true) {
      ServerThread serverThread = new ServerThread(serverSocket.accept(), clients.size());
      clients.add(serverThread);
      serverThread.start();
    }
  }
}
