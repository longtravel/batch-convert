package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;

public class LegacyDocumentItemProcessor implements ItemProcessor<LegacyDocument, LegacyDocument> {

	private static final Logger log = LoggerFactory.getLogger(LegacyDocumentItemProcessor.class);

	@Override
	public LegacyDocument process(final LegacyDocument legacyDocument) throws Exception {

		log.info("Processing: " + legacyDocument.getDocLocator());

		return legacyDocument;
	}

}
