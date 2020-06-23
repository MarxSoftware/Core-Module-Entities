package com.thorstenmarx.webtools.core.modules.entities.store;

/*-
 * #%L
 * webtools-entities
 * %%
 * Copyright (C) 2016 - 2018 Thorsten Marx
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import com.thorstenmarx.webtools.api.entities.Result;
import com.thorstenmarx.webtools.core.modules.entities.DefaultResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marx
 */
@Deprecated(since = "1.6.0")
public class H2DB implements DB<BooleanQuery> {

	private static final Logger LOGGER = LoggerFactory.getLogger(H2DB.class);

	private final File path;

	private JdbcConnectionPool pool;
	private NRTCachingDirectory nrt_index;
	private IndexWriter writer;
	private Directory directory;
	private SearcherManager nrt_manager;

	public H2DB(final File path) {
		this.path = path;
	}

	public void open() {
		try {
			Class.forName("org.h2.Driver");
			File dbFile = new File(path, "store.db");
			pool = JdbcConnectionPool.create("jdbc:h2:" + dbFile.getAbsolutePath(), "sa", "sa");
			init();

			directory = FSDirectory.open(Paths.get(new File(path, "index").toURI()));

			Analyzer analyzer = new KeywordAnalyzer();
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
			indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			indexWriterConfig.setCommitOnClose(true);
			nrt_index = new NRTCachingDirectory(directory, 5.0, 60.0);
			writer = new IndexWriter(nrt_index, indexWriterConfig);

			final SearcherFactory sf = new SearcherFactory();
			nrt_manager = new SearcherManager(writer, true, true, sf);

		} catch (ClassNotFoundException ex) {
			LOGGER.error("", ex);
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(H2DB.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void init() {
		try (Connection connection = pool.getConnection();
				Statement st = connection.createStatement()) {
			st.execute("CREATE TABLE IF NOT EXISTS entities (db_id VARCHAR(255) UNIQUE, db_name VARCHAR(255), db_type VARCHAR(255), db_content CLOB, db_version VARCHAR(256))");
			connection.commit();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void clear(final String type) {
		try (Connection connection = pool.getConnection();
				PreparedStatement st = connection.prepareStatement("DELETE FROM entities WHERE db_type = ?")) {

			st.setString(1, type);
			st.execute();

			writer.deleteDocuments(new Term("db_type", type));
			nrt_manager.maybeRefresh();
			connection.commit();
		} catch (SQLException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	@Override
	public void clearAll() {
		try (Connection connection = pool.getConnection();
				PreparedStatement st = connection.prepareStatement("DELETE FROM entities")) {

			st.execute();

			writer.deleteDocuments(new MatchAllDocsQuery());
			nrt_manager.maybeRefresh();
			connection.commit();
		} catch (SQLException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Result<DBEntity> list(final String type, final int offset, final int limit) {
		try (Connection connection = pool.getConnection()) {

			String statement = "SELECT * FROM entities WHERE db_type = ? LIMIT ? OFFSET ?";

			int count = count(type);
			if (count == 0) {
				return DefaultResult.EMPTY;
			}
			Result<DBEntity> result = new DefaultResult<>(count, offset, limit);

			try (PreparedStatement ps = connection.prepareStatement(statement)) {
				ps.setString(1, type);
				ps.setInt(2, limit);
				ps.setInt(3, offset);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						DBEntity entity = createEntity(rs);
						result.add(entity);
					}
				}

			}
			return result;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public List<DBEntity> query(final BooleanQuery luceneQuery) throws IOException {
		IndexSearcher searcher = nrt_manager.acquire();
		try {
			List<DBEntity> result = new ArrayList<>();

			TopDocs topDocs = searcher.search(luceneQuery, Integer.MAX_VALUE);

			for (final ScoreDoc doc : topDocs.scoreDocs) {
				final String id = searcher.doc(doc.doc).get("db_id");
				DBEntity entity = get(id, null);
				if (entity != null) {
					entity.id(id);
					result.add(entity);
				}
			}

			return result;
		} finally {
			nrt_manager.release(searcher);
		}
	}

	@Override
	public DBEntity get(final String id, final String type) {
		try (Connection connection = pool.getConnection()) {

			String statement = "SELECT * FROM entities WHERE db_id = ?";

			try (PreparedStatement ps = connection.prepareStatement(statement)) {
				ps.setString(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						DBEntity entity = createEntity(rs);
						return entity;
					}
				}

			}
			return null;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	private DBEntity createEntity(final ResultSet rs) throws SQLException {
		final String name = rs.getString("db_name");
		final String type = rs.getString("db_type");
		final String id = rs.getString("db_id");
		final String version = rs.getString("db_version");
		final String content = clobToString(rs.getClob("db_content"));
		final DBEntity entity = new DBEntity(type, version);
		entity.id(id);
		entity.name(name);
		entity.content(content);
		return entity;
	}

	@Override
	public int count(final String type) {
		try (Connection connection = pool.getConnection()) {

			String statement = "SELECT count(db_id) as count FROM entities WHERE db_type = ?";

			try (PreparedStatement ps = connection.prepareStatement(statement)) {
				ps.setString(1, type);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt("count");
					}
				}
			}
			return 0;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public boolean batch(final List<DBEntity> entities) {
		try (Connection connection = pool.getConnection()) {

			String insertStatement = "INSERT INTO entities (db_id, db_name, db_type, db_content, db_version) VALUES(?, ?, ?, ?, ?)";
			String updateStatement = "UPDATE entities SET db_name = ?, db_content=?, db_version=? WHERE db_id=?";
			try (PreparedStatement insert = connection.prepareStatement(insertStatement);
					PreparedStatement update = connection.prepareStatement(updateStatement)) {

				for (final DBEntity entity : entities) {
					if (entity.isUpdate()) {
						update.setString(1, entity.name());
						update.setClob(2, new StringReader(entity.content()));
						update.setString(3, entity.version());
						update.setString(4, entity.id());

						update.execute();
					} else {
						insert.setString(1, entity.id());
						insert.setString(2, entity.name());
						insert.setString(3, entity.type());
						insert.setClob(4, new StringReader(entity.content()));
						insert.setString(5, entity.version());

						insert.execute();
					}

					Document document = new Document();
					document.add(new StringField("db_id", entity.id(), Field.Store.YES));
					document.add(new StringField("db_type", entity.type(), Field.Store.YES));

					addAttributes(entity, document);

					writer.updateDocument(new Term("db_id", entity.id()), document);
				};

				writer.flush();
				writer.commit();
				nrt_manager.maybeRefresh();
				connection.commit();

				return true;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public boolean add(final DBEntity entity) {

		try (Connection connection = pool.getConnection()) {

			String statement;
			if (entity.isUpdate()) {
				statement = "UPDATE entities SET db_name = ?, db_content=?, db_version=? WHERE db_id=?";
			} else {
				statement = "INSERT INTO entities (db_id, db_name, db_type, db_content, db_version) VALUES(?, ?, ?, ?, ?)";
			}

			try (PreparedStatement ps = connection.prepareStatement(statement)) {
				if (entity.isUpdate()) {
					ps.setString(1, entity.name());
					ps.setClob(2, new StringReader(entity.content()));
					ps.setString(3, entity.version());
					ps.setString(4, entity.id());
				} else {
					ps.setString(1, entity.id());
					ps.setString(2, entity.name());
					ps.setString(3, entity.type());
					ps.setClob(4, new StringReader(entity.content()));
					ps.setString(5, entity.version());
				}
				ps.execute();

				Document document = new Document();
				document.add(new StringField("db_id", entity.id(), Field.Store.YES));
				document.add(new StringField("db_type", entity.type(), Field.Store.YES));

				addAttributes(entity, document);

				writer.updateDocument(new Term("db_id", entity.id()), document);
				writer.flush();
				writer.commit();
				nrt_manager.maybeRefresh();

				connection.commit();

				return true;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Delete an entity and all attributes;
	 *
	 * @param id
	 */
	@Override
	public void delete(final String id, final String type) {
		try (Connection connection = pool.getConnection();
				PreparedStatement st = connection.prepareStatement("DELETE FROM entities WHERE db_id = ?")) {

			st.setString(1, id);
			st.execute();

			writer.deleteDocuments(new Term("db_id", id));

			connection.commit();
		} catch (SQLException | IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void addAttributes(final DBEntity entity, final Document document) throws SQLException {
		for (DBAttribute attribute : entity.attributes().values()) {
			addAttributeToDocument(document, attribute);
		}
	}

	private void addAttributeToDocument(final Document document, final DBAttribute attribute) {
		if (null != attribute.type()) {
			switch (attribute.type()) {
				case BOOLEAN:
					document.add(new StringField(attribute.name(), String.valueOf(attribute.value()), Field.Store.NO));
					break;
				case DOUBLE:
					document.add(new DoublePoint(attribute.name(), (Double) attribute.value()));
					break;
				case FLOAT:
					document.add(new FloatPoint(attribute.name(), (Float) attribute.value()));
					break;
				case INTEGER:
					document.add(new IntPoint(attribute.name(), (Integer) attribute.value()));
					break;
				case LONG:
					document.add(new LongPoint(attribute.name(), (Long) attribute.value()));
					break;
				case STRING:
					document.add(new StringField(attribute.name(), String.valueOf(attribute.value()), Field.Store.NO));
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Close the entities instance and shutdown the database connection.
	 */
	public void close() {
		try {
			defrag();
			pool.dispose();
			writer.close();
			nrt_manager.close();
			directory.close();
		} catch (IOException ex) {
			LOGGER.error("", ex);
			throw new RuntimeException(ex);
		}
	}

	private void defrag() {
		try (
				Connection connection = pool.getConnection();
				Statement st = connection.createStatement()) {

			st.execute("shutdown defrag");

		} catch (SQLException ex) {
			throw new RuntimeException(ex);

		}
	}

	public static H2DB create(final File path) {
		return new H2DB(path);
	}

	private String clobToString(java.sql.Clob data) {
		final StringBuilder sb = new StringBuilder();

		try {
			final Reader reader = data.getCharacterStream();
			final BufferedReader br = new BufferedReader(reader);

			int b;
			while (-1 != (b = br.read())) {
				sb.append((char) b);
			}

			br.close();
			return sb.toString();
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
