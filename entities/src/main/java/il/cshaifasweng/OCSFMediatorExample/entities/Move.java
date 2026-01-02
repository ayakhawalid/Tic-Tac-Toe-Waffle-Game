package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class Move implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int row;
	private int col;
	private String playerId;
	
	public Move(int row, int col, String playerId) {
		this.row = row;
		this.col = col;
		this.playerId = playerId;
	}
	
	public int getRow() {
		return row;
	}
	
	public void setRow(int row) {
		this.row = row;
	}
	
	public int getCol() {
		return col;
	}
	
	public void setCol(int col) {
		this.col = col;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
}

