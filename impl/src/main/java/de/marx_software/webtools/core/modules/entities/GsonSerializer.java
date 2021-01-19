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

import com.google.gson.Gson;
import de.marx_software.webtools.api.entities.Serializer;
import de.marx_software.webtools.api.model.Pair;

/**
 *
 * @author marx
 */
public class GsonSerializer<T> implements Serializer<T>{

	private static final String VERSION = "gson";
	
	private final Gson gson;
	private final Class<T> typeClass;
	
	public GsonSerializer (final Class<T> typeClass) {
		this.gson = new Gson();
		this.typeClass = typeClass;
	}
	
	@Override
	public Pair<String, String> serialize(T object) {
		final String content = gson.toJson(object);
		Pair<String, String> result = new Pair<>(VERSION, content);
		return result;
	}

	@Override
	public Pair<String, T> deserialize(String version, String content) {
		T object = gson.fromJson(content, typeClass);
		
		return new Pair<>(version, object);
	}
	
}
