import java.net.*;
import java.util.*;
import java.io.*;
import cpsc441.a3.*;

/**
 * FastFtp Class
 * 
 * FastFtp implements a basic FTP application based on UDP data transmission.
 * The main mehtod is send() which takes a file name as input argument and send the file 
 * to the specified destination host.
 * 
 */
public class FastFtp {

	public int window;
	public int timeout;
	public TxQueue queue;
	public DatagramSocket socket;
	public Socket tcpSock;
	public int portNum;	
	public boolean running = true;
	public Timer timer;
	public ReceiverThread receiver;
	public InetAddress ip;
		
	
    /**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N (in segments)
     * @param rtoTimer		The time-out interval for the retransmission timer (in milli-seconds)
     */
	public FastFtp(int windowSize, int rtoTimer) {

		window = windowSize;
		timeout = rtoTimer;
		queue = new TxQueue(window);

	}
	

    /**
     * Sends the specified file to the specified destination host:
     * 1. send file name and receiver server confirmation over TCP
     * 2. send file segment by segment over UDP
     * 3. send end of transmission over tcp
     * 3. clean up
     * 
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the rmeote server
     */
	public void send(String serverName, int serverPort, String fileName) {
		// open the TCP connection first
		if(!tcpConnect(serverName, serverPort, fileName)) 
		{
			System.out.println("Error creating TCP connection.");
			System.exit(1);
		}


        // create socket for UDP to send the file.
		try
		{
			socket = new DatagramSocket(tcpSock.getLocalPort());
			ip = InetAddress.getByName(serverName);
			portNum = serverPort;

			File file = new File(fileName);
			long filesize = file.length();
			FileInputStream fis = new FileInputStream(file);

			receiver = new ReceiverThread(this, socket);
			receiver.start();

			int bytesRead = 0;
			int seqNum = 0;

            // send the file by packets           
			while(bytesRead < filesize)
			{
				Segment current = new Segment();
				byte[] data;
				if(filesize-bytesRead < current.MAX_PAYLOAD_SIZE)
				{
					data = new byte[(int)(filesize-bytesRead)];
				}
				else
				{
					data = new byte[current.MAX_PAYLOAD_SIZE];
				}
				fis.read(data);
				current.setPayload(data);
				current.setSeqNum(seqNum);
				seqNum++;
				bytesRead+= data.length;

				//wait for room in queue
				while(queue.isFull()){}
				processSend(current);
			}

			while(!queue.isEmpty()){}
			//send end of transmission msg
			endTransmission(serverName, serverPort);
			running = false;
			while(receiver.isAlive()){}
			socket.close();
		}

		catch(Exception e)
		{
			e.printStackTrace();
		}
	}



	// create TCP connection, and return true when the connection is created.
	private boolean tcpConnect(String serverName, int serverPort, String fileName)
	{
		try
		{
			tcpSock = new Socket(serverName, serverPort);
			DataInputStream in = new DataInputStream(tcpSock.getInputStream());
			DataOutputStream out = new DataOutputStream(tcpSock.getOutputStream());
			out.writeUTF(fileName);
			byte msg = -1;
			while((msg = in.readByte()) == -1){}

			return (msg == 0) ? true : false;
		}
		catch(Exception e)
		{
			System.out.println("TCP Socket Error:");
			e.printStackTrace();
		}
		return false;
	}



	//the segment add to queue, convert to a packet and send over udp
	public synchronized void processSend(Segment seg) throws IOException, InterruptedException
	{
		
		DatagramPacket pkt = new DatagramPacket(seg.getBytes(), seg.getBytes().length, ip, portNum);
		socket.send(pkt);
		queue.add(seg);
		//if its the first one in queue (queue.length == 0) start timer
		if(queue.size() == 1)
		{
			startTimer();
		}
	}

	
	//Sends a single 0 byte to show that the file transfer is over
	private void endTransmission(String serverName, int serverPort)
	{
		try
		{
			DataOutputStream out = new DataOutputStream(tcpSock.getOutputStream());
			out.writeByte(0);
			tcpSock.close();
		}
		catch(Exception e)
		{
			System.out.println("TCP End Transmission Error:");
			e.printStackTrace();
		}
	}



   // when the packet has been received, move the window and reset the timer.
	public synchronized void processAck(Segment ack) throws InterruptedException
	{
		//cancel timer
		timer.cancel();
		//remove anything with seq number lower than ack from queue
		while(!queue.isEmpty() && queue.element().getSeqNum() < ack.getSeqNum())
		{
			queue.remove();
		}
		//if queue not empty, start new timer
		if(!queue.isEmpty())
		{
			startTimer();
		}
	}


	
	public synchronized void processTimeout() throws IOException
	{
		//get all segments in queue
		//resend them all
		//if queue is not empty, restart timer
		timer.cancel();
		Segment[] pending = queue.toArray();
		for(Segment seg:pending)
		{
			DatagramPacket pkt = new DatagramPacket(seg.getBytes(), seg.getBytes().length, ip, portNum);
			socket.send(pkt);
		}
		if(!queue.isEmpty())
		{
			startTimer();
		}
	}


	public void startTimer()
	{
		if(timer != null)
			timer.cancel();
		timer = new Timer(true);
		timer.schedule(new TimeoutHandler(this), (long)timeout);
	}
	

    /**
     * A simple test driver
     * 
     */
	 public static void main(String[] args) {
		int windowSize = 10; //segments
		int timeout = 100; // milli-seconds
		
		String serverName = "localhost";
		String fileName = "";
		int serverPort = 0;
		
		// check for command line arguments
		if (args.length == 3) {
			// either privide 3 paramaters
			serverName = args[0];
			serverPort = Integer.parseInt(args[1]);
			fileName = args[2];
		}
		else if (args.length == 2) {
			// or just server port and file name
			serverPort = Integer.parseInt(args[0]);
			fileName = args[1];
		}
		else {
			System.out.println("wrong number of arguments, try agaon.");
			System.out.println("usage: java FastFtp server port file");
			System.exit(0);
		}

		
		FastFtp ftp = new FastFtp(windowSize, timeout);
		
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}


	
}
