package sk.upjs.kopr.file_copy.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.FileRequest;

public class Client {
	public static final int NUMBER_OF_WORKERS = Runtime.getRuntime().availableProcessors()/2;
	private static final int BLOCK_SIZE = 16384;
	
	public static void main(String [] args) {
		
		try {	
			
			FileInfo info = FileInfoReceiver.getLocalhostServerFileInfo();
			System.out.println(info.fileName + " "+ info.size);
			
			File fileCopy = new File(info.fileName);
			
			File progress = new File("progress.txt");
			
			ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
			
			if(progress.exists()) {
				try(Scanner sc = new Scanner(progress)){
					long progressOffset = sc.nextLong();
					
					int numberOfOffsets = (int) ((info.size-progressOffset) / BLOCK_SIZE);
					if(numberOfOffsets*BLOCK_SIZE != info.size) numberOfOffsets++;
					
					for(int i = 0; i < numberOfOffsets;i++) {
						long offset = (long) i * BLOCK_SIZE + progressOffset;
						long length = Math.min(BLOCK_SIZE, info.size - offset);
						
						FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  offset, length, InetAddress.getLocalHost(), 5000);
						executor.submit(task);
					}
				}
			}else {
				int numberOfOffsets = (int) (info.size / BLOCK_SIZE);
				if(numberOfOffsets*BLOCK_SIZE != info.size) numberOfOffsets++;
		
				for(int i = 0; i < numberOfOffsets;i++) {
					long offset = (long) i * BLOCK_SIZE;
					long length = Math.min(BLOCK_SIZE, info.size - offset);
					
					FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  offset, length, InetAddress.getLocalHost(), 5000);
					executor.submit(task);
				}				
			}
			
			executor.shutdown();
			executor.awaitTermination(30, TimeUnit.MINUTES);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
