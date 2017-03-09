import java.util.TimerTask;

class TimeoutHandler extends TimerTask {

	public FastFtp parent;

	
	public TimeoutHandler(FastFtp ftp)
	{
		parent = ftp;
		
	}

	
	//tell parent to process the timeout
	public void run()
	{
		try
		{
			parent.processTimeout();
		}
		catch(Exception e)
		{
			System.out.println("Error in TimeoutHandler");
			e.printStackTrace();
		}
	}
}