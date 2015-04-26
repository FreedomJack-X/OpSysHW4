import java.io.*;
import java.net.*;
import java.util.*;

public class Client
{
	// IO streams
	private DataOutputStream toServer;
	private DataInputStream fromServer;

	public static void main( String[] args )
	{
		new Client();
	}

	public Client()
	{
		Scanner keyboard = new Scanner( System.in );

		try
		{
			// Create a socket to connect to the server
			Socket socket = new Socket( "localhost", 9889 );
			//Socket socket = new Socket( "linux02.cs.rpi.edu", 9889 );
			// Socket socket = new Socket( "128.113.126.29", 9889 );

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
				System.out.println( "Sent command " + command + " to server" );

				// Get area from the server
				String result = fromServer.readUTF(); //block
				
				// Display to the text area
				System.out.println( "Area received from server is " + result );
			}
		}
		catch ( IOException ex )
		{
			System.err.println( ex );
		}
	}
}
