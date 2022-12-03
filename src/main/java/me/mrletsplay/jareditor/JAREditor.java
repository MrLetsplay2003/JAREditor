package me.mrletsplay.jareditor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;

public class JAREditor extends Application {

	public static Stage stage;

	public static Path openFilePath;
	public static FileSystem openFileSystem;

	public static ClassFile editedClass;

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
		scene.getStylesheets().add(JAREditor.class.getResource("/keywords.css").toExternalForm());

		primaryStage.setTitle("JAREditor");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void openFile(Path jarFilePath) {
		openFilePath = jarFilePath;
		try {
			if(openFileSystem != null) {
				openFileSystem.close();
			}

			openFileSystem = FileSystems.newFileSystem(jarFilePath, (ClassLoader) null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
