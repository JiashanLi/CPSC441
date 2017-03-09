import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * @author Jiashan Li
 *  Nov 3, 2016
 */


public class ThreadConnection extends Thread {

	private Socket clientSocket;

	public ThreadConnection(Socket s) {
		clientSocket = s;
	}

	public void run() {

		String s = "";
		try {
			InputStream input = clientSocket.getInputStream();
			OutputStream output = clientSocket.getOutputStream();

			// read the inputstream, save the request into a byte array
			byte[] buf = new byte[1024];
			int count = input.read(buf);

			// extract the entire request into a string
			if (count > 0) {
				s += new String(buf, 0, count);
			}

			String responseHeader = formResponseHeader(s);

			// if first line of the header contains 200 OK then output the file.
			if (responseHeader.contains("200 OK")) {

				FileInputStream inFile = new FileInputStream(getFile(s)); 
				buf = new byte[1024];
				count = 0;

                responseHeader = goodHeader(responseHeader, getFile(s));
				output.write((responseHeader).getBytes());

				// read file to byteArray and write file to output stream
				while ((count = inFile.read(buf)) > 0) {
					output.write(buf);
					output.flush();
				}
				inFile.close();
			}

			// some type of error happened
			// append the header and write to outputstream
			else 
			{
				responseHeader = badHeader(responseHeader);
				output.write(responseHeader.getBytes());
			}

			output.close();
			input.close();

		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}


	// get the response header.
	private String formResponseHeader(String s) {

		String firstLineFill = checkRequest(s);
		String line1 = "";

		if(s.contains("HTTP/1.1"))
		 {
			line1 = "HTTP/1.1 " + firstLineFill + "\r\n";
		 }
		else if(s.contains("HTTP/1.0"))
		{
			line1 = "HTTP/1.0 " + firstLineFill + "\r\n";
		}
		
		String line2 = "Connection: close\r\n";
		String line3 = "Date: "
				+ convertDateToString(System.currentTimeMillis())
				+ "\r\n";

		String header = line1 + line2 + line3;
		return header;
	}


// add information of the response file.
private String goodHeader(String s, File f) {

		long fileSize = f.length(); 

		// get last modified date for file
		String lastMod = convertDateToString(f.lastModified());

		String line4 = "Last-Modified: " + lastMod + "\r\n";
		String line5 = "Content-Length: " + fileSize + "\r\n\r\n";

		String header = s + line4 + line5;
		return header;
	}

	
//add information to show the file is bad or not found.
	private String badHeader(String s) {

		String[] arr = s.split("\r\n");
		String line1 = arr[0];
		String line4 = "Content-Type: text/html\r\n\r\n";
		String line5 = "<html>\r\n <body>\r\n" + line1
				+ " \r\n </body>\r\n  </html> ";
		String header = s + line4 + line5;
		return header;

	}


// get the status of the file requested.
	public static String checkRequest(String url) {

		String ok = "200 OK";
		String bad = "400 Bad Request";
		String notFound = "404 Not Found";

// check if the request format is wrong.
		if (url.equals("")) {
			return bad;
		}

		String[] arr = url.split("\r\n");
		String test = arr[0].substring(0, 4);

		if (!(test.equals("GET "))) {
			return bad;
		}

		if (!(arr[0].contains("HTTP/1.0"))) {
			if (!(arr[0].contains("HTTP/1.1")))
				return bad;
		}

		File f = getFile(url);
		//check if the file exists.
		if (f.exists()) {
			return ok;
		}
		return notFound;
	}

   
	public static String convertDateToString(long lastMod) {

		DateFormat d = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		d.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date = d.format(lastMod);

		return date;
	}


// get file from a given url. 
    public static File getFile(String url) {

		String[] arr = url.split("\r\n");
		String[] arr1 = arr[0].split(" ");
		String path = arr1[1].substring(1);

		File f = new File(path);

		return f;

	}


}