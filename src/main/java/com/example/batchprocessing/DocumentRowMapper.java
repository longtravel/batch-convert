package com.example.batchprocessing;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class DocumentRowMapper implements RowMapper<Document> {

  public static final String DOC_LOCATOR_COLUMN = "doclocator";

  public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
    Document document = new Document();
    document.setDocLocator(rs.getString(DOC_LOCATOR_COLUMN));
    document.setNewDoc(0);

    return document;
  }
}