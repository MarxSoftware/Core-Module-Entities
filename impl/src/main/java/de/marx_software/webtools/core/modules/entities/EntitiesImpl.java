package de.marx_software.webtools.core.modules.entities;

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
import de.marx_software.webtools.api.entities.Entities;
import de.marx_software.webtools.api.entities.Serializer;
import de.marx_software.webtools.api.entities.Store;
import de.marx_software.webtools.core.modules.entities.store.DB;
import de.marx_software.webtools.core.modules.entities.store.MVStoreDB;

import java.io.File;

/**
 *
 * @author marx
 */
public class EntitiesImpl implements AutoCloseable, Entities {

	private DB db;
	private File path;

	public EntitiesImpl(final File path) {
		this.path = path;
		if (!this.path.exists()) {
			this.path.mkdirs();
		}
	}

	@Override
	public void close() throws Exception {
		if (db != null) {
			((MVStoreDB)db).close();
		}
	}

	public void open() {
		if (db == null) {
//			db = new H2DB(path);
			db = new MVStoreDB(path);
			((MVStoreDB)db).open();
		}
	}

	@Override
	public <T> StoreImpl<T> store(final Class<T> clazz) {
		if (!clazz.isAnnotationPresent(de.marx_software.webtools.api.entities.annotations.Entity.class)) {
			throw new IllegalArgumentException("Entity annotation not present!");
		}
		return new StoreImpl<>(db, clazz, new GsonSerializer<>(clazz));
	}

	@Override
	public <T> Store<T> store(final Class<T> clazz, final Serializer<T> serializer) {
		if (!clazz.isAnnotationPresent(de.marx_software.webtools.api.entities.annotations.Entity.class)) {
			throw new IllegalArgumentException("Entity annotation not present!");
		}
		return new StoreImpl<>(db, clazz, serializer);
	}

}
