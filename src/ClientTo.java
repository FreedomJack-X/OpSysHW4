/*
 * Samuel Yuan
 * Renjie Xie
 * Xi Xi
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientTo {
	private DataOutputStream toServer;
	private DataInputStream fromServer;
	public static void main( String[] args )
	{
		new ClientTo();
	}

	public ClientTo()
	{
		Scanner keyboard = new Scanner( System.in );

		try
		{
			// Create a socket to connect to the server
			Socket socket = new Socket( "localhost", 8765 );

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
				
				// Display result
				long threadId = Thread.currentThread().getId();
				System.out.println("[thread " + threadId + "] Rcvd: " + result);
			}
		}
		catch ( IOException ex )
		{
			System.err.println( ex );
		}
	}
}
