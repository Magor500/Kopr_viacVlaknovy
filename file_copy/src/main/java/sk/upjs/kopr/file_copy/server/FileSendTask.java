package sk.upjs.kopr.file_copy.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.FileRequest;

public class FileSendTask implements Runnable {

	private static final int BLOCK_SIZE = 16384; // 16 kB
	private final File fileToSend;
	private final Socket socket;
	
	public FileSendTask(File fileToSend, Socket socket) throws FileNotFoundException {
		this.fileToSend = fileToSend;
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			ObjectInputStream ois = null;
			ObjectOutputStream oos = null;
			try(RandomAccessFile raf = new RandomAccessFile(fileToSend, "r")) {
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());
				String command = ois.readUTF();
				if (command.equals("info")) {
					oos.writeObject(new FileInfo(fileToSend.getName(), fileToSend.length()));
					oos.flush();
					return;
				}
				if (! command.equals("file")) {
					oos.writeUTF("unknown command");
					return;
				}
				FileRequest fileRequest = (FileRequest) ois.readObject();
				if (fileRequest.offset < 0 || fileRequest.length < 0 || fileRequest.offset + fileRequest.length > fileToSend.length()) {
					throw new RuntimeException(socket.getInetAddress() + ":" + socket.getPort() + " : " 
											   + fileRequest + " exceeds the file size " + fileToSend.length());
				}
				raf.seek(fileRequest.offset);
				byte[] buffer = new byte[BLOCK_SIZE];
				for (long send = 0; send < fileRequest.length; send += BLOCK_SIZE) {
					if (ois.available() > 0) {
						throw new RuntimeException(socket.getInetAddress() + ":" + socket.getPort() + " : "  
												   + "Premature closing data stream after " + send + " bytes send for " + fileRequest);
					}
					int size = (int) Math.min(BLOCK_SIZE, fileRequest.length - send); 
					raf.read(buffer, 0, size);
					oos.write(buffer, 0, size);
				}
				oos.flush();
				
			} finally {
				if (oos != null) oos.close();
				if (ois != null) ois.close();
				if (socket != null && socket.isConnected()) socket.close();
			}
		} catch (Exception e) {
			
		}
	}
}
