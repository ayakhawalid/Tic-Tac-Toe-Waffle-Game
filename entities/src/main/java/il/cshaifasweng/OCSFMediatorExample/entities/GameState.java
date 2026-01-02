package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class GameState implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final int EMPTY = 0;
	public static final int X = 1;
	public static final int O = 2;
	
	private int[][] board;
	private int currentPlayer; // X or O
	private String playerXId;
	private String playerOId;
	private boolean gameOver;
	private int winner; // 0 = no winner, X = 1, O = 2, 3 = tie
	private String message;
	private boolean waitingForPlayer;
	
	public GameState() {
		board = new int[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				board[i][j] = EMPTY;
			}
		}
		gameOver = false;
		winner = 0;
		waitingForPlayer = true;
		message = "Waiting for second player...";
	}
	
	public int[][] getBoard() {
		return board;
	}
	
	public void setBoard(int[][] board) {
		this.board = board;
	}
	
	public int getCurrentPlayer() {
		return currentPlayer;
	}
	
	public void setCurrentPlayer(int currentPlayer) {
		this.currentPlayer = currentPlayer;
	}
	
	public String getPlayerXId() {
		return playerXId;
	}
	
	public void setPlayerXId(String playerXId) {
		this.playerXId = playerXId;
	}
	
	public String getPlayerOId() {
		return playerOId;
	}
	
	public void setPlayerOId(String playerOId) {
		this.playerOId = playerOId;
	}
	
	public boolean isGameOver() {
		return gameOver;
	}
	
	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}
	
	public int getWinner() {
		return winner;
	}
	
	public void setWinner(int winner) {
		this.winner = winner;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public boolean isWaitingForPlayer() {
		return waitingForPlayer;
	}
	
	public void setWaitingForPlayer(boolean waitingForPlayer) {
		this.waitingForPlayer = waitingForPlayer;
	}
	
	public boolean makeMove(int row, int col, int player) {
		if (board[row][col] != EMPTY || gameOver || player != currentPlayer) {
			return false;
		}
		
		board[row][col] = player;
		checkGameOver();
		
		if (!gameOver) {
			currentPlayer = (currentPlayer == X) ? O : X;
		}
		
		return true;
	}
	
	private void checkGameOver() {
		// Check rows
		for (int i = 0; i < 3; i++) {
			if (board[i][0] != EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
				gameOver = true;
				winner = board[i][0];
				return;
			}
		}
		
		// Check columns
		for (int j = 0; j < 3; j++) {
			if (board[0][j] != EMPTY && board[0][j] == board[1][j] && board[1][j] == board[2][j]) {
				gameOver = true;
				winner = board[0][j];
				return;
			}
		}
		
		// Check diagonals
		if (board[0][0] != EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
			gameOver = true;
			winner = board[0][0];
			return;
		}
		
		if (board[0][2] != EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
			gameOver = true;
			winner = board[0][2];
			return;
		}
		
		// Check for tie
		boolean isFull = true;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == EMPTY) {
					isFull = false;
					break;
				}
			}
			if (!isFull) break;
		}
		
		if (isFull) {
			gameOver = true;
			winner = 3; // Tie
		}
	}
	
	public GameState copy() {
		GameState copy = new GameState();
		copy.board = new int[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				copy.board[i][j] = this.board[i][j];
			}
		}
		copy.currentPlayer = this.currentPlayer;
		copy.playerXId = this.playerXId;
		copy.playerOId = this.playerOId;
		copy.gameOver = this.gameOver;
		copy.winner = this.winner;
		copy.message = this.message;
		copy.waitingForPlayer = this.waitingForPlayer;
		return copy;
	}
}

