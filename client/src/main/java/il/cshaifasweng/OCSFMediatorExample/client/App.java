package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private SimpleClient client;
    private GameController gameController;

    @Override
    public void start(Stage stage) throws IOException {
    	EventBus.getDefault().register(this);
    	client = SimpleClient.getClient();
    	client.openConnection();
        
        // Load game FXML and get controller
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("game.fxml"));
        Parent root = fxmlLoader.load();
        gameController = fxmlLoader.getController();
        
        scene = new Scene(root, 400, 500);
        stage.setTitle("Tic-Tac-Toe Waffle Game");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    
    

    @Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
    	EventBus.getDefault().unregister(this);
    	try {
    		GameMessage disconnectMsg = new GameMessage(GameMessage.DISCONNECT);
    		client.sendToServer(disconnectMsg);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
        client.closeConnection();
		super.stop();
	}
    
    @Subscribe
    public void onGameEvent(GameEvent event) {
    	Platform.runLater(() -> {
    		GameMessage msg = event.getGameMessage();
    		if (gameController != null) {
    			gameController.updateGameState(msg);
    		}
    	});
    }
    
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
    	Platform.runLater(() -> {
    		Alert alert = new Alert(AlertType.WARNING,
        			String.format("Message: %s\nTimestamp: %s\n",
        					event.getWarning().getMessage(),
        					event.getWarning().getTime().toString())
        	);
        	alert.show();
    	});
    	
    }

	public static void main(String[] args) {
        launch();
    }

}