package sk.upjs.kopr.file_copy.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.server.Server;

class FileReceiveTaskTest {

	private File destDir = new File("./");
	private InetAddress inetAddress;
	private int serverPort = Server.SERVER_PORT;
	private FileInfo fileInfo;
	private File file;
	
	public FileReceiveTaskTest() throws UnknownHostException {
		fileInfo = FileInfoReceiver.getLocalhostServerFileInfo();
		if (fileInfo == null) {
			fail("Cannot test client, server unreachable");
			return;
		}
		file = new File(destDir,fileInfo.fileName);
		inetAddress = InetAddress.getByName("localhost");
	}
	
	@BeforeEach
	void setUp() throws Exception {
		if (file.exists()) {
			file.delete();
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		// file.delete();
	}

	@Test
	void testReceiveWholeFile() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		try {
			
			FileReceiveTask task = new FileReceiveTask(file, fileInfo.size, 0, fileInfo.size, inetAddress, serverPort,0);
			Future<Void> future = executorService.submit(task);
			try {
				future.get();
			} catch (InterruptedException e) {
				fail(e);
			} catch (ExecutionException e) {
				fail(e);
			}
		    long mismatch = Files.mismatch(file.toPath(), Server.FILE_TO_SHARE.toPath());
		    assertEquals(-1, mismatch); // checking that files have the same content			
		} catch (IOException e) {
			fail("cannot create task");
		} finally {
			executorService.shutdown();
		}
	}

}
