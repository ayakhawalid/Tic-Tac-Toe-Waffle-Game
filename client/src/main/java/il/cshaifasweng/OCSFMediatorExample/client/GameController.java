package il.cshaifasweng.OCSFMediatorExample.client;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.entities.GameState;
import il.cshaifasweng.OCSFMediatorExample.entities.Move;

public class GameController {
	
	@FXML
	private Label statusLabel;
	
	@FXML
	private Label playerLabel;
	
	@FXML
	private Label legendLabel;
	
	@FXML
	private GridPane gameBoard;
	
	@FXML
	private Button newGameButton;
	
	private Button[][] boardButtons;
	private SimpleClient client;
	private GameState currentGameState;
	private int myPlayerSymbol;
	private String myPlayerId;
	private Image butterImage;
	private Image blueberryImage;
	
	@FXML
	void initialize() {
		client = SimpleClient.getClient();
		boardButtons = new Button[3][3];
		
		// Load images with error handling - load at original size and quality
		try {
			// Load images at original size - don't modify brightness or resolution
			butterImage = new Image(getClass().getResourceAsStream("/butter.png"));
			blueberryImage = new Image(getClass().getResourceAsStream("/blueberry.png"));
			if (butterImage.isError() || blueberryImage.isError()) {
				System.err.println("Error loading images. Using text fallback.");
				butterImage = null;
				blueberryImage = null;
			}
		} catch (Exception e) {
			System.err.println("Failed to load images: " + e.getMessage());
			e.printStackTrace();
			butterImage = null;
			blueberryImage = null;
		}
		
		initializeBoard();
		
		// Initially enable new game button (user can try to reconnect)
		newGameButton.setDisable(false);
		
		// Send connect message
		try {
			GameMessage connectMsg = new GameMessage(GameMessage.CONNECT);
			client.sendToServer(connectMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeBoard() {
		// Set board background to darker beige (waffle grid lines) - disable any blur effects
		// Using darker beige (#D2B48C) for grid lines, keeping beige squares (#F5DEB3)
		gameBoard.setStyle("-fx-background-color: #D2B48C; " +
		                   "-fx-background-radius: 10px; " +
		                   "-fx-padding: 8px; " +
		                   "-fx-effect: null;"); // Disable any effects that might cause blur
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				Button btn = new Button("");
				btn.setPrefSize(80, 80);
				btn.setMinSize(80, 80);
				btn.setMaxSize(80, 80);
				// Make buttons beige (waffle squares) - no border, grid lines will show through gaps
				// Keep same color even when disabled (not player's turn)
				btn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
				             "-fx-background-color: #F5DEB3; " +
				             "-fx-background-radius: 0px; " +
				             "-fx-border-width: 0px; " +
				             "-fx-opacity: 1.0; " +
				             "-fx-effect: null;"); // Disable any effects that might cause blur
				btn.setDisable(true); // Disabled until game starts
				final int row = i;
				final int col = j;
				btn.setOnAction(e -> handleCellClick(row, col));
				boardButtons[i][j] = btn;
				gameBoard.add(btn, j, i);
			}
		}
	}
	
	private void handleCellClick(int row, int col) {
		if (currentGameState == null || currentGameState.isGameOver() || 
		    currentGameState.isWaitingForPlayer() || 
		    currentGameState.getCurrentPlayer() != myPlayerSymbol) {
			return;
		}
		
		if (currentGameState.getBoard()[row][col] != GameState.EMPTY) {
			return; // Cell already occupied
		}
		
		// Send move to server
		try {
			Move move = new Move(row, col, myPlayerId);
			GameMessage moveMsg = new GameMessage(GameMessage.MOVE);
			moveMsg.setMove(move);
			client.sendToServer(moveMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateGameState(GameMessage message) {
		Platform.runLater(() -> {
			if (message.getClientId() != null && myPlayerId == null) {
				// Store client ID on first message
				myPlayerId = message.getClientId();
			}
			
			// Update status label with message if available
			if (message.getMessage() != null) {
				statusLabel.setText(message.getMessage());
			}
			
			if (message.getGameState() != null) {
				currentGameState = message.getGameState();
				myPlayerSymbol = message.getPlayerSymbol();
				
				// Update status label with game state message
				if (currentGameState.getMessage() != null) {
					statusLabel.setText(currentGameState.getMessage());
				}
				
				// Update player label
				if (myPlayerSymbol == GameState.X) {
					playerLabel.setText("You are Player X (Blueberry)");
				} else if (myPlayerSymbol == GameState.O) {
					playerLabel.setText("You are Player O (Butter)");
				} else {
					playerLabel.setText("");
				}
				
				// Update board
				updateBoard();
				
				// Enable/disable buttons based on turn
				boolean canPlay = !currentGameState.isGameOver() && 
				                  !currentGameState.isWaitingForPlayer() &&
				                  currentGameState.getCurrentPlayer() == myPlayerSymbol;
				enableBoard(canPlay);
				
				// Enable new game button when game is over or waiting for player
				// Disable only when game is active (not over and not waiting)
				boolean gameIsActive = !currentGameState.isGameOver() && !currentGameState.isWaitingForPlayer();
				newGameButton.setDisable(gameIsActive);
				System.out.println("Game state - isOver: " + currentGameState.isGameOver() + 
				                   ", isWaiting: " + currentGameState.isWaitingForPlayer() + 
				                   ", button disabled: " + gameIsActive);
			} else {
				// No game state - disable all buttons
				enableBoard(false);
				// Enable new game button when there's no game state (can reconnect)
				newGameButton.setDisable(false);
				System.out.println("No game state - button enabled");
			}
		});
	}
	
	private void updateBoard() {
		if (currentGameState == null) return;
		
		int[][] board = currentGameState.getBoard();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				Button btn = boardButtons[i][j];
				if (board[i][j] == GameState.X) {
					// Use blueberry image for X, or fallback to text
					if (blueberryImage != null && !blueberryImage.isError()) {
						ImageView imageView = new ImageView(blueberryImage);
						imageView.setFitWidth(75);
						imageView.setFitHeight(75);
						imageView.setPreserveRatio(true);
						imageView.setSmooth(true); // Use smooth for better quality
						imageView.setOpacity(1.0); // Full opacity - no brightness changes
						imageView.setCache(false);
						// Ensure no CSS effects on the image
						imageView.setStyle("-fx-opacity: 1.0;");
						btn.setGraphic(imageView);
						btn.setText("");
						// Button style - waffle square, no border (grid lines show through gaps)
						btn.setStyle("-fx-background-color: #F5DEB3; " +
						             "-fx-background-radius: 0px; " +
						             "-fx-border-width: 0px; " +
						             "-fx-opacity: 1.0;");
					} else {
						btn.setGraphic(null);
						btn.setText("X");
						btn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
						             "-fx-text-fill: #6B46C1; " +
						             "-fx-background-color: #F5DEB3; " +
						             "-fx-background-radius: 0px; " +
						             "-fx-border-width: 0px; " +
						             "-fx-opacity: 1.0;");
					}
					btn.setDisable(true);
				} else if (board[i][j] == GameState.O) {
					// Use butter image for O, or fallback to text
					if (butterImage != null && !butterImage.isError()) {
						ImageView imageView = new ImageView(butterImage);
						imageView.setFitWidth(75);
						imageView.setFitHeight(75);
						imageView.setPreserveRatio(true);
						imageView.setSmooth(true); // Use smooth for better quality
						imageView.setOpacity(1.0); // Full opacity - no brightness changes
						imageView.setCache(false);
						// Ensure no CSS effects on the image
						imageView.setStyle("-fx-opacity: 1.0;");
						btn.setGraphic(imageView);
						btn.setText("");
						// Button style - waffle square, no border (grid lines show through gaps)
						btn.setStyle("-fx-background-color: #F5DEB3; " +
						             "-fx-background-radius: 0px; " +
						             "-fx-border-width: 0px; " +
						             "-fx-opacity: 1.0;");
					} else {
						btn.setGraphic(null);
						btn.setText("O");
						btn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
						             "-fx-text-fill: #FCD34D; " +
						             "-fx-background-color: #F5DEB3; " +
						             "-fx-background-radius: 0px; " +
						             "-fx-border-width: 0px; " +
						             "-fx-opacity: 1.0;");
					}
					btn.setDisable(true);
				} else {
					btn.setGraphic(null);
					btn.setText("");
					btn.setStyle("-fx-background-color: #F5DEB3; " +
					             "-fx-background-radius: 0px; " +
					             "-fx-border-width: 0px; " +
					             "-fx-opacity: 1.0; " +
					             "-fx-effect: null;"); // Disable any effects
				}
			}
		}
	}
	
	private void enableBoard(boolean enable) {
		if (currentGameState == null) {
			// Even without game state, make buttons visible but disabled
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					Button btn = boardButtons[i][j];
					btn.setDisable(true);
					// Reapply style to prevent color change when disabled
					btn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
					             "-fx-background-color: #F5DEB3; " +
					             "-fx-background-radius: 0px; " +
					             "-fx-border-width: 0px; " +
					             "-fx-opacity: 1.0; " +
					             "-fx-effect: null;");
				}
			}
			return;
		}
		
		int[][] board = currentGameState.getBoard();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				Button btn = boardButtons[i][j];
				boolean shouldDisable = !enable || board[i][j] != GameState.EMPTY || 
				                        currentGameState.isGameOver();
				btn.setDisable(shouldDisable);
				// Reapply style after setDisable to prevent color change when disabled
				// Get current style from button or use default beige
				String currentStyle = btn.getStyle();
				if (currentStyle == null || currentStyle.isEmpty()) {
					currentStyle = "-fx-background-color: #F5DEB3; " +
					               "-fx-background-radius: 0px; " +
					               "-fx-border-width: 0px; " +
					               "-fx-opacity: 1.0; " +
					               "-fx-effect: null;";
				}
				// Ensure opacity is always 1.0 to prevent graying out
				if (!currentStyle.contains("-fx-opacity: 1.0")) {
					currentStyle += " -fx-opacity: 1.0;";
				}
				btn.setStyle(currentStyle);
			}
		}
	}
	
	@FXML
	void newGame(ActionEvent event) {
		// Reset local state
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				boardButtons[i][j].setGraphic(null);
				boardButtons[i][j].setText("");
				boardButtons[i][j].setDisable(true);
				boardButtons[i][j].setStyle("-fx-background-color: #F5DEB3; -fx-background-radius: 0px; -fx-border-width: 0px; -fx-opacity: 1.0;");
			}
		}
		currentGameState = null;
		myPlayerSymbol = 0;
		myPlayerId = null;
		playerLabel.setText("");
		newGameButton.setDisable(true);
		
		// Reconnect to server - this will start a new game when 2 players connect
		try {
			if (client != null && client.isConnected()) {
				// Already connected, send connect message to request new game
				statusLabel.setText("Requesting new game...");
				// Send connect message again to reset state
				GameMessage connectMsg = new GameMessage(GameMessage.CONNECT);
				client.sendToServer(connectMsg);
			} else {
				// Reconnect
				client = SimpleClient.getClient();
				if (!client.isConnected()) {
					client.openConnection();
				}
				statusLabel.setText("Connecting to server...");
				GameMessage connectMsg = new GameMessage(GameMessage.CONNECT);
				client.sendToServer(connectMsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
			statusLabel.setText("Error connecting to server");
		}
	}
}

