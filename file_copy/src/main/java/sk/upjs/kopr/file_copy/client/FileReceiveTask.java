package sk.upjs.kopr.file_copy.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import sk.upjs.kopr.file_copy.FileRequest;

public class FileReceiveTask implements Callable<Void>{
	private static final int BUFFER_SIZE = 16384;
	private MyFileWriter myFileWriter;
	private long offset;
	private long length; // length of data to be received
	private InetAddress inetAddress;
	private int serverPort;
	private int indexOfThread;
	
	public FileReceiveTask(File fileToSave, long fileSize, long offset, long length, InetAddress inetAddress, int serverPort, int indexOfThread) throws IOException {
		this.offset = offset;
		this.length = length;
		this.inetAddress = inetAddress;
		this.serverPort = serverPort;
		this.indexOfThread = indexOfThread;
		myFileWriter = MyFileWriter.getInstance(fileToSave, fileSize);
	}

	@Override
	public Void call() throws Exception {
		
		
		try(Socket socket = new Socket(inetAddress, serverPort)) {
			
			File file = new File("progress" + indexOfThread +".txt");
			//File numberOfThreads = new File("numberOfThreads.txt");
			
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeUTF("file");
			oos.flush();
			FileRequest fileRequest = new FileRequest(offset, length);
			oos.writeObject(fileRequest);
			oos.flush();
			long fileOffset = offset;
			while(true) {	
				byte[] bytes = ois.readNBytes(BUFFER_SIZE);
				if (bytes.length > 0) {
					myFileWriter.write(fileOffset, bytes, 0, bytes.length);
				}
					
				if (bytes.length < BUFFER_SIZE) {
					try {
						if(!file.exists()) {
							file.createNewFile();	
						}
						
						FileWriter fw = new FileWriter(file, false);
						fw.write("Finished");
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					//System.out.println(bytes.length);
					//System.out.println(indexOfThread);
					
					//file.deleteOnExit();
					//numberOfThreads.deleteOnExit();
					oos.close();
					ois.close();
					break;
				}
				fileOffset += bytes.length;
				if ((fileOffset / BUFFER_SIZE) % 1000 == 0) {
					System.out.println(fileOffset);
					
					synchronized (file) {
						try {
							if(!file.exists()) {
								file.createNewFile();	
							}
							
							FileWriter fw = new FileWriter(file, false);
							fw.write(String.valueOf(offset) + " " + String.valueOf(fileOffset) + " " + String.valueOf(length));
							fw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		}
		
		return null;
	}

}
