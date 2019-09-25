package com.thorstenmarx.webtools.core.modules.entities;

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
import com.thorstenmarx.webtools.api.cluster.Cluster;
import com.thorstenmarx.webtools.api.entities.Entities;
import com.thorstenmarx.webtools.api.entities.Serializer;
import com.thorstenmarx.webtools.api.entities.Store;
import com.thorstenmarx.webtools.core.modules.entities.store.ClusterDB;
import com.thorstenmarx.webtools.core.modules.entities.store.H2DB;

import java.io.File;

/**
 *
 * @author marx
 */
public class EntitiesImpl implements AutoCloseable, Entities {

	private H2DB db;
	private ClusterDB cluster_db;
	private File path;
	private final Cluster cluster;

	public EntitiesImpl(final File path) {
		this(path, null);
	}

	public EntitiesImpl(final File path, final Cluster cluster) {
		this.path = new File(path, "entities");
		this.cluster = cluster;
		if (!path.exists()) {
			path.mkdirs();
		}
	}

	private boolean isCluster() {
		return cluster != null;
	}

	@Override
	public void close() throws Exception {
		if (db != null) {
			db.close();
		}
		if (cluster_db != null) {
			cluster_db.close();
		}
	}

	public void open() {
		if (db == null) {
			db = new H2DB(path);
			db.open();

			if (isCluster()) {
				cluster_db = new ClusterDB(db, cluster);
			}
		}
	}

	@Override
	public <T> StoreImpl<T> store(final Class<T> clazz) {
		if (!clazz.isAnnotationPresent(com.thorstenmarx.webtools.api.entities.annotations.Entity.class)) {
			throw new IllegalArgumentException("Entity annotation not present!");
		}
		if (isCluster()) {
			return new StoreImpl<>(cluster_db, clazz, new GsonSerializer<>(clazz));
		}
		return new StoreImpl<>(db, clazz, new GsonSerializer<>(clazz));
	}

	@Override
	public <T> Store<T> store(final Class<T> clazz, final Serializer<T> serializer) {
		if (!clazz.isAnnotationPresent(com.thorstenmarx.webtools.api.entities.annotations.Entity.class)) {
			throw new IllegalArgumentException("Entity annotation not present!");
		}
		if (isCluster()) {
			return new StoreImpl<>(cluster_db, clazz, serializer);
		}
		return new StoreImpl<>(db, clazz, serializer);
	}

}
