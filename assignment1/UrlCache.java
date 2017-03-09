import java.io.*;
import java.net.*;
import java.util.*;
/**
 * UrlCache Class
 * 
 * @author 	Majid Ghaderi
 * @version	1.1, Sep 30, 2016
 * @author implemented by Jiashan Li by October 14, 2016
 */
public class UrlCache {

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 *
     * @throws UrlCacheException if encounters any errors/exceptions
     */

        public UrlCacheNode head;
        public final String CATALOG_PATH = "cache/catalog";

	    public UrlCache() throws UrlCacheException {
          
        File cacheDirectory = new File("cache"); // check if the directory exist.
        if(!cacheDirectory.exists() || !cacheDirectory.isDirectory())
              cacheDirectory.mkdir();

        File catalog = new File(CATALOG_PATH);     // initialize the catalog
        if(catalog.exists())
        {
            
            try(BufferedReader reader = new BufferedReader(new FileReader(catalog)))
        {
            String urlLine ="";
            String dateLine = "";
           
            
            while ((urlLine != null) && (dateLine != null))   // read all data in original catalog
            {
            
                 urlLine = reader.readLine();
                 dateLine = reader.readLine();
                
                
                if((urlLine == null)||(dateLine == null))  // if there is no more data, stop reading
                    break;
            
                if(head == null)
                {
                    head = new UrlCacheNode(urlLine, dateLine);   
                }
                else
                {
                    head.lastAdd(new UrlCacheNode(urlLine, dateLine));
                }
            }
            reader.close();
        }
        catch(Exception ex)
        {
            System.out.println("Fail to initialize catalog");
           
        }

        }

	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public void getObject(String url) throws UrlCacheException {
          
    // parse url
        int port = 80;
        String urlSplit;
        
        if(url.matches(".*:[0-9]+.*"))    //Check if url includes a port number,get the port number if there is one
        {           
            urlSplit = url.split(":")[1];
            urlSplit = urlSplit.split("/")[0];
            port = Integer.parseInt(urlSplit);
            url = url.replaceAll(":[0-9]+", "");           
            
        }
        
        urlSplit = url;           // get the host information
        urlSplit = urlSplit.replace("http[s]+://", "");
        String host = urlSplit.split("/")[0];
        
        urlSplit = url;           // get the port information
        urlSplit = urlSplit.replace("http[s]+://", "");
        String path = "/" + urlSplit.split("/",2)[1];
        
       

            try{       
            Socket socket = new Socket(host, port);  // creat a TCP connection, pass the data to socket.
            PrintWriter toServer = new PrintWriter(socket.getOutputStream());
            InputStream fromServer = socket.getInputStream();
            
            toServer.print("GET " + path + " " + "HTTP/1.1" + "\n");
            if(isInCatalog(url))  // if the file has alreafy been in the catalog, replace only when it has been modified.
            {
                toServer.print("Host: "+ host + "\n");
                toServer.print("If-Modified-Since: " + findNode(url).getFormattedLastModified()+"\n\n");
            }
            else   // if the file doesn't exist, download it from the website.
            {
                
                toServer.print("Host: "+ host + "\n\n");
            }
               toServer.flush();
               

             String line;   //get every line of the file.
             line = readLine(fromServer);
             String httpReturnMsg = "";
             
             while(!line.equals("") && line != null)  // convert the input stream to strings and get all strings for the file
            {
                httpReturnMsg = httpReturnMsg + line + "\n";
                line = readLine(fromServer);
            }
            
    

   
             if(httpReturnMsg.split("\n")[0].contains("200 OK")) //If the file is new or motified, create a new file in cache and write to it.
            {
               
                 ByteArrayOutputStream outSteam = new ByteArrayOutputStream();  //get the byte array of the inputstream.
                 byte[] buffer = new byte[1024];  
                 int len = -1;  
                 while ((len = fromServer.read(buffer)) != -1) {  
                            outSteam.write(buffer, 0, len);  
                  }  
                 outSteam.close();  
                 fromServer.close();  
                 byte [] data = outSteam.toByteArray();  
               

                 urlSplit = url;    //write the byte array to a new file.
                 FileOutputStream  out = new FileOutputStream("cache/"+urlSplit.replaceAll("/","-"));
                 out.write(data);
                 out.close();

                  
                String lastModified = parseLastModified(httpReturnMsg); //get the last modified date
                 
                //Add new object to the catalog
                if(head == null)
                {
                    head = new UrlCacheNode(url, lastModified);
                }
                else
                {
                    head.lastAdd(new UrlCacheNode(url, lastModified));
                }
                //write the information to catalog
                updateCatalog();
            }

// do nothing if the file has already existed and haven't been motified since last time.
            else if(httpReturnMsg.split("\n")[0].contains("304 Not Modified"))
            {
                System.out.println("Object " + url + " has not been modifed.");
            }

            else
            {
                throw new UrlCacheException("Bad HTTP code: " + httpReturnMsg.split("\n")[0]);
        }

	         socket.close();
    }   

        catch(Exception ex)
        {
            System.out.println(ex.toString());
} 	
	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     * @throws UrlCacheException if the specified url is not in the cache, or there are other errors/exceptions
     */
	public long getLastModified(String url) throws UrlCacheException {
              UrlCacheNode object = findNode(url);
              return object.getLastModified().getTime();

		
	}


// convert the inputstream to char and get one line in the file consist of chars.    
    public String readLine(InputStream input) throws Exception
    {
        String str = "";
        char current = (char)input.read();
        while(current != '\n')
        {
            str = str + current;
            current = (char)input.read();
        }
        str = str.replaceAll("\r", "");
        str = str.replaceAll("\n", "");
        return str;
    }


//print out the file information in the catalog.      
     public void updateCatalog() throws Exception
       {
        File catalog = new File(CATALOG_PATH);
        catalog.createNewFile();
        PrintWriter writer = new PrintWriter(catalog);
        writer.print(head.recursiveToString());
        writer.flush();
        writer.close();
       }
    

// check if the url has already been in the catalog
    public boolean isInCatalog(String url)
    {
        UrlCacheNode current = head;
        if(head == null)
        {
            return false;
        }
        while(!current.getUrl().equals(url))
        {
            current = current.getNext();
            if(current == null)
            {
                return false;
            }
        }
        return true;
    }
   

// find the node with the same url in the argument
    public UrlCacheNode findNode(String url) throws UrlCacheException
    {
        UrlCacheNode current = head;
        while(!current.getUrl().equals(url))
        {
            current = current.getNext();
            if(current == null)
            {
                throw new UrlCacheException("File not in catalog");
            }
        }
        return current;
    }



  
    //return Last modified date
   public String parseLastModified(String httpMsg) throws UrlCacheException
    {
        String[] httpLines = httpMsg.split("\n");
        for(int i = 0; i < httpLines.length; i++)
        {
            if(httpLines[i].contains("Last-Modified:"))
            {
                
                return httpLines[i].replace("Last-Modified: ", "").trim();
            }
        }
        throw new UrlCacheException("Bad HTTP message: No Last-Modified found.");
    }



}
