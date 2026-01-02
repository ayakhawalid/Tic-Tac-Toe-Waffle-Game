package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class GameMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final String CONNECT = "CONNECT";
	public static final String DISCONNECT = "DISCONNECT";
	public static final String MOVE = "MOVE";
	public static final String GAME_STATE = "GAME_STATE";
	public static final String WAITING = "WAITING";
	public static final String GAME_START = "GAME_START";
	
	private String type;
	private GameState gameState;
	private Move move;
	private String message;
	private int playerSymbol; // X or O for the receiving player
	private String clientId; // Client's unique ID
	
	public GameMessage(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public GameState getGameState() {
		return gameState;
	}
	
	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}
	
	public Move getMove() {
		return move;
	}
	
	public void setMove(Move move) {
		this.move = move;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public int getPlayerSymbol() {
		return playerSymbol;
	}
	
	public void setPlayerSymbol(int playerSymbol) {
		this.playerSymbol = playerSymbol;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
}

