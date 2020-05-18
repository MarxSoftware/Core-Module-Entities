package com.thorstenmarx.webtools.core.modules.entities.store;

import java.io.Serializable;

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
/**
 * SQL:
 * CREATE TABLE IF NOT EXISTS attributes (id VARCHAR(255) UNIQUE, name VARCHAR(255), value VARCHAR())
 *
 * @author marx
 */
public class DBAttribute extends Identifiable implements Serializable {
	public enum TYPE {
		INTEGER, FLOAT, DOUBLE, STRING, BOOLEAN, LONG;
		
		public static boolean is (final String type) {
			for (TYPE aType : values()) {
				if (aType.name().equals(type)) {
					return true;
				}
			}
					
			return false;
		}
	}
	private final String name;
	
	private final TYPE type;
	
	private Object value;
	
	DBAttribute (final String name, final TYPE type) {
		this.type = type;
		this.name = name;
	}

	public String name() {
		return name;
	}
	
	
	public TYPE type() {
		return type;
	}

	public Object value() {
		return value;
	}

	public DBAttribute value(Object value) {
		this.value = value;
		return this;
	}
	
	public <T> T value (Class<T> valueType) {
		if (valueType.isInstance(value)) {
			return valueType.cast(value);
		}
		return null;
	}
	
	public static DBAttribute create (final String name, final TYPE type) {
		return new DBAttribute(name, type);
	}
	
}
