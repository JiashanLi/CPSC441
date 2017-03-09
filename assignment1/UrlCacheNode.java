import java.text.*;
import java.util.*;


public class UrlCacheNode {


	private SimpleDateFormat date_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private String fullUrl;
    private Date lastModified;
    private UrlCacheNode next;


    //Constructs a node using a URL a Last-Modified date and a Filename where the data is stored
    public UrlCacheNode(String inputUrl, String inputLastModified) throws UrlCacheException
    {
    	date_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    	try
    	{
    		fullUrl = inputUrl;
    		lastModified = date_FORMAT.parse(inputLastModified);
    	}
    	catch(Exception ex)
    	{
    		throw new UrlCacheException("Fail to initialize UrlCacheNode");
    	}
    }
   

   //Add a node to the end of the linked list
    public void lastAdd(UrlCacheNode node)
    {
        if(next == null)
        {
            this.setNext(node);
        }
        else
        {
            UrlCacheNode current = next;
            while(current.getNext() != null)
            {
                current = current.getNext();
            }
             current.setNext(node);
        }
    }



    //return a String with nodes data
    public String toString()
    {
    	return fullUrl+"\n"+date_FORMAT.format(lastModified)+"\n\n";
    }

    //return a String representation of this node and all following nodes
    public String recursiveToString()
    {
    	return toString() + ((next!=null)? next.recursiveToString() : "");
    }


    public String getUrl()
    {
    	return fullUrl;
    }

    public Date getLastModified()
    {
    	return lastModified;
    }

    //return last modified as an HTTP formatted String
    public String getFormattedLastModified()
    {
    	return date_FORMAT.format(lastModified);
    }

    public UrlCacheNode getNext()
    {
    	return next;
    }

    public void setNext(UrlCacheNode node)
    {
    	next = node;
    }

   
  
}
