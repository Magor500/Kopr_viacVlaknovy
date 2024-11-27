package sk.upjs.kopr.file_copy.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sk.upjs.kopr.file_copy.FileInfo;
import javafx.scene.layout.VBox;

public class Gui extends Application{
    private TextField textField;
    private Button startButton;
	private boolean isRunning = false;
    
	@Override
	public void start(Stage primaryStage) throws Exception {
        
		textField = new TextField();
        textField.setPromptText("Select a number of threads");

        startButton = new Button("Start");
      
        Client client = new Client();

        File numberOfThreads = new File("numberOfThreads.txt");
        
        if(numberOfThreads.exists()) {
        	textField.setDisable(true);
        }
        
        
        startButton.setOnAction(event -> {
        	if(numberOfThreads.exists()) {
        		client.start();
            }else {
            	try {
            		Client.setNUMBER_OF_WORKERS(Integer.parseInt(textField.getText()));
                	client.start();
            	}catch (NumberFormatException e) {
            		Alert alert = new Alert(AlertType.WARNING);
            		alert.setContentText("Please insert number");
            		alert.showAndWait();
            	}
            }
        	
        	isRunning = true;
        	
        });

       
       primaryStage.setOnCloseRequest(event -> {
    	   if(isRunning) {
    		   client.shutdownExe();
    	   }
       });

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(textField, startButton);
        
        Scene scene = new Scene(vbox, 400, 200);
        primaryStage.setTitle("Copy file from server");
        primaryStage.setScene(scene);
        primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
		
	}
	
	
}
