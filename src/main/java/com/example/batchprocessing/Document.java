package com.example.batchprocessing;

public class Document {

	private String docLocator;
	private int newdoc;

	public Document() {
	}
	public Document(String docLocator){
		this.docLocator = docLocator;
	}
	public void setDocLocator(String docLocator) {
		this.docLocator = docLocator;
	}
	public String getDocLocator() {
		return docLocator;
	}

	public void setNewdoc(int newdoc) {this.newdoc = newdoc;}
	public int getNewdoc() { return newdoc;}

	@Override
	public String toString() {
		return "new docLocator: " + docLocator + ", newdoc: " + newdoc;
	}
}
