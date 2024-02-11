package lab11b;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
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
//import lab11b.GoGameClient.GoGameClientService;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


/**
 * The GoGameServer class hosts a simple game of GO
 * and supports one opponent client on a local network
 * @author Sean Donaghy
 *
 */
public class GoGameServer extends Application {
	
	private static final int SCENE_WIDTH = 500; 
	private static final int SCENE_HEIGHT = 580;
	private static final int BOARD_LENGTH = 450;
	private static final int NUMBER_OF_ROWS = 9;
	private static final int SQUARE_LENGTH = BOARD_LENGTH / NUMBER_OF_ROWS;  // 50
	private static final int STONE_RADIUS = 15;

	Pane root;
	DrawBoard board;
	
	private static boolean[][] added = new boolean[NUMBER_OF_ROWS][NUMBER_OF_ROWS];
	private static Ellipse[][] stones = new Ellipse[NUMBER_OF_ROWS][NUMBER_OF_ROWS];
	
	private static Text timeViolations;
	private int noOfTimeViolations = 0;
	private static PenaltyTimer timer = new GoGameServer().new PenaltyTimer();//see if this works
	
	private final int REFRESH_LIMIT = 10; // 10/60 seconds
	private int refreshCountDown = REFRESH_LIMIT;
	private Refresh refreshCount;
	
	private TextField textField;
	private Button sitBtn;
	
	private static Text youText;
	private static Text oppText;
	
	private GoGameServerService serverService;
	private String color = "White";
	
	private static boolean isTurn;
	/**
	 * Start method to draw the graphic interface and initialize
	 * the server thread
	 */
	@Override
	public void start(Stage primaryStage) {
		
		isTurn = false;
		
		
		serverService = new GoGameServer().new GoGameServerService();
		Thread t = new Thread(serverService);
		
		
		
		// Initialize all the stones to null;
		for(int row = 0; row < NUMBER_OF_ROWS; row++) {
			for(int col = 0; col < NUMBER_OF_ROWS; col++) {
				stones[row][col] = null;
			}
		}
		
		root = new Pane();
		drawAll();
	
		Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);

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
		HBox playerInfo = new HBox(BOARD_LENGTH / 2, timeViolations, youText);		
		
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
			for(int row = 0; row < NUMBER_OF_ROWS; row++) {
				for(int col = 0; col < NUMBER_OF_ROWS; col++) {
					Rectangle r = new Rectangle(col * SQUARE_LENGTH, row * SQUARE_LENGTH, SQUARE_LENGTH, SQUARE_LENGTH);
					r.setFill(Color.GREEN);
					r.setStroke(Color.BLACK);
					
					r.setOnMouseClicked(new MouseClickEventHandler());
					
					this.getChildren().add(r);
					
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
	private class GoGameServerService implements Runnable{
		
		
		private static DataOutputStream out;
		private static DataInputStream in;
		
		final static int PORT = 1150;
		@Override
		public void run() {
			
			try(ServerSocket server = new ServerSocket(PORT)){
				
				while(true) {
					
					try {
						
						Socket s = server.accept();
						System.out.println("Client Connected");
						//initialize input and outputstreams
						in = new DataInputStream(s.getInputStream());
						out = new DataOutputStream(s.getOutputStream());
						
						
						boolean done = false;
						while(!done) {
							
							String message = in.readUTF();
							
							receiveName(message);
							//message = in.readUTF();
							receivePlay(message);
	
						}
						
						System.out.print("The loop finished");
					}
					
					catch(IOException e) {
						
						e.printStackTrace();
					}
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
		
		private void receiveName(String message){
			
			String name;
			if(message.contains("PLAYER")) {
				
				name = message.substring(7);
				oppText.setText("Opponent name: "+name);
				//System.out.println(oppText.getText());
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
		private void receivePlay(String message){
			
			if(message.contains("PLAY")) {
				isTurn = true;
				
				
				
				Scanner playReader = new Scanner(message);
				playReader.useDelimiter(" ");
				int x=0;
				int y=0;                
				
				System.out.println(message);
					
				playReader.next();
				x = playReader.nextInt();
				y = playReader.nextInt();	
				playReader.close();
				
				int row = (int)(y/SQUARE_LENGTH);
				int col = (int)(x/SQUARE_LENGTH);
				
				int xPos = x+SQUARE_LENGTH/2;
				int yPos = y+SQUARE_LENGTH/2;
				
				
				//System.out.println("Play Received from client:\nrow: "+row+"\ncol: "+col);
				
				Ellipse stone = new Ellipse(xPos,yPos,STONE_RADIUS,STONE_RADIUS);
				//The client's stones are black
				stone.setFill(Color.BLACK);
				
				//update the stones arrays for the refresh to check
				stones[row][col] = stone;
				
				//System.out.println(stones[row][col].toString());
				//System.out.println(added[row][col]);
				
				//start the penalty timer as soon as the opponent has made a play
				timer.start();
				
				
			}
			
		}
	}
	/* Every 1/6 seconds, the system refreshes to add new stones to the board if any
	 * 
	 */
	private class Refresh extends AnimationTimer {
		
		@Override
		public void handle(long arg0) {
		
			refreshCountDown--;
			
			
			if(refreshCountDown == 0) {
			
				refreshCountDown = REFRESH_LIMIT;
			
				// Add the new stones to the board
				for(int row = 0; row < NUMBER_OF_ROWS; row++) {
					for(int col = 0; col < NUMBER_OF_ROWS; col++) {
						if(stones[row][col] != null && !added[row][col]) {
							board.getChildren().add(stones[row][col]);
							//System.out.println(stones[row][col]);
							//System.out.println(board.getChildren().toString());
							added[row][col] = true;
							//System.out.println(added[row][col]);
						}
					}
				}
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
					GoGameServerService.sendName(textField.getText(),serverService.getDataOutputStream());
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
		
		Ellipse stone = new Ellipse(x,y,STONE_RADIUS,STONE_RADIUS);
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
			
			//establish x and y location of the stone in the center of the squar
			//xpos and ypos are the positions of the actual stone
			double xPos = r.getX()+SQUARE_LENGTH/2;
			double yPos = r.getY()+SQUARE_LENGTH/2;
			
			int row = (int)(r.getY()/SQUARE_LENGTH);
			int col = (int)(r.getX()/SQUARE_LENGTH);
			
			if(isTurn&&!added[row][col]) {
				
				addStone(xPos,yPos,Color.WHITE,row,col);
				/*
				Ellipse stone = new Ellipse(xPos,yPos,STONE_RADIUS,STONE_RADIUS);
				stone.setFill(Color.WHITE);
				
				//update the stones arrays for the refresh to check
				stones[row][col] = stone;
				
				isTurn = false;
				*/
				try {
					
					//inform server of the turn
					GoGameServerService.sendPlay((int)r.getX(),(int)r.getY());
					System.out.println("Play sent to client");
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

