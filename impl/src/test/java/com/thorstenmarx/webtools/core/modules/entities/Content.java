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

import com.thorstenmarx.webtools.api.entities.annotations.Field;
import java.util.List;

/**
 *
 * @author marx
 */
@com.thorstenmarx.webtools.api.entities.annotations.Entity(type = "content")
public class Content {
	
	@Field(name = "id", key = true)
	private String id;
	
	@Field(name = "firstname")
	private String vorname;
	
	@Field(name = "age")
	private int age;
	
	@Field(name = "length")
	private float length;
	
	@Field(name = "married")
	private boolean married;

	@Field(name = "subcontent")
	private SubContent subContent;
	
	@Field(name = "subcontent2")
	private List<SubContent> subContent2;
	
	public Content () {
		
	}

	public List<SubContent> getSubContent2() {
		return subContent2;
	}

	public void setSubContent2(List<SubContent> subContent2) {
		this.subContent2 = subContent2;
	}

	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public SubContent getSubContent() {
		return subContent;
	}

	public void setSubContent(SubContent subContent) {
		this.subContent = subContent;
	}
	
	

	public boolean isMarried() {
		return married;
	}

	public Content setMarried(boolean married) {
		this.married = married;
		return this;
	}

	
	
	
	public float getLength() {
		return length;
	}

	public Content setLength(float length) {
		this.length = length;
		return this;
	}
	
	public int getAge() {
		return age;
	}

	public Content setAge(int age) {
		this.age = age;
		return this;
	}
	
	

	public String getVorname() {
		return vorname;
	}

	public Content setVorname(String vorname) {
		this.vorname = vorname;
		return this;
	}
}
