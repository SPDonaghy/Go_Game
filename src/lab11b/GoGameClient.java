package lab11b;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
//import lab11b.GoGameServer.GoGameServerService;
import lab11b.GoGameServer.MouseClickEventHandler;
import lab11b.GoGameServer.PenaltyTimer;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import lab11b.Shared_Elements.*;

/**
 * The GoGameClient class joins a simple game of GO
 * by connecting to a GoGameServer host
 * @author Sean Donaghy
 *
 */
public class GoGameClient extends Application {
	
	
	Pane root;
	DrawBoard board;
	
	private static boolean[][] added = new boolean[Const.NUMBER_OF_ROWS][Const.NUMBER_OF_ROWS];
	private static Ellipse[][] stones = new Ellipse[Const.NUMBER_OF_ROWS][Const.NUMBER_OF_ROWS];
	
	private static Text timeViolations;
	private int noOfTimeViolations = 0;
	private static PenaltyTimer timer = new GoGameClient().new PenaltyTimer();
	
	private final int REFRESH_LIMIT = 10; // 10/60 seconds
	private int refreshCountDown = REFRESH_LIMIT;
	private Refresh refreshCount;
	
	private TextField textField;
	private Button sitBtn;
	
	private static Text youText;
	private static Text oppText;
	
	private GoGameClientService clientService;

	private String color = "Black";
	
	private static boolean isTurn;
	/**
	 * Start method to draw the graphic interface and initialize
	 * the client thread
	 */
	@Override
	public void start(Stage primaryStage) {
		
		
		isTurn = true;
		
		clientService = new GoGameClient().new GoGameClientService();
		Thread t = new Thread(clientService);
		
		
		// Initialize all the stones to null;
		for(int row = 0; row < Const.NUMBER_OF_ROWS; row++) {
			for(int col = 0; col < Const.NUMBER_OF_ROWS; col++) {
				stones[row][col] = null;
			}
		}
		
		root = new Pane();
		drawAll();
	
		Scene scene = new Scene(root, Const.SCENE_WIDTH, Const.SCENE_HEIGHT);

		// Set up fresh and start
		refreshCount = new Refresh();
		refreshCount.start();
		
		primaryStage.setTitle("Go Game");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		t.start();
	}
	/** Draw everything on the interface
	 * 
	 */
	public void drawAll() {
		
		// Set up the top
		Text playerColorText = new Text(color);
		playerColorText.setFont(Font.font(20));
		
		HBox playerColorTextBox = new HBox(playerColorText);
		
		timeViolations = new Text("Time Violations: " + noOfTimeViolations);	
		youText = new Text("Your name: ");
		HBox playerInfo = new HBox(Const.BOARD_LENGTH / 2, timeViolations, youText);		
		
		board = new DrawBoard();
		
		// Layout text field and sit button
		textField = new TextField("Enter your name");	
		sitBtn = new Button("Sit");
		sitBtn.setOnAction(new ButtonEventHandler());
	
		oppText = new Text("Opponent name: ");
		HBox bottom = new HBox(10, textField, sitBtn, oppText);
		HBox.setMargin(oppText, new Insets(0, 0, 0, 100));
		
		VBox box = new VBox(10, playerColorTextBox, playerInfo, board, bottom);
		
		playerColorTextBox.setAlignment(Pos.CENTER);

		root.getChildren().addAll(box);
		
		root.setLayoutX(23);
			
	}
	/* Draw the board with squares
	 * 
	 */
	private class DrawBoard extends Group {
		
		public DrawBoard() {
			
			// Draw rows and columns of stones
			for(int row = 0; row < Const.NUMBER_OF_ROWS; row++) {
				for(int col = 0; col < Const.NUMBER_OF_ROWS; col++) {
					Rectangle r = new Rectangle(col * Const.SQUARE_LENGTH, row * Const.SQUARE_LENGTH, Const.SQUARE_LENGTH, Const.SQUARE_LENGTH);
					r.setFill(Color.GREEN);
					r.setStroke(Color.BLACK);
					
					r.setOnMouseClicked(new MouseClickEventHandler());
					
					this.getChildren().add(r);
					
				}
			}	
		}
	}
	/* Every 1/60 seconds, the system refreshes to add new stones to the board if any
	 * 
	 */
	private class Refresh extends AnimationTimer {
		
		@Override
		public void handle(long arg0) {
		
			refreshCountDown--;
			
			if(refreshCountDown == 0) {
			
				refreshCountDown = REFRESH_LIMIT;
			
				// Add the new stones to the board
				for(int row = 0; row < Const.NUMBER_OF_ROWS; row++) {
					for(int col = 0; col < Const.NUMBER_OF_ROWS; col++) {
						if(stones[row][col] != null && !added[row][col]) {
						
							board.getChildren().add(stones[row][col]);
							added[row][col] = true;
						}
					}
				}
			}
		}
		
	}
	/**
	 * This class handles the networking aspect of the application
	 * and establishes the connection with the opponent player
	 * @author Sean Donaghy
	 *
	 */
	private class GoGameClientService  implements Runnable{
		
		private static Socket s;
		private static DataOutputStream out;
		private static DataInputStream in;

		
		@Override
		public void run() {
			
			try {
				
				s = new Socket("REPLACE WITH IP",Const.PORT);
				in = new DataInputStream(s.getInputStream());
				out = new DataOutputStream(s.getOutputStream());
				System.out.println("Successful Connection Made");
				
				boolean done = false;
				while(!done) {
					
					String message = in.readUTF();
					
					receiveName(message);
					
					//message = in.readUTF();
					receivePlay(message);
				}
			}
			
			catch(IOException e) {
				
				e.printStackTrace();
			}
		}
		
		public DataOutputStream getDataOutputStream() {
			return out;
		}
		
		public static void sendName(String name,DataOutputStream out) throws IOException{
			out.writeUTF("PLAYER "+name);
			out.flush();
		}
		
		private void receiveName(String message) {
			
			String name;
			if(message.contains("PLAYER")) {
				
				name = message.substring(7);
				oppText.setText("Opponent name: "+name);
			}
		}
		public static void sendPlay(int x,int y)throws IOException {
			
			out.writeUTF("PLAY "+x+" "+y);
			out.flush();
		}
		/**
		 * Receive play needs to signal that it is now the reciever's turn
		 * Then it needs to read the other player's play and update the game
		 * @param message
		 */
		private void receivePlay(String message) {
			
			if(message.contains("PLAY")) {
				isTurn = true;
				
				Scanner playReader = new Scanner(message);
				playReader.useDelimiter(" ");
				int x=0;
				int y=0;
				
				playReader.next();
				x = playReader.nextInt();
				y = playReader.nextInt();
				
				int row = (int)(y/(int)Const.SQUARE_LENGTH);
				int col = (int)(x/(int)Const.SQUARE_LENGTH);
				
				Ellipse stone = new Ellipse(x+Const.SQUARE_LENGTH/2,y+Const.SQUARE_LENGTH/2,Const.STONE_RADIUS,Const.STONE_RADIUS);
				//The Server's stones are white
				stone.setFill(Color.WHITE);
				
				//update the stones arrays for the refresh to check
				stones[row][col] = stone;
				
				System.out.println("Play received from server");
				
				//start the timer as soon as the opponent's play is received
				timer.start();
			}
		}
		
	}
	/**
	 * This event handler both sets the name for the player and sends that name to the opponent
	 * through the output stream
	 * @author Sean Donaghy
	 *
	 */
	public class ButtonEventHandler implements EventHandler<ActionEvent>{
		@Override
		public void handle(ActionEvent e){
			
			try {
				
				if(e.getSource()==sitBtn) {
					youText.setText("Your name: "+textField.getText());
					GoGameClientService.sendName(textField.getText(),clientService.getDataOutputStream());
				}
			}
			
			catch(IOException exception) {
				
				exception.printStackTrace();
			}
		}
	}
	/**
	 * 
	 * @param x x position of the stone
	 * @param y y position of the stone
	 * @param color color of the stone
	 * @param row which row of the board the stone is in, beginning from 0
	 * @param col which column of the board the stone is in, beginning from 0
	 */
	private static void addStone(double x,double y,Color color,int row,int col) {
		
		Ellipse stone = new Ellipse(x,y,Const.STONE_RADIUS,Const.STONE_RADIUS);
		stone.setFill(color);
		
		//update the stones arrays for the refresh to check
		stones[row][col] = stone;
		isTurn = false;
		
		timer.stop();
		timer.reset();
		
	}
	/**
	 * Event handler for placing stones when squares on the board are selected
	 * @author Sean Donaghy
	 *
	 */
	public class MouseClickEventHandler implements EventHandler<MouseEvent>{
		@Override
		public void handle(MouseEvent e) {

			//typecast the event source as a rectangle so that we may 
			//treat it like one and get it's location
			Rectangle r = (Rectangle)e.getSource();
			
			//System.out.println(e.getSource().toString());
			
			//establish x and y location of the stone in the center of the square
			double xPos = r.getX()+Const.SQUARE_LENGTH/2;
			double yPos = r.getY()+Const.SQUARE_LENGTH/2;
			
			int row = (int)(r.getY()/Const.SQUARE_LENGTH);
			int col = (int)(r.getX()/Const.SQUARE_LENGTH);
			
			//System.out.println(xPos);
			
			if(isTurn&&!added[row][col]) {
				
				
				addStone(xPos,yPos,Color.BLACK,row,col);
				try {
					
					//inform server of the turn
					GoGameClientService.sendPlay((int)r.getX(),(int)r.getY());
					System.out.println("Play sent to server");
				}
				
				catch(IOException exception) {
					exception.printStackTrace();
				}
			}
			
			
		}
	}
	/**
	 * Tracks players time violations
	 * gives 1 violation for every 5 seconds the player waits to make a turn
	 * @author Sean Donaghy
	 *
	 */
	public class PenaltyTimer extends AnimationTimer {
		
		private static final int PENALTY_TIME= 60*5;
		private static int counter = PENALTY_TIME;
		
		@Override
		public void handle(long now) {
			
			counter--;
			if(counter == 0) {
				
				noOfTimeViolations++;
				timeViolations.setText("Time Violations: " + noOfTimeViolations);
				counter = PENALTY_TIME;
			}
		}
		public void reset(){
			
			counter = PENALTY_TIME;
		}
	}
	/**
	 * Main method to launch the application
	 * @param args
	 */
	public static void main(String[] args) {
		Application.launch(args);
		
	
	}
}


