/*
 * Samuel Yuan
 * Renjie Xie
 * Xi Xi
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{
	private int totalFrames;
	//private int currentFrameOffset;
	private int frameSize;
	private int fileFrames;
	private byte[][] memory;
	private Map<Integer, Integer> pageTable; //frame and page assocation
	private Map<Integer, String> frameFile;	 //frame and file names
	private Map<String, Integer> fileFrame;
	private Map<Integer, Integer> frameRank;
	//private 
	
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
			//currentFrameOffset = 0; //start at frame 0
			frameSize = 1024;
			fileFrames = 4;
			//create block of memory
			memory = new byte[totalFrames][];
			pageTable = new HashMap<Integer, Integer>();
			frameFile = new HashMap<Integer, String>();
			fileFrame = new HashMap<String, Integer>();
			frameRank = new HashMap<Integer, Integer>();
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
				return "ERROR: FILE DOESN'T EXIST\n";
			}
			
			//check if file has already been uploaded
			String destPath = "storage\\" + sourcePath;
			File destFile = new File(destPath);
			if (destFile.exists())
			{
				return "ERROR: FILE EXISTS\n";
			}
			
			addFileToServer(thread1, command);
			//return "ACK\n";
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
		
		// if the file exist in frame
		if(fileFrame.containsKey(sourcePath))
		{
			currentFrame = fileFrame.get(sourcePath);
		}
		else{
			// if not, find free frame space assign to file
			for(int i = 0; i < totalFrames; i++)
			{
				if(!frameFile.containsKey(i))
				{
					currentFrame = i;
					fileFrame.put(sourcePath, i);
					frameRank.put(i, 1);
					frameRank.put(i+1, 2);
					frameRank.put(i+2, 3);
					frameRank.put(i+3, 4);
					break;
				}
			}
		}
		String destPath = "storage/" + sourcePath;
		File currentFile = new File(destPath);
		
		//print error msg if byte range is invalid
		//if byteoffset or filelength is larger than current file
		//if byteoffset is greater than filelength (param 2 should be less than param 3)
		if (byteOffset < 0 || (byteOffset+fileLength) > currentFile.length() || fileLength < 0)
		{
			System.out.println("ERROR: INVALID BYTE RANGE");
			return;
		}
		int actualFrame = currentFrame;
		while (bytesLeft > 0)
		{
			boolean inMemory = false;
			int standard = 5;
			// find  if the page need to assign already in memory
			for(int k=0; k<4; k++)
			{
				// check if the frame-page association already in pageTable
				if(pageTable.containsKey(actualFrame+k))
				{
					// check the if the currentPage equal to the page associate with frame
					if(currentPage == pageTable.get(actualFrame+k))
					{
						// check if the memory in frame is in full size
						int count = 0;
						for(int u = 0; u < frameSize; u++)
						{
							if(memory[actualFrame+k][u]!=0)
							{
								count++;
							}
						}
						if(count == frameSize)
						{
							// assign the page information to result
							byte[] result = memory[actualFrame+k];
							for(int g = 0; g < 4; g++)
							{
								if(g==k)
								{
									frameRank.put(actualFrame+g,4);
								}
								else
								{
									frameRank.put(actualFrame+g,frameRank.get(actualFrame+g)-1);
								}
							}
							inMemory = true;
							currentFrame = actualFrame + k;
							break;
						}
					}
				}
			}
			if(!inMemory)
			{
				// find smallest rank frame will be replaced
				for(int p=0; p<4; p++)
				{
					if(frameRank.get(actualFrame + p) < standard)
					{
						standard = frameRank.get(actualFrame + p);
						currentFrame = actualFrame + p;
					}
				}
				if (memory[currentFrame] == null)
				{
					memory[currentFrame] = new byte[frameSize];
					frameFile.put(currentFrame, sourcePath);
					pageTable.put(currentFrame, currentPage);
					System.out.println("[thread " + thread1.getId() + "] Allocated page " + currentPage + 
						" to frame " + currentFrame);
				}
				else
				{
					memory[currentFrame] = new byte[frameSize];
					int oldPage = pageTable.get(currentFrame);
					pageTable.put(currentFrame, currentPage);
					System.out.println("[thread " + thread1.getId() + "] Allocated page " + currentPage + 
							" to frame " + currentFrame + " (replaced page " + oldPage + ")");
				}
			}
			else
			{
				int oldPage = pageTable.get(currentFrame);
				System.out.println("[thread " + thread1.getId() + "] Allocated page " + currentPage + 
						" to frame " + currentFrame + " (Same as " + oldPage + ")");
			}
			
			
			int numBytesSent;
			if(bytesLeft > frameSize)
			{
				numBytesSent = (currentPage + 1) * frameSize - currentByteOffset;
			}
			else
			{
				numBytesSent = bytesLeft;
			}
			System.out.println("[thread " + thread1.getId() + "] Sent: ACK " + numBytesSent);
			System.out.println("[thread " + thread1.getId() + "] Transferred " + numBytesSent + " bytes " +
					"from offset " + currentByteOffset);
			
			//save file data to memory
			byte[] bytes = new byte[totalFrames * frameSize];
			if(!inMemory)
			{
				for(int g = 0; g < 4; g++)
				{
					if(g==currentFrame)
					{
						frameRank.put(actualFrame+g,4);
					}
					else
					{
						frameRank.put(actualFrame+g,frameRank.get(actualFrame+g)-1);
					}
				}
				try 
				{
					//read
					BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(destPath));
					bufferedInput.read(bytes, currentByteOffset, numBytesSent);

					//write
					for (int i = currentByteOffset; i < currentByteOffset + numBytesSent; i++)
					{
						memory[currentFrame][i % frameSize] = bytes[i];
					}

					bufferedInput.close();

				}
				catch (IOException ex)
				{
					System.err.println(ex);
				}
			}
			//increment counters
			currentPage++;
			//currentFrame++;
			//currentFrame %= fileFrames;
			currentByteOffset += numBytesSent;
			bytesLeft -= numBytesSent;
		}
		
		//currentFrameOffset += fileFrames; //move to the next file block in memory
		//currentFrameOffset %= totalFrames; //make sure offset doesn't exceed max number frames in memory (32)
	}
	
	public synchronized void deleteFileFromServer(Thread thread1, String command)
	{
		//parameters
		String[] splitStr = command.split(" ");
		String sourcePath = splitStr[1];

		System.out.println("[thread " + thread1.getId() + "] Rcvd: " + command);
		for (int i = 0; i < totalFrames; i++)
		{
			if(frameFile.containsKey(i))
			{
				if(frameFile.get(i).equals(sourcePath))
				{
					memory[i] = null;
					frameFile.remove(i);
					System.out.println("[thread " + thread1.getId() + "] Deallocated frame " + i);
				}
			}
		}
		if(fileFrame.containsKey(sourcePath))
		{
			fileFrame.remove(sourcePath);
		}

		//delete file data
		String destPath = "storage/" + sourcePath;
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
		
		File folder = new File("storage/");
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
					if (error.equals(""))
					{
						outputToClient.writeUTF(error);
					}
					else if(error.equals("ACK"))
					{
						outputToClient.writeUTF(error);
					}
					else
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
