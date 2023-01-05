package me.mrletsplay.jareditor.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EditorItem {

	private OpenedFile openedFile;
	private Path path;
	private String displayName;
	private EditedFile file;

	private List<EditorItem> children;

	public EditorItem(OpenedFile openedFile, Path path, String displayName) {
		this.openedFile = openedFile;
		this.path = path;
		this.displayName = displayName;
		this.children = new ArrayList<>();
	}

	public OpenedFile getOpenedFile() {
		return openedFile;
	}

	public Path getPath() {
		return path;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void addChild(EditorItem child) {
		children.add(child);
	}

	public List<EditorItem> getChildren() {
		return children;
	}

	public EditedFile open() {
		if(file != null) return file;
		if(Files.isDirectory(path)) return null;
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(path);
			return file = new EditedFile(this, bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public EditedFile getFile() {
		return file;
	}

	public void update() {
		if(path != null) path = openedFile.getFileSystem().getPath(path.toString());
		for(EditorItem child : children) child.update();
	}

	@Override
	public String toString() {
		return displayName + (file != null ? "*" : "");
	}

}
