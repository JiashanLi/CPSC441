import java.net.*;
import java.io.*;

/**
 * @author Jiashan Li
 * Nov 3, 2016
 */


 class WebServer {

	private ServerSocket serverSocket;
	private MainThread mainThread;

// initiate the server socket and main thread.
	public WebServer(int port) {
		try {
			serverSocket = new ServerSocket(port);
			mainThread = new MainThread(serverSocket);
		    } 
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Can not open port: " + port, e);
		}

	}

// call the main thread to build connection.
	public void start() {

		mainThread.start();

	}


	public void shutdown() {

		mainThread.setStopped();
	}
}