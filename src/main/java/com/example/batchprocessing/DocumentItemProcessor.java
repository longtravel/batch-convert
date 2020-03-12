package com.example.batchprocessing;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

public class DocumentItemProcessor implements ItemProcessor<Document, Document> {

	@Autowired
	private Environment env;

	private static final Logger log = LoggerFactory.getLogger(DocumentItemProcessor.class);

	@Override
	public Document process(final Document doc) throws Exception {

		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(env.getProperty("doc.url"));

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("locator", doc.getDocLocator()));
		httpPost.setEntity(new UrlEncodedFormEntity(params));
		try {
			CloseableHttpResponse response = client.execute(httpPost);
			log.info("REST call status: " + response.toString());
			if (response.getStatusLine().getStatusCode() == 200)
				doc.setNewDoc(200);
		}
		catch (Exception exception){
			log.info("Exception caught: " + exception.getMessage());
		}
		return doc;
	}
}
