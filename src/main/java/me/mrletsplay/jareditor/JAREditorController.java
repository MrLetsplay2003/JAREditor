package me.mrletsplay.jareditor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import me.mrletsplay.jareditor.format.ByteCodeParser;
import me.mrletsplay.jareditor.format.ClassFileFormatter;
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
	}

	@FXML
	void open(ActionEvent event) {
		treeFiles.getSelectionModel().selectedItemProperty().addListener(v -> {
			TreeItem<String> it = treeFiles.getSelectionModel().getSelectedItem();
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

		Path jarFile = Paths.get("/home/mr/Downloads/MrCore-4.2-SNAPSHOT.jar");

		TreeItem<String> root = new TreeItem<>(jarFile.getFileName().toString());
		root.setExpanded(true);
		treeFiles.setRoot(root);

		JAREditor.openFile(jarFile);
		add(root, JAREditor.openFileSystem, JAREditor.openFileSystem.getPath("/"));
	}

	@FXML
	void save(ActionEvent event) {
		String code = areaEdit.getText();
		ByteCodeParser.parse(JAREditor.editedClass, code);
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
