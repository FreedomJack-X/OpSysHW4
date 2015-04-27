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
			ServerSocket serverSocket = new ServerSocket(8765);
			Socket socket;
			
			while ( true )
			{
				//create new client
				Client thread1 = new Client("Thread-1");
			    thread1.start();
				
				// Listen for a connection request
				socket = serverSocket.accept();   // BLOCK
				
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
				
				//ADD <filename> <bytes>\n
				if (firstWord.equals("ADD"))
				{
					if (splitStr.length != 3)
					{
						outputToClient.writeUTF("ERROR: format should be ADD <filename> <bytes>\n");
						break;
					}
					
					String filename = splitStr[1];
					int numBytes = Integer.valueOf(splitStr[2]);
					
					System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
					System.out.println("[thread " + thread1.getId() + "] Transferred file (" + numBytes + " bytes)");
					System.out.println("[thread " + thread1.getId() + "] Sent: ACK");
					System.out.println("[thread " + thread1.getId() + "] Client closed its socket....terminating");
					
					//Upload file data
					String filepath = "storage\\" + filename;
					byte[] bytes = new byte[numBytes];
					
					BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(filename));
					bufferedInput.read(bytes, 0, numBytes);
					
					BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(filepath));
					bufferedOut.write(bytes);
					bufferedOut.flush();
					
					bufferedInput.close();
					bufferedOut.close();
				}
				//READ <filename> <byte-offset> <length>\n
				else if (firstWord.equals("READ"))
				{
					if (splitStr.length != 4)
					{
						outputToClient.writeUTF("ERROR: format should be READ <filename> <byte-offset> <length>\n");
						break;
					}
					
					System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
				}
				//DELETE <filename>\n
				else if (firstWord.equals("DELETE"))
				{
					if (splitStr.length != 2)
						break;
				}
				//DIR\n
				else if (firstWord.equals("DIR"))
				{
					if (splitStr.length != 1)
						break;
				}
								
				// Send area back to the client
				//outputToClient.writeUTF(command);
			}
			
			socket.close();
		}
		catch( IOException ex )
		{
			System.err.println( ex );
		}
		
		System.out.println( "Multithreaded Server ended at " + new Date() );
	}
}
