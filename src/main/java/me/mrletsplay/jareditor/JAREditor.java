package me.mrletsplay.jareditor;

import java.net.URL;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.mrletsplay.jareditor.file.OpenedFile;

public class JAREditor extends Application {

	public static Stage stage;

	public static OpenedFile openedFile;

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.getIcons().add(new Image("/jareditor.png"));
		URL url = JAREditor.class.getResource("/jareditor.fxml");
		FXMLLoader loader = new FXMLLoader(url);
		Parent p = loader.load(url.openStream());
		JAREditorController c = loader.getController();
		c.init();
		Scene scene = new Scene(p);
		scene.getStylesheets().add(JAREditor.class.getResource("/highlight.css").toExternalForm());

		primaryStage.setTitle("JAREditor");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static OpenedFile openFile(Path filePath) {
		if(openedFile != null) {
			openedFile.close();
		}

		return openedFile = new OpenedFile(filePath);
	}

}
