package com.thorstenmarx.webtools.core.modules.entities.store;

import com.google.gson.Gson;
import com.thorstenmarx.webtools.api.cluster.ClusterMessageAdapter;
import com.thorstenmarx.webtools.api.cluster.ClusterService;
import com.thorstenmarx.webtools.api.entities.Result;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marx
 */
public class ClusterDB<T> implements DB<T>, ClusterMessageAdapter<String>{

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDB.class);
	
	private final DB<T> wrapped;
	final ClusterService cluster;
	final Gson gson = new Gson();
	
	private static final String TYPE = "entities_type";

	public enum Command {
		ADD,
		DELETE,
		CLEAR,
		BATCH
	}

	public ClusterDB (final DB<T> wrapped, final ClusterService cluster) {
		this.wrapped = wrapped;
		this.cluster = cluster;
		
		cluster.registerAdpater(this);
	}
	
	public void close () {
	}
	
	@Override
	public boolean add(final DBEntity entity) {
		Payload payload = new Payload();
		payload.entity = entity;
		payload.command = Command.ADD;
		
		cluster.replicate(TYPE, gson.toJson(payload));
		wrapped.add(entity);
		return true;
	}

	@Override
	public boolean batch(List<DBEntity> entities) {
		Payload payload = new Payload();
		payload.entities = entities;
		payload.command = Command.BATCH;
		
		cluster.replicate(TYPE, gson.toJson(payload));
		wrapped.batch(entities);
				
		return true;
	}

	@Override
	public void clear(String type) {
		Payload payload = new Payload();
		payload.type = type;
		payload.command = Command.CLEAR;
		
		cluster.replicate(type, gson.toJson(payload));
		wrapped.clear(type);
	}

	@Override
	public int count(String type) {
		return wrapped.count(type);
	}

	@Override
	public void delete(String id, String type) {
		Payload payload = new Payload();
		payload.id = id;
		payload.type = type;
		payload.command = Command.DELETE;
		
		cluster.replicate(type, gson.toJson(payload));
		wrapped.delete(id, type);
	}
	@Override
	public void clearAll() {
		wrapped.clearAll();
	}
	

	@Override
	public DBEntity get(String id, String type) {
		return wrapped.get(id, type);
	}

	@Override
	public Result<DBEntity> list(String type, int offset, int limit) {
		return wrapped.list(type, offset, limit);
	}

	@Override
	public List<DBEntity> query(T luceneQuery) throws IOException {
		return wrapped.query(luceneQuery);
	}

	@Override
	public void apply(final String message) {
		
		Payload payload = gson.fromJson(message, Payload.class);
		if (Command.ADD.equals(payload.command)) {
			wrapped.add(payload.entity);
		} else if (Command.DELETE.equals(payload.command)) {
			wrapped.delete(payload.id, payload.type);
		} else if (Command.CLEAR.equals(payload.command)) {
			wrapped.clear(payload.type);
		} else if (Command.BATCH.equals(payload.command)) {
			wrapped.batch(payload.entities);
		}
	}
	
		@Override
	public Class<String> getValueClass() {
		return String.class;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void reset() {
		wrapped.clearAll();
	}
	
	public static class Payload {
		public DBEntity entity;
		public List<DBEntity> entities;
		public String id;
		public String type;
		public Command command;
	}
}
