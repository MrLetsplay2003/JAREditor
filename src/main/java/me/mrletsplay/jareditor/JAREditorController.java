package me.mrletsplay.jareditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
import me.mrletsplay.jareditor.file.EditedFile;
import me.mrletsplay.jareditor.file.EditorItem;
import me.mrletsplay.jareditor.file.OpenedFile;
import me.mrletsplay.jareditor.format.ClassFileFormatter;
import me.mrletsplay.jareditor.format.ClassFileParser;
import me.mrletsplay.jareditor.syntax.SyntaxHighlighting;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;

public class JAREditorController {

	@FXML
	private SplitPane paneEdit;

	@FXML
	private TreeView<EditorItem> treeFiles;

	@FXML
	private CodeArea areaEdit;

	public void init() {
		areaEdit = new CodeArea();
		paneEdit.getItems().add(new VirtualizedScrollPane<>(areaEdit));
		areaEdit.setStyle("-fx-font-family: 'DejaVu Sans Mono'");
		areaEdit.textProperty().addListener((obs, oldValue, newValue) -> {
			areaEdit.setStyleSpans(0, SyntaxHighlighting.computeHighlighting(areaEdit.getText()));

			TreeItem<EditorItem> it = treeFiles.getSelectionModel().getSelectedItem();
			if(it == null) return;

			EditorItem item = it.getValue();
			EditedFile edit = item.getFile();
			if(edit == null) return;
			edit.setEditorContents(newValue);
		});


		treeFiles.getSelectionModel().selectedItemProperty().addListener(v -> {
			TreeItem<EditorItem> it = treeFiles.getSelectionModel().getSelectedItem();
			if(it == null) return;

			Path p = it.getValue().getPath();
			if(p == null || Files.isDirectory(p)) {
				areaEdit.replaceText("Select a file to edit");
				return;
			}

			EditedFile edit = it.getValue().open();
			if(edit.getEditorContents() == null) {
				if(p.getFileName().toString().endsWith(".class")) {
					try {
						ClassFile cf = new ClassFile(new ByteArrayInputStream(edit.getOriginalContents()));
						edit.setEditorContents(ClassFileFormatter.formatClass(cf));
					}catch(Exception e) {
						e.printStackTrace();
						areaEdit.replaceText("Failed to load class: " + e.toString());
					}
				}else {
					edit.setEditorContents(new String(edit.getOriginalContents(), StandardCharsets.UTF_8));
				}
			}

			areaEdit.replaceText(edit.getEditorContents());
			areaEdit.moveTo(0);
			areaEdit.requestFollowCaret();
		});
	}

	@FXML
	void newFile(ActionEvent event) {

	}

	@FXML
	void open(ActionEvent event) {
		FileChooser ch = new FileChooser();
		ch.getExtensionFilters().add(new ExtensionFilter("Java archives and classes", "*.jar", "*.war", "*.zip", "*.class"));
		File f = ch.showOpenDialog(JAREditor.stage);
		if(f == null) return;
		Path jarFile = f.toPath();

		OpenedFile opened = JAREditor.openFile(jarFile);

		TreeItem<EditorItem> root = new TreeItem<>(opened.getRoot());
		root.setExpanded(true);
		treeFiles.setRoot(root);
		add(root);
	}

	@FXML
	void save(ActionEvent event) {
		TreeItem<EditorItem> it = treeFiles.getSelectionModel().getSelectedItem();
		if(it == null) return;

		EditorItem item = it.getValue();
		EditedFile edit = item.getFile();
		if(edit == null) return;

		try {
			String code = areaEdit.getText();
			if(edit.getItem().getPath().getFileName().toString().endsWith(".class")) {
				var p = ClassFileParser.parse(new ClassFile(new ByteArrayInputStream(edit.getOriginalContents())), code);
				if(p.isErr()) {
					Alert a = new Alert(AlertType.ERROR);
					System.out.println(code.substring(p.getErr().getIndex()));
					a.setContentText(p.getErr().toString());
					a.show();
					p.getErr().printStackTrace();
					return;
				}
				ClassFile cf = p.value();
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				cf.write(bOut);
				Files.write(item.getPath(), bOut.toByteArray());
			}else {
				Files.writeString(item.getPath(), code, StandardCharsets.UTF_8);
			}
			JAREditor.openedFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
			Alert a = new Alert(AlertType.ERROR);
			a.setContentText(e.toString());
			a.show();
		}
	}

	@FXML
	void saveAs(ActionEvent event) {

	}

	@FXML
	void saveArchive(ActionEvent event) {

	}

	@FXML
	void saveArchiveAs(ActionEvent event) {

	}

	@FXML
	void preferences(ActionEvent event) {

	}

	@FXML
	void quit(ActionEvent event) {

	}

	public void add(TreeItem<EditorItem> item) {
		for(EditorItem c : item.getValue().getChildren()) {
			TreeItem<EditorItem> child = new TreeItem<EditorItem>(c);
			item.getChildren().add(child);
			add(child);
		}
	}

}
