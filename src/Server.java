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
				
				// Create data input and output streams for primitive data types
				DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
				DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());

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
					
					addFileToServer(thread1, command);
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
					
					//check if file exists
					File file = new File(splitStr[1]);
					if (!file.isFile())
					{
						outputToClient.writeUTF("Sent: ERROR NO SUCH FILE\n");
						break;
					}
					
					readFileOnServer(thread1, command);
				}
				//DELETE <filename>\n
				else if (firstWord.equals("DELETE"))
				{
					if (splitStr.length != 2)
					{
						outputToClient.writeUTF("ERROR: format should be DELETE <filename>\n");
						break;
					}
					
					deleteFileFromServer(thread1, command);				
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
	
	public void addFileToServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];
		int numBytes = Integer.valueOf(splitStr[2]);
		
		System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
		System.out.println("[thread " + thread1.getId() + "] Transferred file (" + numBytes + " bytes)");
		System.out.println("[thread " + thread1.getId() + "] Sent: ACK");
		System.out.println("[thread " + thread1.getId() + "] Client closed its socket....terminating");
		
		//Upload file data
		String destPath = "storage\\" + sourcePath;
		byte[] bytes = new byte[numBytes];

		try
		{
			BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(sourcePath));
			bufferedInput.read(bytes, 0, numBytes);

			BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(destPath));
			bufferedOut.write(bytes);
			bufferedOut.flush();

			bufferedInput.close();
			bufferedOut.close();
		}
		catch (IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	public void readFileOnServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];
		int byteOffset = Integer.valueOf(splitStr[2]);
		int fileLength = Integer.valueOf(splitStr[3]);
		
		//constants
		int totalFrames = 32;
		int frameSize = 1024;
		int fileFrames = 4; 
		byte[] memory = new byte[totalFrames * frameSize];
		
		int bytesLeft = fileLength;
		int currentByteOffset = byteOffset;
		int currentPage = currentByteOffset / frameSize;
		int currentFrame = 0;
		
		while (bytesLeft > 0)
		{
			System.out.println("[thread " + thread1.getId() + "] Allocated page " + currentPage + 
					" to frame " + currentFrame);

			int numBytesSent = (currentPage + 1) * frameSize - currentByteOffset;
			System.out.println("[thread " + thread1.getId() + "] Sent: ACK " + numBytesSent);
			System.out.println("[thread " + thread1.getId() + "] Transferred " + numBytesSent + " bytes " +
					"from offset " + currentByteOffset);
			
			//save file data to memory
			byte[] bytes = new byte[totalFrames * frameSize];
			
			try 
			{
				//read
				BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(sourcePath));
				bufferedInput.read(bytes, currentByteOffset, numBytesSent);

				//write
				for (int i = currentByteOffset; i < currentByteOffset + numBytesSent; i++)
				{
					memory[i] = bytes[i];
				}

				bufferedInput.close();

				//increment counters
				currentPage++;
				currentFrame++;
				currentFrame %= fileFrames;
				currentByteOffset += numBytesSent;
				bytesLeft -= numBytesSent;
			}
			catch (IOException ex)
			{
				System.err.println(ex);
			}
		}
	}
	
	public void deleteFileFromServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];
		
		int fileFrames = 4; 
				
		System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
		for (int i = 0; i < fileFrames; i++)
		{
			System.out.println("[thread " + thread1.getId() + "] Deallocated frame " + i);
		}

		//delete file data
		String destPath = "storage\\" + sourcePath;
		File file = new File(destPath);
		 
		if(file.delete())
		{
			System.out.println("[thread " + thread1.getId() + "] Deleted " + sourcePath + " file");
			System.out.println("[thread " + thread1.getId() + "] Sent: ACK");
		}
		else
		{
			System.out.println("[thread " + thread1.getId() + "] Deleted failed.");
		}
		
		System.out.println("[thread " + thread1.getId() + "] Client closed its socket....terminating");
	}
}
