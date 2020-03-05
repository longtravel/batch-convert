package com.example.batchprocessing;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class LegacyDocumentRowMapper implements RowMapper<LegacyDocument> {

  public static final String DOC_LOCATOR_COLUMN = "DOC_HANDLE";

  public LegacyDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
    LegacyDocument legacyDocument = new LegacyDocument();

    legacyDocument.setDocLocator(rs.getString(DOC_LOCATOR_COLUMN));

    return legacyDocument;
  }
}