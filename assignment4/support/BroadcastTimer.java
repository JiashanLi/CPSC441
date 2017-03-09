import java.util.TimerTask;
//Jiashan Li
//10171607

class BroadcastTimer extends TimerTask {

	public Router parent;

	/**
	 * @param the parent FastFtp obj
	 * @return void
	 * @throws none
	 */
	public BroadcastTimer(Router r)
	{
		parent = r;
		
	}

	/**
	 * @param none
	 * @return void
	 * @throws none
	 * 
	 * tell parent to process the timeout
	 */
	public void run()
	{
		try
		{
			if(parent.active)
			{
				parent.broadcast();
			}
		}
		catch(Exception e)
		{
			System.out.println("Error in TimeoutHandler");
			e.printStackTrace();
		}
	}
}