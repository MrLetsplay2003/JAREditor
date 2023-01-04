package me.mrletsplay.jareditor.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EditorItem {

	private Path path;
	private String displayName;
	private EditedFile file;

	public EditorItem(Path path, String displayName) {
		this.path = path;
		this.displayName = displayName;
	}

	public Path getPath() {
		return path;
	}

	public EditedFile open() {
		if(file != null) return file;
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

	@Override
	public String toString() {
		return displayName + (file != null ? "*" : "");
	}

}
