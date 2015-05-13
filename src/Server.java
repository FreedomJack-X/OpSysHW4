import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{
	private int totalFrames;
	private int currentFrameOffset;
	private int frameSize;
	private int fileFrames;
	private byte[][] memory;
	
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
			int clientNumber = 0;
			//constants
			totalFrames = 32;
			currentFrameOffset = 0; //start at frame 0
			frameSize = 1024;
			fileFrames = 4;
			//create block of memory
			memory = new byte[totalFrames][];
			
			while ( true )
			{			
				// Listen for a connection request
				socket = serverSocket.accept();   // BLOCK
				System.out.println("Received incoming connection from "+socket.getInetAddress().getHostName());
				// Create a new thread for the connection
		        HandleAClient thread = new HandleAClient( socket );
		        thread.start();
				
			}
			
			//socket.close();
		}
		catch( IOException ex )
		{
			System.err.println( ex );
		}
		
		System.out.println( "Multithreaded Server ended at " + new Date() );
	}
	
	public String runCommand(Thread thread1, String command)
	{
		String[] splitStr = command.split(" ");
		String firstWord = splitStr[0];
		//System.out.println("First word is : " + firstWord);
			
		//check if the storage directory exists
		File theDir = new File("storage");
		// if the directory does not exist, create it
		if (!theDir.exists())
		{
			theDir.mkdir();
		}

		//ADD <filename> <bytes>
		if (firstWord.equals("ADD") || firstWord.equals("STORE"))
		{
			if (splitStr.length != 3)
			{
				return "ERROR: format should be ADD <filename> <bytes>\n";
			}
			
			//check if file exists on local disk
			String sourcePath = splitStr[1];
			File sourceFile = new File(sourcePath);
			if (!sourceFile.exists())
			{
				return "ERROR: FILE DOESN'T EXIST";
			}
			
			//check if file has already been uploaded
			String destPath = "storage\\" + sourcePath;
			File destFile = new File(destPath);
			if (destFile.exists())
			{
				return "ERROR: FILE EXISTS";
			}
			
			addFileToServer(thread1, command);
		}
		//READ <filename> <byte-offset> <length>\n
		else if (firstWord.equals("READ"))
		{
			if (splitStr.length != 4)
			{
				return "ERROR: format should be READ <filename> <byte-offset> <length>\n";
			}
			
			System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
			
			//check if file exists
			String destPath = "storage/" + splitStr[1];
			File file = new File(destPath);
			if (!file.isFile())
			{
				return "Sent: ERROR NO SUCH FILE\n";
			}
			
			readFileOnServer(thread1, command);
		}
		//DELETE <filename>\n
		else if (firstWord.equals("DELETE"))
		{
			if (splitStr.length != 2)
			{
				return "ERROR: format should be DELETE <filename>\n";
			}
			
			deleteFileFromServer(thread1, command);				
		}
		//DIR\n
		else if (firstWord.equals("DIR"))
		{
			if (splitStr.length != 1)
				return "ERROR: format should be DIR\n";
			
			listFileFromServer(thread1, command);
		}
						
		// Send area back to the client
		//outputToClient.writeUTF(command);
		
		return ""; //no error message
	}
	
	public synchronized void addFileToServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];
		int numBytes = Integer.valueOf(splitStr[2]);
		
		System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
		System.out.println("[thread " + thread1.getId() + "] Transferred file (" + numBytes + " bytes)");
		System.out.println("[thread " + thread1.getId() + "] Sent: ACK");
		//System.out.println("[thread " + thread1.getId() + "] Client closed its socket....terminating");
		
		//Upload file data
		String destPath = "storage/" + sourcePath;
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
	
	public synchronized void readFileOnServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];
		int byteOffset = Integer.valueOf(splitStr[2]);
		int fileLength = Integer.valueOf(splitStr[3]);
		
		int bytesLeft = fileLength;
		int currentByteOffset = byteOffset;
		int currentPage = currentByteOffset / frameSize;
		int currentFrame = 0;
		
		String destPath = "storage/" + sourcePath;
		File currentFile = new File(destPath);
		
		//print error msg if byte range is invalid
		//if byteoffset or filelength is larger than current file
		//if byteoffset is greater than filelength (param 2 should be less than param 3)
		if (byteOffset > currentFile.length() || 
				fileLength > currentFile.length() ||
				byteOffset > fileLength)
		{
			System.out.println("ERROR: INVALID BYTE RANGE");
			return;
		}
		
		while (bytesLeft > 0)
		{
			int actualFrame = currentFrame + currentFrameOffset;
			
			if (memory[actualFrame] == null)
			{
				memory[actualFrame] = new byte[frameSize];
				System.out.println("[thread " + thread1.getId() + "] Allocated page " + currentPage + 
					" to frame " + actualFrame);
			}
			else
			{
				memory[actualFrame] = new byte[frameSize];
				int oldPage = currentPage - fileFrames;
				System.out.println("[thread " + thread1.getId() + "] Allocated page " + currentPage + 
						" to frame " + actualFrame + " (replaced page " + oldPage + ")");
			}
			
			int numBytesSent = (currentPage + 1) * frameSize - currentByteOffset;
			System.out.println("[thread " + thread1.getId() + "] Sent: ACK " + numBytesSent);
			System.out.println("[thread " + thread1.getId() + "] Transferred " + numBytesSent + " bytes " +
					"from offset " + currentByteOffset);
			
			//save file data to memory
			byte[] bytes = new byte[totalFrames * frameSize];
			
			try 
			{
				//read
				BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(destPath));
				bufferedInput.read(bytes, currentByteOffset, numBytesSent);

				//write
				for (int i = currentByteOffset; i < currentByteOffset + numBytesSent; i++)
				{
					memory[actualFrame][i % frameSize] = bytes[i];
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
		
		currentFrameOffset += fileFrames; //move to the next file block in memory
		currentFrameOffset %= totalFrames; //make sure offset doesn't exceed max number frames in memory (32)
	}
	
	public synchronized void deleteFileFromServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];

		System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
		for (int i = 0; i < totalFrames; i++)
		{
			if (memory[i] != null)
			{
				memory[i] = null;
				System.out.println("[thread " + thread1.getId() + "] Deallocated frame " + i);
			}
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
			System.out.println("[thread " + thread1.getId() + "] Delete failed.");
		}
		
		//System.out.println("[thread " + thread1.getId() + "] Client closed its socket....terminating");
	}
	
	public void listFileFromServer(Thread thread1, String command)
	{
		System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
		
		File folder = new File(".");
		int numFiles = folder.listFiles().length;
		//print number files overall
		System.out.println(numFiles);
		//print each file
		for (final File fileEntry : folder.listFiles()) 
		{
			System.out.println(fileEntry.getName());
		}
	}
	
	// Inner class
	// Define the thread class for handling incoming connection
	class HandleAClient extends Thread
	{
		private Socket socket; // A connected socket

		public HandleAClient( Socket socket )
		{
			this.socket = socket;
		}

		public void run()
		{
			try {
				// Create data input and output streams
				DataInputStream inputFromClient = new DataInputStream( socket.getInputStream() );
				DataOutputStream outputToClient = new DataOutputStream( socket.getOutputStream() );

				// Continuously serve the client
				while ( true )
				{
					// below is the application-level protocol
					String command = inputFromClient.readUTF(); // BLOCK
					
					String error = runCommand(this, command);
					if (!error.equals(""))
					{
						outputToClient.writeUTF(error);
						break;
					}
				}
				socket.close();
			}
			catch( IOException ex ) {
				System.err.println( "[thread " + this.getId() + "] Client closed its socket....terminating" );
			}
			
		}
	}
}
