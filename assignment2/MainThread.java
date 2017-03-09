import java.io.*;
import java.net.*;

/**
 * @author Jiashan Li
 * Nov 3, 2016
 */

public class MainThread extends Thread {
	
	private ServerSocket serverSocket;
	private boolean terminated = false;
	

	public MainThread(ServerSocket s)
	{
		serverSocket = s;	
	}
	

	 //build multiple connections when the server is not terminated.
	public void run(){		
		
		// listen for incoming connections, 
		// if one exists create a thread to handle it
		while(!(terminated))
		{
			Socket clientSocket = new Socket();
			
			try {				
				
				clientSocket = serverSocket.accept();
				ThreadConnection slave = new ThreadConnection(clientSocket);
				System.out.println("Accepted connection.");
				slave.start();		//after connected, parse the request and return the response.		
				
			} 

			catch (IOException e) {
				
				e.printStackTrace();
				if(terminated)    // the server closed.
				{
					System.out.println("Server Stopped");
					return;
				}
				throw new RuntimeException("Client Connection error",e);				
			}
			
		}		
	}

	  //method toggles the boolean value stopped
	 //which in turn controls the server_listen loop.

	public void setStopped()
	{
		terminated = true;
	}

}