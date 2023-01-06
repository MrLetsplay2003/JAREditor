package me.mrletsplay.jareditor.file;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class OpenedFile {

	private Path filePath;
	private FileSystem fileSystem; // For archives
	private EditorItem root;

	public OpenedFile(Path filePath) {
		this.filePath = filePath;

		if(!filePath.getFileName().toString().endsWith(".class")) {
			try {
				fileSystem = FileSystems.newFileSystem(filePath, (ClassLoader) null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		root = new EditorItem(this, fileSystem == null ? filePath : null, filePath.getFileName().toString());
		if(fileSystem != null) {
			root = add(root, fileSystem.getPath("/"));
		}
	}

	private EditorItem add(EditorItem item, Path p) {
		try {
			List<Path> children = StreamSupport.stream(Files.newDirectoryStream(p).spliterator(), false)
				.sorted()
				.collect(Collectors.toList());

			if(children.size() == 1 && Files.isDirectory(children.get(0))) {
				Path c = children.get(0);
				item = new EditorItem(this, c, item.getDisplayName() + "/" + c.getFileName().toString());
				if(Files.isDirectory(c)) item = add(item, c);
				return item;
			}else {
				for(Path c : children) {
					EditorItem it = new EditorItem(this, c, c.getFileName().toString());
					if(Files.isDirectory(c)) it = add(it, c);
					item.addChild(it);
				}
				return item;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public EditorItem getRoot() {
		return root;
	}

	public Path getFilePath() {
		return filePath;
	}

	public FileSystem getFileSystem() {
		return fileSystem;
	}

	public boolean isArchive() {
		return fileSystem != null;
	}

	public void flush() {
		if(fileSystem != null) {
			try {
				fileSystem.close();
				fileSystem = FileSystems.newFileSystem(filePath, (ClassLoader) null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			root.update();
		}
	}

	public void close() {
		if(fileSystem != null) {
			try {
				fileSystem.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
