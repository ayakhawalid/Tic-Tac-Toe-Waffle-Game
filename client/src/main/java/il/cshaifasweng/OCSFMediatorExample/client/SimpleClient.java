package il.cshaifasweng.OCSFMediatorExample.client;

import org.greenrobot.eventbus.EventBus;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

public class SimpleClient extends AbstractClient {
	
	private static SimpleClient client = null;

	private SimpleClient(String host, int port) {
		super(host, port);
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		if (msg instanceof GameMessage) {
			EventBus.getDefault().post(new GameEvent((GameMessage) msg));
		}
		else if (msg.getClass().equals(Warning.class)) {
			EventBus.getDefault().post(new WarningEvent((Warning) msg));
		}
		else{
			String message = msg.toString();
			System.out.println(message);
		}
	}
	
	public static SimpleClient getClient() {
		return getClient("localhost");
	}
	
	public static SimpleClient getClient(String host) {
		if (client == null) {
			client = new SimpleClient(host, 3000);
		}
		return client;
	}

}
