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
import java.io.IOException;
import java.util.List;

/**
 *
 * @author marx
 */
public interface DB<T> {

	boolean add(final DBEntity entity);

	boolean batch(final List<DBEntity> entities);

	void clear(final String type);
	
	void clearAll ();

	int count(final String type);

	/**
	 * Delete an entity and all attributes;
	 *
	 * @param id
	 */
	void delete(final String id, final String type);

	DBEntity get(final String id, final String type);

	Result<DBEntity> list(final String type, final int offset, final int limit);

	List<DBEntity> query(final T luceneQuery) throws IOException;
	
}
