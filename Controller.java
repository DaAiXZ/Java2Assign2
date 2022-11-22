package application.controller;

import application.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private static final int PLAY_1 = 1;
    private static final int PLAY_2 = 2;
    private static final int EMPTY = 0;
    private static final int BOUND = 90;
    private static final int OFFSET = 15;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    @FXML
    public Label role;

    @FXML
    public Button button;

    @FXML
    private Pane base_square;

    @FXML
    private Rectangle game_panel;

    private static boolean TURN = false;

    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    private static int playerNum = 0;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Socket socket;
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("Can't connect to server");
            alert.showAndWait();
            System.exit(0);
        }


        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String message = dataInputStream.readUTF();
                    System.out.println(message);

                    if (message.contains("playerNum")){
                        playerNum = Integer.parseInt(message.split(" ")[1]);
                        if (playerNum == PLAY_1) {
                            role.setText("Wait for the player to enter (0/1)");
                            button.setDisable(true);
                            TURN = true;
                        }else {
                            role.setText("Wait for the homeowner to start (1/1)");
                            button.setText("Ready");
                            button.setDisable(true);
                            TURN = false;
                        }
                    }

                    else if (message.equals("join")) {
                        dataOutputStream.writeUTF("join");
                        dataOutputStream.flush();
                        Platform.runLater(this::joinToAddEventAction);

                    }else if (message.equals("start")) {
                        if (playerNum == 2){
                            dataOutputStream.writeUTF("start");
                            dataOutputStream.flush();
                            Platform.runLater(() -> role.setText("Game start"));
                        }

                        Platform.runLater(this::addEventAction);
                    }else if (message.equals("opponentLeft")){
                      Platform.runLater(()->{
                          role.setText("Your opponent has exited the room");
                      });
                    } else if (message.contains("p")){
                        int x = Integer.parseInt(message.split(" ")[1]);
                        int y = Integer.parseInt(message.split(" ")[2]);
                        int player = Integer.parseInt(message.split(" ")[3]);
                        Platform.runLater(() -> {
                            if ( refreshBoard(x,y,player) ){
                                if (checkWin()){
                                    role.setText("You lose!");
                                }else if (checkEnd()){
                                    role.setText("tie");
                                } else {
                                    role.setText("Your turn");
                                    TURN = true;
                                }
                            }
                        });


                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();


    }


    private void joinToAddEventAction(){
        role.setText("Players have already joined (1/1)");
        button.setText("Start");
        button.setDisable(false);
        button.setOnMouseClicked(event -> {
            try {
                dataOutputStream.writeUTF("start");
                dataOutputStream.flush();
                button.setDisable(true);
                role.setText("Game start");
                addEventAction();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void addEventAction(){
        game_panel.setOnMouseClicked(event -> {
            int x = (int) (event.getX() / BOUND);
            int y = (int) (event.getY() / BOUND);
            if (checkWin()){
                System.out.println("Win"+(TURN?PLAY_1:PLAY_2));
            }else if (checkEnd()){
                System.out.println("End");
            }else if (TURN && refreshBoard(x, y, playerNum)) {
                TURN = !TURN;
                try {
                    dataOutputStream.writeUTF("p " + x + " " + y + " " + playerNum);
                    System.out.println( x + " " + y + " " + playerNum);
                    dataOutputStream.flush();
                    if (checkWin()){
                        role.setText("you win!");
                    }else if (checkEnd()){
                        role.setText("tie!");
                    }else {
                        role.setText("opponent's turns");
                    }

                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        });
    }

    private boolean refreshBoard (int x, int y,int player) {
        if (chessBoard[x][y] == EMPTY) {
            chessBoard[x][y] = player;
            drawChess();
            return true;
        }
        return false;
    }

    private void drawChess () {
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard[0].length; j++) {
                if (flag[i][j]) {
                    // This square has been drawing, ignore.
                    continue;
                }
                switch (chessBoard[i][j]) {
                    case PLAY_1:
                        drawCircle(i, j);
                        break;
                    case PLAY_2:
                        drawLine(i, j);
                        break;
                    case EMPTY:
                        // do nothing
                        break;
                    default:
                        System.err.println("Invalid value!");
                }
            }
        }
    }

    private void drawCircle (int i, int j) {
        Circle circle = new Circle();
        base_square.getChildren().add(circle);
        circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
        circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
        circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
        circle.setStroke(Color.RED);
        circle.setFill(Color.TRANSPARENT);
        flag[i][j] = true;
    }

    private void drawLine (int i, int j) {
        Line line_a = new Line();
        Line line_b = new Line();
        base_square.getChildren().add(line_a);
        base_square.getChildren().add(line_b);
        line_a.setStartX(i * BOUND + OFFSET * 1.5);
        line_a.setStartY(j * BOUND + OFFSET * 1.5);
        line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
        line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_a.setStroke(Color.BLUE);

        line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
        line_b.setStartY(j * BOUND + OFFSET * 1.5);
        line_b.setEndX(i * BOUND + OFFSET * 1.5);
        line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_b.setStroke(Color.BLUE);
        flag[i][j] = true;
    }

    private boolean checkWin () {
        for (int i = 0; i < chessBoard.length; i++) {
            if (chessBoard[0][i] ==  chessBoard[1][i] && chessBoard[1][i] == chessBoard[2][i] && chessBoard[0][i] != EMPTY) {
                return true;
            }
            if (chessBoard[i][0] ==  chessBoard[i][1] && chessBoard[i][1] == chessBoard[i][2] && chessBoard[i][0] != EMPTY) {
                return true;
            }
        }
        if (chessBoard[0][0] ==  chessBoard[1][1] && chessBoard[1][1] == chessBoard[2][2] && chessBoard[0][0] != EMPTY) {
            return true;
        }
        if (chessBoard[0][2] ==  chessBoard[1][1] && chessBoard[1][1] == chessBoard[2][0] && chessBoard[0][2] != EMPTY) {
            return true;
        }
        return false;

    }

    private boolean checkEnd(){
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard[0].length; j++) {
                if (chessBoard[i][j] == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }
}
