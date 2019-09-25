package com.thorstenmarx.webtools.core.modules.entities.store;

import com.google.gson.Gson;
import com.thorstenmarx.webtools.api.cluster.Cluster;
import com.thorstenmarx.webtools.api.cluster.Message;
import com.thorstenmarx.webtools.api.cluster.services.MessageService;
import com.thorstenmarx.webtools.api.entities.Result;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marx
 */
public class ClusterDB<T> implements DB<T>, MessageService.MessageListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDB.class);
	
	private final DB<T> wrapped;
	final Cluster cluster;
	final Gson gson = new Gson();
	
	private static final String ENTITIES_ADD = "entities_add";
	private static final String ENTITIES_DELETE = "entities_delete";
	private static final String ENTITIES_CLEAR = "entities_clear";
	private static final String ENTITIES_BATCH = "entities_batch";

	public ClusterDB (final DB<T> wrapped, final Cluster cluster) {
		this.wrapped = wrapped;
		this.cluster = cluster;
		
		cluster.getMessageService().registerMessageListener(this);
	}
	
	public void close () {
		cluster.getMessageService().unregisterMessageListener(this);
	}
	
	@Override
	public boolean add(final DBEntity entity) {
		PayloadAdd payload = new PayloadAdd();
		payload.entity = entity;
		
		Message message = new Message();
		message.setType(ENTITIES_ADD);
		message.setPayload(gson.toJson(payload));
		try {	
			cluster.getMessageService().publish(message);
			return true;
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
		return false;
	}

	@Override
	public boolean batch(List<DBEntity> entities) {
		PayloadBatch payload = new PayloadBatch();
		payload.entities = entities;
		
		Message message = new Message();
		message.setType(ENTITIES_BATCH);
		message.setPayload(gson.toJson(payload));
		try {	
			cluster.getMessageService().publish(message);
			return true;
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
		return false;
	}

	@Override
	public void clear(String type) {
		PayloadClear payload = new PayloadClear();
		payload.type = type;
		
		Message message = new Message();
		message.setType(ENTITIES_CLEAR);
		message.setPayload(gson.toJson(payload));
		try {	
			cluster.getMessageService().publish(message);
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}

	@Override
	public int count(String type) {
		return wrapped.count(type);
	}

	@Override
	public void delete(String id) {
		PayloadDelete payload = new PayloadDelete();
		payload.id = id;
		
		Message message = new Message();
		message.setType(ENTITIES_DELETE);
		message.setPayload(gson.toJson(payload));
		try {	
			cluster.getMessageService().publish(message);
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}

	@Override
	public DBEntity get(String id) {
		return wrapped.get(id);
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
	public void handle(Message message) {
		if (ENTITIES_ADD.equals(message.getType())) {
			PayloadAdd payloadadd = gson.fromJson(message.getPayload(), PayloadAdd.class);
			wrapped.add(payloadadd.entity);
		} else if (ENTITIES_DELETE.equals(message.getType())) {
			PayloadDelete payload = gson.fromJson(message.getPayload(), PayloadDelete.class);
			wrapped.delete(payload.id);
		} else if (ENTITIES_CLEAR.equals(message.getType())) {
			PayloadClear payload = gson.fromJson(message.getPayload(), PayloadClear.class);
			wrapped.clear(payload.type);
		} else if (ENTITIES_BATCH.equals(message.getType())) {
			PayloadBatch payload = gson.fromJson(message.getPayload(), PayloadBatch.class);
			wrapped.batch(payload.entities);
		}
	}
	
	public static class PayloadAdd {
		public DBEntity entity;
	}
	public static class PayloadBatch {
		public List<DBEntity> entities;
	}
	public static class PayloadDelete {
		public String id;
	}
	public static class PayloadClear {
		public String type;
	}
}
