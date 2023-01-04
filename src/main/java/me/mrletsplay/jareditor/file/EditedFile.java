package me.mrletsplay.jareditor.file;

public class EditedFile {

	private EditorItem item;
	private byte[] originalContents;
	private String editorContents;

	public EditedFile(EditorItem item, byte[] originalContents) {
		this.item = item;
		this.originalContents = originalContents;
	}

	public EditorItem getItem() {
		return item;
	}

	public byte[] getOriginalContents() {
		return originalContents;
	}

	public void setEditorContents(String editorContents) {
		this.editorContents = editorContents;
	}

	public String getEditorContents() {
		return editorContents;
	}

}
