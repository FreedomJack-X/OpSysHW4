import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{
	public static void main( String[] args )
	{
		new Server();
	}

	public Server()
	{
		System.out.println( "Multithreaded Server started at " + new Date() );

		try
		{
			// Create a server socket that will listen on port 9889
			ServerSocket serverSocket = new ServerSocket( 9889 );
			
			Client T1 = new Client("Thread-1");
		    T1.start();

			while ( true )
			{
				// Listen for a connection request
				Socket socket = serverSocket.accept();   // BLOCK

				System.out.println( "Client connection rcvd at " + new Date() );

				// Create data input and output streams
				//  for primitive data types
				DataInputStream inputFromClient =
						new DataInputStream( socket.getInputStream() );
				DataOutputStream outputToClient =
						new DataOutputStream( socket.getOutputStream() );

				// below is the application-level protocol
				String command = inputFromClient.readUTF(); // BLOCK

				String[] splitStr = command.split(" ");
				String firstWord = splitStr[0];
				System.out.println("First word is : " + firstWord);
				
				//STORE <filename> <bytes>\n<file-contents>
				if (firstWord.equals("STORE"))
				{
					if (splitStr.length != 3)
						return;
				}
				//READ <filename> <byte-offset> <length>\n
				else if (firstWord.equals("READ"))
				{
					if (splitStr.length != 4)
						return;
				}
				//DELETE <filename>\n
				else if (firstWord.equals("DELETE"))
				{
					if (splitStr.length != 2)
						return;
				}
				//DIR\n
				else if (firstWord.equals("DIR"))
				{
					if (splitStr.length != 1)
						return;
				}
								
				// Send area back to the client
				outputToClient.writeUTF(firstWord);
				
				System.out.println( "Command received from client: " + command);

				socket.close();
			}
		}
		catch( IOException ex )
		{
			System.err.println( ex );
		}
		
		System.out.println( "Multithreaded Server ended at " + new Date() );
	}
}
