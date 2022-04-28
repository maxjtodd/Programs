package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Scanner;
import java.text.SimpleDateFormat;  
import java.util.Date; 


public class WebWorker implements Runnable
{

	private Socket socket;
	
	private File file;
	private boolean error404;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
		file = null;
		error404 = false;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			readHTTPRequest(is);
			writeHTTPHeader(os, "text/html");
			writeContent(os);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private File readHTTPRequest(InputStream is)
	{
		String line;
		File f;
		
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
				
				
				// check if file exists
				if (line.contains("GET ")) {
					
					// get the file path in line 
					line = line.replace("GET /", "");
					line = line.replace(" HTTP/1.1", "");
					
					f = new File(line);
					
					
					// file exists, set private file object to a file object with the file path
					if (f.exists()) {
						file = new File(line);
						error404 = false;
					}
					// file doesn't exist
					else {
						// file accessed isn't home page, 404 error
						if (!line.contains("favicon.ico") && !line.equals("")) {							
							error404 = true;
							System.out.println("NO FAVICON OR BLANK");
						}
						// file accessed is home page
						else {
							error404 = false;
						}
						
					}
					
					System.out.println(f.getAbsolutePath() + "\t" + line + "\terror404 = " + error404);
				}
				
				
				
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return null;
	}
	
	
	
	

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		if (!error404)
			os.write("HTTP/1.1 200 OK\n".getBytes());
		else
			os.write("HTTP/1.1 404 NOT FOUND\n".getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception
	{	
		// 404 error, show error page
		if (error404) {
			
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>ERROR 404</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
			
		}
		// no file was requested, show home page
		else if (file == null) {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>My web server works!</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}
		// file was requested, show file contents
		else {
			
			Scanner reader = new Scanner(file);
			String line = "";
			
			while(reader.hasNextLine()) {
				
				line = reader.nextLine();
				line = convertTags(line);
				os.write(line.getBytes());
				
				
			}
			
		}
		
	}
	
	
	/**
	 * Converts custom HTML tags in a given string to desired output
	 * @param line String to convert tags 
	 * @return String with converted tags
	 */
	private String convertTags(String line) {
		
		String output = line;
		
		// formatting <cs371date> tag 
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");  
	    Date currentDate = new Date(); 
		output = output.replaceAll("<cs371date>", format.format(currentDate));
		
		// formatting <cs371server> tag
		output = output.replaceAll("<cs371server>", "Max Todd's Server");
		
		return output;
	}
	
	

} // end class
