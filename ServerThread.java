package application.server;

import application.Main;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerThread extends Thread {
  private final Socket socket;
  private final int id;
  private final int playerNum;

  private DataInputStream dataInputStream;
  private DataOutputStream dataOutputStream;

  private ServerThread opponent;
  private List<ServerThread> room;

  public ServerThread(Socket socket, int id) {
    this.socket = socket;
    this.id = id;

    if (Server.clients.size() == 0) {
      this.playerNum = 1;
      room = new ArrayList<>();
      room.add(this);
      Server.rooms.add(room);
    } else {
      room = findRoom();
      if (room != null) {
        this.playerNum = 2;
        room.add(this);
      } else {
        this.playerNum = 1;
        room = new ArrayList<>();
        room.add(this);
        Server.rooms.add(room);
      }
    }
  }

  public List<ServerThread> findRoom() {
    for (List<ServerThread> room : Server.rooms) {
      if (room.size() == 1) {
        return room;
      }
    }
    return null;
  }

  @Override
  public void run() {
    super.run();
    System.out.println("Client " + id + " connected");

    boolean canStart = false;
    boolean isStarted = false;

    try {
      dataInputStream = new DataInputStream(socket.getInputStream());
      dataOutputStream = new DataOutputStream(socket.getOutputStream());

      dataOutputStream.writeUTF("playerNum " + playerNum);
      dataOutputStream.flush();

      if (playerNum == 2) {
        opponent = room.get(0);
        opponent.sendMessage("join");
        canStart = true;
      }

      while (!canStart) {
        String message = dataInputStream.readUTF();
        if (message.equals("join")) {
          opponent = room.get(1);
          canStart = true;
        }
      }

      while (!isStarted) {
        String message = dataInputStream.readUTF();
        if (message.equals("start")) {
          opponent.sendMessage(message);
          isStarted = true;
        }
      }

      while (isStarted) {
        String message = dataInputStream.readUTF();
        if (message.equals("opponentLeft")) {
          isStarted = false;
        } else {
          opponent.sendMessage(message);
        }

      }
    } catch (Exception e) {
      if (opponent != null) {
        opponent.sendMessage("opponentLeft");
        opponent = null;
      }

      room.remove(this);
      Server.clients.remove(this);
      Server.rooms.remove(room);
    }
  }

  public void sendMessage(String message) {
    try {
      dataOutputStream.writeUTF(message);
      dataOutputStream.flush();
    } catch (Exception e) {
      if (opponent != null) {
        opponent.sendMessage("opponentLeft");
        opponent = null;
      }

      room.remove(this);
      Server.clients.remove(this);
    }
  }
}

