package com.example.batchprocessing;

public class LegacyDocument {

	private String docLocator;

	public LegacyDocument() {
	}

	public LegacyDocument (String docLocator){
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
		return "docLocator: " + docLocator;
	}
}
