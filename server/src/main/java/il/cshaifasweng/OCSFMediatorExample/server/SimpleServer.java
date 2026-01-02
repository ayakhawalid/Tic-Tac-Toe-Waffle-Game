package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.entities.GameState;
import il.cshaifasweng.OCSFMediatorExample.entities.Move;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

public class SimpleServer extends AbstractServer {
	private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();
	private GameState gameState;
	private Map<ConnectionToClient, String> clientIds;
	private Map<ConnectionToClient, Boolean> newGameRequests; // Track which clients requested new game
	private Random random;
	
	public SimpleServer(int port) {
		super(port);
		gameState = new GameState();
		gameState.setWaitingForPlayer(true);
		gameState.setMessage("Waiting for second player...");
		clientIds = new HashMap<>();
		newGameRequests = new HashMap<>();
		random = new Random();
		System.out.println("Server started on port " + port);
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		System.out.println("Client connected: " + client.getInetAddress().getHostAddress());
		
		// Clean up disconnected clients first
		cleanupDisconnectedClients();
		
		System.out.println("Current subscribers count after cleanup: " + SubscribersList.size());
		
		// Check if client already exists
		boolean alreadyExists = false;
		for (SubscribedClient sc : SubscribersList) {
			if (sc.getClient().equals(client)) {
				alreadyExists = true;
				break;
			}
		}
		
		if (alreadyExists) {
			System.out.println("Client already in list, skipping");
			return;
		}
		
		String clientId = generateClientId();
		clientIds.put(client, clientId);
		
		// Check if we have 2 players
		if (SubscribersList.size() < 2) {
			SubscribedClient connection = new SubscribedClient(client);
			SubscribersList.add(connection);
			System.out.println("Added client. New subscribers count: " + SubscribersList.size());
			
			if (SubscribersList.size() == 1) {
				// First player - waiting for second
				gameState = new GameState(); // Reset game state
				gameState.setWaitingForPlayer(true);
				gameState.setMessage("Waiting for second player...");
				try {
					GameMessage msg = new GameMessage(GameMessage.WAITING);
					msg.setGameState(gameState.copy());
					msg.setClientId(clientId);
					client.sendToClient(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (SubscribersList.size() == 2) {
				// Second player connected - start game
				System.out.println("Starting new game with 2 players");
				startNewGame();
			}
		} else {
			// Already 2 players, reject connection
			System.out.println("Rejecting connection - game is full");
			try {
				GameMessage msg = new GameMessage(GameMessage.WAITING);
				msg.setMessage("Game is full. Please try again later.");
				msg.setClientId(clientId);
				client.sendToClient(msg);
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void cleanupDisconnectedClients() {
		// Get the list of actually connected clients from the server
		Thread[] activeConnections = getClientConnections();
		java.util.Set<ConnectionToClient> activeClients = new java.util.HashSet<>();
		
		for (Thread t : activeConnections) {
			if (t instanceof ConnectionToClient) {
				activeClients.add((ConnectionToClient) t);
			}
		}
		
		// Remove clients that are not in the active connections list
		ArrayList<SubscribedClient> toRemove = new ArrayList<>();
		for (SubscribedClient sc : SubscribersList) {
			ConnectionToClient client = sc.getClient();
			if (client == null || !activeClients.contains(client)) {
				toRemove.add(sc);
			} else {
				// Additional check: verify thread is still alive
				if (!client.isAlive() || client.isInterrupted()) {
					toRemove.add(sc);
				}
			}
		}
		
		for (SubscribedClient sc : toRemove) {
			SubscribersList.remove(sc);
			ConnectionToClient client = sc.getClient();
			if (client != null) {
				clientIds.remove(client);
			}
			System.out.println("Removed disconnected client during cleanup");
		}
		
		if (!toRemove.isEmpty()) {
			System.out.println("Cleaned up " + toRemove.size() + " disconnected client(s)");
		}
	}
	
	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		System.out.println("Client disconnected: " + client.getInetAddress().getHostAddress());
		System.out.println("Subscribers before removal: " + SubscribersList.size());
		
		clientIds.remove(client);
		newGameRequests.remove(client); // Also remove from new game requests
		
		// Remove from subscribers
		SubscribedClient toRemove = null;
		for (SubscribedClient subscribedClient : SubscribersList) {
			if (subscribedClient.getClient().equals(client)) {
				toRemove = subscribedClient;
				break;
			}
		}
		if (toRemove != null) {
			SubscribersList.remove(toRemove);
			System.out.println("Removed client. New subscribers count: " + SubscribersList.size());
		}
		
		// Reset game state when a player disconnects
		gameState = new GameState();
		gameState.setWaitingForPlayer(true);
		gameState.setMessage("Waiting for second player...");
		
		// If there's still a player, notify them
		if (SubscribersList.size() == 1) {
			try {
				ConnectionToClient remainingClient = SubscribersList.get(0).getClient();
				String remainingClientId = clientIds.get(remainingClient);
				GameMessage msg = new GameMessage(GameMessage.WAITING);
				msg.setGameState(gameState.copy());
				msg.setClientId(remainingClientId);
				remainingClient.sendToClient(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof GameMessage) {
			GameMessage gameMsg = (GameMessage) msg;
			String clientId = clientIds.get(client);
			
			if (GameMessage.MOVE.equals(gameMsg.getType())) {
				handleMove(gameMsg.getMove(), clientId, client);
			} else if (GameMessage.CONNECT.equals(gameMsg.getType())) {
				// Handle CONNECT message from already-connected client (e.g., "New Game" button)
				// Clean up disconnected clients first
				cleanupDisconnectedClients();
				
				// Check if client is already in subscribers list
				boolean alreadyInList = false;
				for (SubscribedClient sc : SubscribersList) {
					if (sc.getClient().equals(client)) {
						alreadyInList = true;
						break;
					}
				}
				
				// If not in list, add them
				if (!alreadyInList && SubscribersList.size() < 2) {
					SubscribedClient connection = new SubscribedClient(client);
					SubscribersList.add(connection);
					System.out.println("Added client via CONNECT message. New subscribers count: " + SubscribersList.size());
				}
				
				// Mark this client as requesting a new game
				newGameRequests.put(client, true);
				System.out.println("Client " + clientId + " requested new game. Total requests: " + newGameRequests.size() + ", Subscribers: " + SubscribersList.size());
				
				// CRITICAL: Check if we have 2 players AND both have requested new game
				if (SubscribersList.size() == 2) {
					// Check if both players in the list have requested new game
					int requestsFromSubscribers = 0;
					for (SubscribedClient sc : SubscribersList) {
						if (newGameRequests.containsKey(sc.getClient()) && newGameRequests.get(sc.getClient())) {
							requestsFromSubscribers++;
						}
					}
					
					if (requestsFromSubscribers == 2) {
						// Both players are ready - start new game immediately
						System.out.println("Both players ready - starting new game (from CONNECT message)");
						// Clear the requests
						newGameRequests.clear();
						startNewGame();
						// Don't send any waiting messages - the game has started
						return; // Exit early to avoid sending waiting message
					} else {
						// One player requested, waiting for the other
						System.out.println("One player requested new game, waiting for second. Requests: " + requestsFromSubscribers);
					}
				}
				
				// Only send waiting message if we have exactly 1 player
				// This should only happen when the first player presses "New Game" before the second player
				if (SubscribersList.size() == 1) {
					gameState = new GameState();
					gameState.setWaitingForPlayer(true);
					gameState.setMessage("Waiting for second player...");
					try {
						GameMessage waitingMsg = new GameMessage(GameMessage.WAITING);
						waitingMsg.setGameState(gameState.copy());
						waitingMsg.setClientId(clientId);
						client.sendToClient(waitingMsg);
						System.out.println("Sent waiting message to first player. Current subscribers: " + SubscribersList.size());
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (SubscribersList.size() == 2 && newGameRequests.getOrDefault(client, false)) {
					// We have 2 players but only this one requested - send waiting message to this player
					gameState = new GameState();
					gameState.setWaitingForPlayer(true);
					gameState.setMessage("Waiting for second player to press New Game...");
					try {
						GameMessage waitingMsg = new GameMessage(GameMessage.WAITING);
						waitingMsg.setGameState(gameState.copy());
						waitingMsg.setClientId(clientId);
						client.sendToClient(waitingMsg);
						System.out.println("Sent waiting message to player who requested. Other player hasn't requested yet.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					// More than 2 players (shouldn't happen, but handle it)
					System.out.println("Unexpected: More than 2 subscribers when handling CONNECT");
				}
			}
		}
	}
	
	private void handleMove(Move move, String playerId, ConnectionToClient client) {
		if (gameState.isWaitingForPlayer() || gameState.isGameOver()) {
			return;
		}
		
		// Determine which symbol this player is
		int playerSymbol = 0;
		if (gameState.getPlayerXId() != null && gameState.getPlayerXId().equals(playerId)) {
			playerSymbol = GameState.X;
		} else if (gameState.getPlayerOId() != null && gameState.getPlayerOId().equals(playerId)) {
			playerSymbol = GameState.O;
		} else {
			return; // Invalid player
		}
		
		// Check if it's this player's turn
		if (gameState.getCurrentPlayer() != playerSymbol) {
			// Not this player's turn
			try {
				GameMessage msg = new GameMessage(GameMessage.GAME_STATE);
				msg.setGameState(gameState.copy());
				msg.setMessage("It's not your turn!");
				client.sendToClient(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		// Make the move
		if (gameState.makeMove(move.getRow(), move.getCol(), playerSymbol)) {
			// Update message based on game state
			if (gameState.isGameOver()) {
				if (gameState.getWinner() == GameState.X) {
					gameState.setMessage("Player X (Blueberry) wins!");
				} else if (gameState.getWinner() == GameState.O) {
					gameState.setMessage("Player O (Butter) wins!");
				} else {
					gameState.setMessage("It's a tie!");
				}
			} else {
				String currentPlayerName = (gameState.getCurrentPlayer() == GameState.X) ? "X (Blueberry)" : "O (Butter)";
				gameState.setMessage("Turn of player " + currentPlayerName);
			}
			
			// Send updated game state to both players
			broadcastGameState();
		} else {
			// Invalid move
			try {
				GameMessage msg = new GameMessage(GameMessage.GAME_STATE);
				msg.setGameState(gameState.copy());
				msg.setMessage("Invalid move!");
				client.sendToClient(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void startNewGame() {
		gameState = new GameState();
		
		// Randomly assign X and O
		ArrayList<ConnectionToClient> players = new ArrayList<>();
		for (SubscribedClient sc : SubscribersList) {
			players.add(sc.getClient());
		}
		
		if (random.nextBoolean()) {
			gameState.setPlayerXId(clientIds.get(players.get(0)));
			gameState.setPlayerOId(clientIds.get(players.get(1)));
		} else {
			gameState.setPlayerXId(clientIds.get(players.get(1)));
			gameState.setPlayerOId(clientIds.get(players.get(0)));
		}
		
		// Randomly decide who starts
		gameState.setCurrentPlayer(random.nextBoolean() ? GameState.X : GameState.O);
		gameState.setWaitingForPlayer(false);
		
		String currentPlayerName = (gameState.getCurrentPlayer() == GameState.X) ? "X (Blueberry)" : "O (Butter)";
		gameState.setMessage("Game started! Turn: Player " + currentPlayerName);
		
		// Send game start message to both players
		for (int i = 0; i < SubscribersList.size(); i++) {
			try {
				ConnectionToClient player = SubscribersList.get(i).getClient();
				String playerId = clientIds.get(player);
				int playerSymbol = playerId.equals(gameState.getPlayerXId()) ? GameState.X : GameState.O;
				
				GameMessage msg = new GameMessage(GameMessage.GAME_START);
				msg.setGameState(gameState.copy());
				msg.setPlayerSymbol(playerSymbol);
				msg.setClientId(playerId);
				msg.setMessage("You are player " + (playerSymbol == GameState.X ? "X (Blueberry)" : "O (Butter)"));
				player.sendToClient(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void broadcastGameState() {
		for (SubscribedClient sc : SubscribersList) {
			try {
				ConnectionToClient client = sc.getClient();
				String playerId = clientIds.get(client);
				int playerSymbol = playerId.equals(gameState.getPlayerXId()) ? GameState.X : GameState.O;
				
				GameMessage msg = new GameMessage(GameMessage.GAME_STATE);
				msg.setGameState(gameState.copy());
				msg.setPlayerSymbol(playerSymbol);
				msg.setClientId(playerId);
				client.sendToClient(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String generateClientId() {
		return "client_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
	}
	
	public void sendToAllClients(String message) {
		try {
			for (SubscribedClient subscribedClient : SubscribersList) {
				subscribedClient.getClient().sendToClient(message);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
