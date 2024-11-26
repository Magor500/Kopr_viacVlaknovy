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
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.FileRequest;

public class Client {
	public static final int NUMBER_OF_WORKERS = Runtime.getRuntime().availableProcessors();
	
	public static void main(String [] args) {
		
		try {	
			FileInfo info = FileInfoReceiver.getLocalhostServerFileInfo();
			System.out.println(info.fileName + " "+ info.size);
			
			File fileCopy = new File(info.fileName);
			
			File numberOfThreads = new File("numberOfThreads.txt");
			
			long lenght = info.size/(NUMBER_OF_WORKERS);
				
			if(numberOfThreads.exists()) {
				try(Scanner sc = new Scanner(numberOfThreads)){
					int threads = sc.nextInt();
					File[] filesProgress = new File[threads];
					
					ExecutorService executor = Executors.newFixedThreadPool(threads);
					
					for(int i = 0; i < threads;i++) {
						filesProgress[i] = new File("progress" + i +".txt");
						
						if(filesProgress[i].exists()) {
							try(Scanner sc1 = new Scanner(filesProgress[i])){
								String buffer = sc1.nextLine();
								String[] bufferSplit = buffer.split(" ");
								if(bufferSplit.length == 3) {
									//System.out.println(Long.parseLong(bufferSplit[1])+ " " +Math.abs( Math.abs(Long.parseLong(bufferSplit[0]) - Long.parseLong(bufferSplit[1])) - Long.parseLong(bufferSplit[2])) );
									FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  Long.parseLong(bufferSplit[1]), 
											Math.abs( Math.abs(Long.parseLong(bufferSplit[0]) - Long.parseLong(bufferSplit[1])) - Long.parseLong(bufferSplit[2])), InetAddress.getLocalHost(), 5000,i);
									executor.submit(task);	
								}	
							}
						}else {
							long offset = (long )i * lenght;
							FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  offset, lenght, InetAddress.getLocalHost(), 5000,i);
							executor.submit(task);
						}
					}
					
					File lastChunk = new File("progress"+threads+".txt");
					
					if(lastChunk.exists()) {
						try(Scanner sc1 = new Scanner(lastChunk)){
							String buffer = sc1.nextLine();
							String[] bufferSplit = buffer.split(" ");
							if(bufferSplit.length == 3) {
								FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  Long.parseLong(bufferSplit[1]), 
										( Math.abs( Math.abs(Long.parseLong(bufferSplit[0]) - Long.parseLong(bufferSplit[1])) - Long.parseLong(bufferSplit[2]))  ), InetAddress.getLocalHost(), 5000,threads);
								executor.submit(task);
							}
						}
						
					}else {
						if(info.size != lenght*threads) {
							long offset = info.size - (lenght*(threads));
							FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  offset, lenght, InetAddress.getLocalHost(), 5000,threads);
							executor.submit(task);
						}
					}
							
					executor.submit(() -> {
						numberOfThreads.deleteOnExit();
						
						for(int i = 0 ; i < threads;i++) {
							File file = new File("progress" + i +".txt");
							file.deleteOnExit();
						}
						if(info.size != lenght*threads) 
						lastChunk.deleteOnExit();
						
					});
					
					executor.shutdown();
					executor.awaitTermination(30, TimeUnit.MINUTES);	
					
					if(executor.isTerminated()) {
						try {
							MyFileWriter mfw = MyFileWriter.getInstance(fileCopy, info.size);
							mfw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				
			}else {
				ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
				
				try {
					numberOfThreads.createNewFile();
					
					FileWriter fw = new FileWriter(numberOfThreads, false);
					fw.write(String.valueOf(NUMBER_OF_WORKERS));
					fw.close();
				} catch (IOException e) {
						// TODO Auto-generated catch block
					e.printStackTrace();
				}
					
				for(int i = 0; i < NUMBER_OF_WORKERS;i++) {
					long offset = (long )i * lenght;
					FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  offset, lenght, InetAddress.getLocalHost(), 5000,i);
					executor.submit(task);
				}
				
				if(info.size != lenght*NUMBER_OF_WORKERS) {
					long offset = info.size - (lenght*(NUMBER_OF_WORKERS));
					FileReceiveTask task =  new FileReceiveTask(fileCopy, info.size,  offset, lenght, InetAddress.getLocalHost(), 5000,NUMBER_OF_WORKERS);
					executor.submit(task);
				}
				
				executor.submit(() -> {
					numberOfThreads.deleteOnExit();
					
					for(int i = 0 ; i < NUMBER_OF_WORKERS;i++) {
						File file = new File("progress" + i +".txt");
						file.deleteOnExit();
					}
					
					if(info.size != lenght*NUMBER_OF_WORKERS) {
						File file = new File("progress" + NUMBER_OF_WORKERS +".txt");
						file.deleteOnExit();
					}
				});
				
				executor.shutdown();
				executor.awaitTermination(30, TimeUnit.MINUTES);
				
				if(executor.isTerminated()) {
					
					try {
						MyFileWriter mfw = MyFileWriter.getInstance(fileCopy, info.size);
						mfw.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			
			
			//File progress = new File("progress.txt");
			
			//ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
			
			/*if(progress.exists()) {
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
			}*/
			
			
			
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
