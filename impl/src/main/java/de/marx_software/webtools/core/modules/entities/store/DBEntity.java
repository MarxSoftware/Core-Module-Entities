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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author marx
 */
public class DBEntity extends Identifiable implements Serializable {
	
	private boolean update = false;
	
	private String name;
	private final String type;
	
	private final String version;
	
	private String content = "";
	
	private final Map<String, DBAttribute> attributes = new HashMap<>();
	
	public DBEntity (final String type, final String version) {
		this.type = type;
		this.version = version;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	
	
	public String getVersion () {
		return version;
	}
	
	public String getContent() {
		return content;
	}

	public DBEntity setContent(String content) {
		this.content = content;
		return this;
	}
	
	

	public Map<String, DBAttribute> getAttributes () {
		return attributes;
	}
	public void addAttribute (final DBAttribute attribute) {
		attributes.put(attribute.name(), attribute);
	}
	
	public String getName() {
		return name;
	}

	public DBEntity setName(String name) {
		this.name = name;
		return this;
	}

	public String getType() {
		return type;
	}
	
	
}
