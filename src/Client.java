import java.io.*;
import java.net.*;
import java.util.*;

// Define a thread class for handling the incoming connection
public class Client extends Thread
{
	private Thread t;
	private String threadName;
	   
	// IO streams
	private DataOutputStream toServer;
	private DataInputStream fromServer;

	public Client(String name)
	{
		threadName = name;
		System.out.println("Creating " +  threadName );
	}

	public void run() 
	{
		Scanner keyboard = new Scanner( System.in );

		try
		{
			// Create a socket to connect to the server
			Socket socket = new Socket("localhost", 8765);

			// Create an input stream to receive data from the server
			fromServer = new DataInputStream( socket.getInputStream() );

			// Create an output stream to send data to the server
			toServer = new DataOutputStream( socket.getOutputStream() );
			
			System.out.println("Listening on port " + socket.getPort());
			System.out.println("Received incoming connection from " + socket.getInetAddress().getCanonicalHostName());

			while ( true )
			{
				// Get the command from the user
				System.out.print( "Enter command: " );
				String command = keyboard.nextLine();
				
				// Send the command to the server
				toServer.writeUTF(command);
				toServer.flush();
				//System.out.println( "Sent command " + command + " to server" );

				// Get result from the server
				String result = fromServer.readUTF(); //block
				
				long threadId = Thread.currentThread().getId();
				
				// Display result
				System.out.println( "[thread " + threadId + "] Rcvd: " + result);
			}
		}
		catch ( IOException ex )
		{
			System.err.println( ex );
		}
	}
	
	public void start ()
	{
		//System.out.println("Starting thread");
		if (t == null)
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}
}
