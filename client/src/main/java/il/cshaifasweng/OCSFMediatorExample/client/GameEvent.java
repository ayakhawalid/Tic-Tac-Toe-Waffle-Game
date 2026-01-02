package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;

public class GameEvent {
	private GameMessage gameMessage;
	
	public GameEvent(GameMessage gameMessage) {
		this.gameMessage = gameMessage;
	}
	
	public GameMessage getGameMessage() {
		return gameMessage;
	}
}

