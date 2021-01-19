package de.marx_software.webtools.core.modules.entities.store;

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
import de.marx_software.webtools.api.entities.Result;
import de.marx_software.webtools.core.modules.entities.DefaultResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marx
 */
public class MVStoreDB implements DB<BooleanQuery> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreDB.class);

	private final File path;

	private NRTCachingDirectory nrt_index;
	private IndexWriter writer;
	private Directory directory;
	private SearcherManager nrt_manager;

	MVStore store;

	public MVStoreDB(final File path) {
		this.path = path;
	}

	public void open() {
		try {
			File dataFolder = new File(path, "data");
			if (!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
			store = new MVStore.Builder()
					.fileName(new File(dataFolder, "storage.db").getAbsolutePath())
					.compress()
					.autoCommitDisabled()
					.open();

			directory = FSDirectory.open(Paths.get(new File(dataFolder, "index").toURI()));

			Analyzer analyzer = new KeywordAnalyzer();
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
			indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			indexWriterConfig.setCommitOnClose(true);
			nrt_index = new NRTCachingDirectory(directory, 5.0, 60.0);
			writer = new IndexWriter(nrt_index, indexWriterConfig);

			final SearcherFactory sf = new SearcherFactory();
			nrt_manager = new SearcherManager(writer, true, true, sf);
			
			
			// do migration stuff here
			File entitiesFolder = new File(path, "entities");
			File entitiesStoreFile = new File(entitiesFolder, "store.db.mv.db");
			File entitiesStoreLock = new File(entitiesFolder, ".locked");
			if (entitiesStoreFile.exists() && !entitiesStoreLock.exists()) {
				LOGGER.debug("found old database format -> start migration");
				H2DB h2DB = new H2DB(entitiesFolder);
				h2DB.open();
				BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
				queryBuilder.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
				List<DBEntity> entities = h2DB.query(queryBuilder.build());

				LOGGER.debug(String.format("found %d entities, start migration", entities.size()));
				if (batch(entities)) {
					LOGGER.debug("migration successfully");
					LOGGER.debug("move old database into backup folder");
					h2DB.close();
					entitiesStoreLock.createNewFile();
				} else {
					LOGGER.debug("migration not successfully");
				}
			}
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}

	@Override
	public void clear(final String type) {

		try {
			store.removeMap(createTypeMapName(type));
			writer.deleteDocuments(new Term("db_type", type));
			writer.commit();
			store.commit();
			nrt_manager.maybeRefresh();

		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}
	@Override
	public void clearAll() {

		try {
			store.getMapNames().forEach((map_name) -> {
				if (map_name.startsWith("entities_")) {
					store.removeMap(map_name);
				}
			});
			writer.deleteDocuments(new MatchAllDocsQuery());
			writer.commit();
			store.commit();
			nrt_manager.maybeRefresh();

		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}

	private static String createTypeMapName(final String type) {
		return "entities_" + type;
	}

	@Override
	public Result<DBEntity> list(final String type, final int offset, final int limit) {
		try {
			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			builder.add(new TermQuery(new Term("db_type", type)), BooleanClause.Occur.MUST);
			List<DBEntity> query = query(builder.build());

			DefaultResult<DBEntity> result = new DefaultResult<>(query.size(), 0, query.size());
			query.stream().skip(offset).limit(limit).forEach(result::add);
			return result;
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
		return new DefaultResult<>(0, 0, 0);
	}

	@Override
	public List<DBEntity> query(final BooleanQuery luceneQuery) throws IOException {
		IndexSearcher searcher = nrt_manager.acquire();
		try {
			List<DBEntity> result = new ArrayList<>();

			TopDocs topDocs = searcher.search(luceneQuery, Integer.MAX_VALUE);

			for (final ScoreDoc doc : topDocs.scoreDocs) {
				DBEntity entity = get(searcher.doc(doc.doc).get("db_id"), searcher.doc(doc.doc).get("db_type"));
				if (entity != null) {
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
		Map<String, DBEntity> map = store.openMap(createTypeMapName(type));

		if (map.containsKey(id)) {
			return map.get(id);
		}
		return null;
	}

	@Override
	public int count(final String type) {
		final String mapName = createTypeMapName(type);
		if (store.hasMap(mapName)) {
			return store.openMap(mapName).size();
		}
		return 0;
	}

	@Override
	public boolean batch(final List<DBEntity> entities) {

		try {
			for (DBEntity entity : entities) {

				Map<String, DBEntity> map = store.openMap(createTypeMapName(entity.getType()));

				map.put(entity.getId(), entity);

				Document document = new Document();
				document.add(new StringField("db_id", entity.getId(), Field.Store.YES));
				document.add(new StringField("db_type", entity.getType(), Field.Store.YES));

				addAttributes(entity, document);

				writer.updateDocument(new Term("db_id", entity.getId()), document);

			}
			store.commit();
			writer.flush();
			writer.commit();
			nrt_manager.maybeRefresh();

			return true;
		} catch (IOException ex) {
			LOGGER.error("", ex);
			store.rollback();
			try {
				writer.rollback();
			} catch (IOException ex1) {
				LOGGER.error("", ex1);
			}
		}

		return false;
	}

	@Override
	public boolean add(final DBEntity entity) {
		return batch(Arrays.asList(entity));
	}

	/**
	 * Delete an entity and all attributes;
	 *
	 * @param id
	 * @param type
	 */
	@Override
	public void delete(final String id, final String type) {
		try {

			Map<String, DBEntity> map = store.openMap(createTypeMapName(type));

			map.remove(id);
			writer.deleteDocuments(new Term("db_id", id));

			store.commit();
			writer.commit();
		} catch (IOException ex) {
			try {
				store.rollback();
				writer.rollback();
			} catch (IOException iOException) {
				LOGGER.error("", iOException);
			}
			throw new RuntimeException(ex);
		}
	}

	private void addAttributes(final DBEntity entity, final Document document) {
		for (DBAttribute attribute : entity.getAttributes().values()) {
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
			store.close();
			writer.close();
			nrt_manager.close();
			directory.close();
		} catch (IOException ex) {
			LOGGER.error("", ex);
			throw new RuntimeException(ex);
		}
	}

	private void defrag() {
		store.compactMoveChunks();
	}

	public static MVStoreDB create(final File path) {
		return new MVStoreDB(path);
	}
}
