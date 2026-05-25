package com.coit20258.drs;

import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;

import com.coit20258.drs.util.Database;
import com.coit20258.drs.util.SceneManager;

public class Drs extends Application {
    
    private static final Logger LOGGER = Logger.getLogger(Drs.class.getName());
    
    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("DRS-Enhanced starting up...");
 
//        Database.boot();
        SceneManager.init(primaryStage, 1100, 720); 
        SceneManager.switchTo("LoginView");
 
        LOGGER.info("DRS-Enhanced ready.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
