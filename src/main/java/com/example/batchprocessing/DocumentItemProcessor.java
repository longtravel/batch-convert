package com.example.batchprocessing;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.util.Base64;

public class DocumentItemProcessor implements ItemProcessor<Document, Document> {

	@Autowired
	private Environment env;

	private static final Logger log = LoggerFactory.getLogger(DocumentItemProcessor.class);

	@Override
	public Document process(final Document doc) throws Exception {

		CloseableHttpClient client = HttpClients.createDefault();
		String localUrl = env.getProperty("doc.url") + encodeValue(doc.getDocLocator());

		byte[] bytes = (env.getProperty("dmsUser") + ":" + env.getProperty("dmsPass")).getBytes("UTF-8");
		String encoding = Base64.getEncoder().encodeToString(bytes);

		HttpPost httpPost = new HttpPost(localUrl);
		httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
		System.out.println("executing request " + httpPost.getRequestLine());
		try {
			CloseableHttpResponse response = client.execute(httpPost);
			StatusLine statusLine = response.getStatusLine();
			System.out.println(statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
			String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
			log.info ("Response body: " + responseBody);

			if (response.getStatusLine().getStatusCode() == 200)
				doc.setNewdoc(200);
		}
		catch (Exception exception){
			log.info("Exception caught: " + exception.getMessage());
		}
		return doc;
	}

	private static String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex.getCause());
		}
	}
}
