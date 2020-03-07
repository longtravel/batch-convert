package com.example.batchprocessing;

public class Document {

	private String docLocator;

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

	@Override
	public String toString() {
		return "new docLocator: " + docLocator;
	}
}
