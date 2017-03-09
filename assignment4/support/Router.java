import java.net.*;
import java.util.*;
import java.io.*;
import cpsc441.a4.shared.*

/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 * A separate process broadcasts routing update messages
 * to directly connected neighbors at regular intervals.
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	2.1
 *
 */


public class Router {

	int id;
	String server;
	int port;
	int interval;
	int[] linkcost;
	int[] nexthop;
	int[][] mincost;
	boolean[] neighbors;
	RtnTable tbl;
	ObjectInputStream in;
	ObjectOutputStream out;
	boolean active = true;
	Timer timer; 
	
    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {

		id = routerId;	
		server = serverName;
		port = serverPort;
		interval = updateInterval;
		
	}
	

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     */
	public RtnTable start() {
		
		try
		{
			Socket sock = new Socket(InetAddress.getByName(server), port);
			in = new ObjectInputStream(sock.getInputStream());
			out = new ObjectOutputStream(sock.getOutputStream());
			sendPacket(DvrPacket.SERVER, DvrPacket.HELLO);
			DvrPacket serverHello = (DvrPacket)in.readObject(); //get the servers HELLO

			if(serverHello.type != DvrPacket.HELLO)
			{
				throw new Exception("Bad packet recieved: Not HELLO");
			}
			else if(serverHello.sourceid != DvrPacket.SERVER)
			{
				throw new Exception("Bad packet recieved: Not from SERVER");
			}

			linkcost = serverHello.mincost;
			neighbors = initNeighbors();
			nexthop = new int[linkcost.length];

			for(int i = 0; i<nexthop.length; i++)
			{
				if(neighbors[i])
				{
					nexthop[i] = i;
				}
				else
				{
					nexthop[i] = id;
				}
			}

			mincost = new int[linkcost.length][linkcost.length];
			for(int i = 0; i < mincost.length; i++)
			{
				Arrays.fill(mincost[i], DvrPacket.INFINITY);
			}
			mincost[id] = linkcost;
			tbl = new RtnTable(linkcost, nexthop); //init RtnTable
			

			//Start timer.
			timer = new Timer();
			timer.scheduleAtFixedRate(new BroadcastTimer(this), 0, (long)interval);

			while(active || in.available() > 0)
			{
				try
				{
					rcvPacket();
				}
				catch(SocketException e)
				{
					if(active)
					{
						throw e;
					}
				}
			}
			sock.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return tbl;
	}




	public void sendPacket(int destId, int type) throws IOException
	{
		if(type == DvrPacket.HELLO)
		{
			out.writeObject(new DvrPacket(id, destId, type)); //create and send a HELLO pkt
		}
		else if(type == DvrPacket.ROUTE)
		{
			out.writeObject(new DvrPacket(id, destId, type, mincost[id]));
		}

	}


   //calculates who direct neighbors are 
	public boolean[] initNeighbors()
	{
		boolean[] n = new boolean[linkcost.length];
		for(int i = 0; i < linkcost.length; i++)
		{
			if(i != id && linkcost[i] != DvrPacket.INFINITY)
			{
				n[i] = true;
			}
		}
		return n;
	}




	public DvrPacket rcvPacket() throws IOException, ClassNotFoundException
	{
		DvrPacket pkt = (DvrPacket)in.readObject();

		if(pkt.type == DvrPacket.HELLO)
		{
			//shouldnt happen I think	
		}
		else if(pkt.type == DvrPacket.ROUTE)
		{
			//process incoming data
			tbl = process(pkt);
			
		}
		else if(pkt.type == DvrPacket.QUIT)
		{
			active = false;
			//end timer, close socket, clean up
			timer.cancel();
		}
		return pkt;
	}



	public RtnTable process(DvrPacket pkt)
	{
		if(pkt.sourceid == pkt.SERVER)
		{
			linkcost = pkt.mincost;
			neighbors = initNeighbors();
		}
		else
		{
			mincost[pkt.sourceid] = pkt.mincost;
		}
		//then update nexthop
		for(int i = 0; i < nexthop.length; i++)
		{
			int currentDistance;
			if(i == id)
			{
				currentDistance = 0;
			}
			else if(nexthop[i] == id)
			{
				currentDistance = DvrPacket.INFINITY; //cant get there
			}
			else
			{
				currentDistance = mincost[id][i]; 
			}
			for(int j = 0; j < mincost.length; j++)
			{
				if((mincost[j][i]+linkcost[j])<currentDistance)
				{
					currentDistance = (mincost[j][i]+linkcost[j]);
					nexthop[i] = j;
					mincost[id][i] = currentDistance;
				}
			}
		}
		//then recalculate the table
		return new RtnTable(mincost[id], nexthop);
	}





	public void broadcast() throws IOException
	{
		try
		{
			for(int i = 0; i<neighbors.length; i++)
			{
				if(neighbors[i])
				{
					sendPacket(i, DvrPacket.ROUTE); //if i is a neighbor send update pkt
				}
			}
		}
		catch(SocketException e)
		{
			if(active)
			{
				throw e;
			}
		}
		
	}

		
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// default parameters
		int routerId = 0;
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000; //milli-seconds
		
		// the router can be run with:
		// i. a single argument: router Id
		// ii. all required arquiments
		if (args.length == 1) {
			routerId = Integer.parseInt(args[0]);
		}
		else if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		}
		else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		// print the parameters
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
		
		// start the server
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
