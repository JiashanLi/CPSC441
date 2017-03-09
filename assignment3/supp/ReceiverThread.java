import java.net.*;
import java.io.*;

public class ReceiverThread extends Thread
{
	public final int DATAGRAM_SIZE = 8+4;
	public final int TIMEOUT = 1000;
	public FastFtp parent;
	public DatagramSocket socket;

	
	public ReceiverThread(FastFtp ftp, DatagramSocket sock)
	{
		parent = ftp;
		socket = sock;
		try
		{
		socket.setSoTimeout(TIMEOUT);
		}
		catch(Exception e)
		{
			System.out.println("Error setting socket timeout.");
		}
	}

	
	 //Listens for ACKs and informs parent when they come
	public void run()
	{

		while (parent.running)
		{
			
			
			byte[] data = new byte[DATAGRAM_SIZE];
			DatagramPacket pkt = new DatagramPacket(data, DATAGRAM_SIZE);
			try
			{
				socket.receive(pkt);
				parent.processAck(new Segment(pkt));
			}
			catch(SocketTimeoutException e)
			{
				//DO nothing
			}
			catch(Exception e)
			{
				System.out.println("Error in ReceiverThread");
				e.printStackTrace();
			}
			
		}
	}
}