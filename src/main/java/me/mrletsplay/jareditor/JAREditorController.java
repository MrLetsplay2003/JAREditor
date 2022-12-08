package me.mrletsplay.jareditor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import me.mrletsplay.jareditor.format.ClassFileFormatter;
import me.mrletsplay.jareditor.format.ClassFileParser;
import me.mrletsplay.jareditor.syntax.SyntaxHighlighting;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;

public class JAREditorController {

	@FXML
	private SplitPane paneEdit;

	@FXML
	private TreeView<String> treeFiles;

	@FXML
	private CodeArea areaEdit;

	public void init() {
		areaEdit = new CodeArea();
		paneEdit.getItems().add(new VirtualizedScrollPane<>(areaEdit));
		areaEdit.setStyle("-fx-font-family: 'DejaVu Sans Mono'");
		areaEdit.textProperty().addListener((obs, oldValue, newValue) -> {
			areaEdit.setStyleSpans(0, SyntaxHighlighting.computeHighlighting(areaEdit.getText()));
		});


		treeFiles.getSelectionModel().selectedItemProperty().addListener(v -> {
			TreeItem<String> it = treeFiles.getSelectionModel().getSelectedItem();
			if(it == null) return;
			List<String> path = new ArrayList<>();
			while(it != treeFiles.getRoot()) {
				path.add(it.getValue());
				it = it.getParent();
			}
			Collections.reverse(path);
			String fullPath = "/" + path.stream().collect(Collectors.joining("/"));
			try {
				Path p = JAREditor.openFileSystem.getPath(fullPath);
				if(Files.isDirectory(p)) return;
				if(fullPath.endsWith(".class")) {
					try {
						ClassFile cf = new ClassFile(Files.newInputStream(p));
						JAREditor.editedClass = cf;
						areaEdit.replaceText(ClassFileFormatter.formatClass(cf));
						areaEdit.moveTo(0);
						areaEdit.requestFollowCaret();
					}catch(Exception e) {
						e.printStackTrace();
						areaEdit.replaceText("Failed to load class: " + e.toString());
					}
				}else {
					areaEdit.replaceText(Files.readString(p, StandardCharsets.UTF_8));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@FXML
	void open(ActionEvent event) {
		FileChooser ch = new FileChooser();
		ch.getExtensionFilters().add(new ExtensionFilter("Java archives", "*.jar", "*.war", "*.zip"));
		File f = ch.showOpenDialog(JAREditor.stage);
		if(f == null) return;
		Path jarFile = f.toPath();

		TreeItem<String> root = new TreeItem<>(jarFile.getFileName().toString());
		root.setExpanded(true);
		treeFiles.setRoot(root);

		JAREditor.openFile(jarFile);
		add(root, JAREditor.openFileSystem, JAREditor.openFileSystem.getPath("/"));
	}

	@FXML
	void save(ActionEvent event) {
		String code = areaEdit.getText();
		var p = ClassFileParser.parse(JAREditor.editedClass, code);
		if(p.isErr()) {
			Alert a = new Alert(AlertType.ERROR);
			a.setContentText(p.getErr().toString());
			a.show();
			return;
		}
		ClassFile cf = p.value();
		try(FileOutputStream fOut = new FileOutputStream(new File("/home/mr/Desktop/testing/HelloWorld.class"))) {
			cf.write(fOut);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void add(TreeItem<String> item, FileSystem fs, Path p) {
		try {
			List<Path> children = StreamSupport.stream(Files.newDirectoryStream(p).spliterator(), false)
				.sorted()
				.collect(Collectors.toList());

			if(children.size() == 1 && Files.isDirectory(children.get(0))) {
				Path c = children.get(0);
				item.setValue(item.getValue() + "/" + c.getFileName().toString());
				if(Files.isDirectory(c)) add(item, fs, c);
			}else {
				for(Path c : children) {
					TreeItem<String> it = new TreeItem<>(c.getFileName().toString());
					item.getChildren().add(it);
					if(Files.isDirectory(c)) add(it, fs, c);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
