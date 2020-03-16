package com.thorstenmarx.webtools.core.modules.entities.annotations;

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
import com.thorstenmarx.webtools.core.modules.entities.store.DBAttribute;
import com.thorstenmarx.webtools.core.modules.entities.store.DBEntity;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author marx
 */
public class AnnotationHelper<T> {

	final Class<T> typeClazz;

	public AnnotationHelper(final Class<T> typeClazz) {
		this.typeClazz = typeClazz;
	}

	public T createInstance(final DBEntity entity) {
		T instance = newInstance();

		for (final DBAttribute attribute : entity.attributes().values()) {
			final String name = attribute.name();
			java.lang.reflect.Field field = findField(name);
			if (field != null) {
				setField(instance, field, attribute.value());
			}
		}

		return instance;
	}

	public T newInstance() {
		for (Constructor constructor : typeClazz.getConstructors()) {
			if (constructor.getParameterCount() == 0) {
				try {
					return (T) constructor.newInstance();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					throw new IllegalArgumentException("could not create object with default constructor", ex);
				}
			}
		}
		throw new IllegalArgumentException("no default constructor available");
	}

	public void setId(final Object instance, final String id) {
		java.lang.reflect.Field idField = findIndex(instance);
		boolean access = idField.isAccessible();
		try {
			idField.setAccessible(true);
			idField.set(instance, id);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} finally {
			idField.setAccessible(access);
		}
	}
	
	public String getId(final Object instance) {
		java.lang.reflect.Field idField = findIndex(instance);
		boolean access = idField.isAccessible();
		try {
			idField.setAccessible(true);
			return (String) idField.get(instance);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} finally {
			idField.setAccessible(access);
		}
	}

	public java.lang.reflect.Field findIndex(final Object instance) {
		for (final java.lang.reflect.Field field : typeClazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(com.thorstenmarx.webtools.api.entities.annotations.Field.class)) {
				if (field.getType().isAssignableFrom(String.class) && field.getAnnotation(Field.class).key() == true) {
					return field;
				}
			}
		}
		throw new IllegalArgumentException("missing mandatory index field");
	}

	public void setField(final Object instance, final java.lang.reflect.Field field, final Object value) {
		boolean access = field.isAccessible();
		try {
			field.setAccessible(true);
			field.set(instance, value);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new RuntimeException("", ex);
		} finally {
			field.setAccessible(access);
		}
	}

	public java.lang.reflect.Field findField(final String name) {
		for (final java.lang.reflect.Field field : typeClazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(com.thorstenmarx.webtools.api.entities.annotations.Field.class)) {
				com.thorstenmarx.webtools.api.entities.annotations.Field annotation = field.getAnnotation(com.thorstenmarx.webtools.api.entities.annotations.Field.class);

				if (annotation.name().equals(name)) {
					return field;
				}
			}
		}
		return null;
	}

	
	
	public DBAttribute fieldToAttribute(final java.lang.reflect.Field field, final Field annotation, final Object entity, final String namePrefix) throws IllegalArgumentException, IllegalAccessException {
		boolean access = field.isAccessible();
		try {
			final String fieldName = getPrefixedFieldName(annotation, field, namePrefix);
			Object value = field.get(entity);
			DBAttribute attribute = null;
			if (value != null) {
				if (value instanceof Boolean) {
					attribute = DBAttribute.create(fieldName, DBAttribute.TYPE.BOOLEAN);
				} else if (value instanceof Integer) {
					attribute = DBAttribute.create(fieldName, DBAttribute.TYPE.INTEGER);
				} else if (value instanceof Long) {
					attribute = DBAttribute.create(fieldName, DBAttribute.TYPE.LONG);
				} else if (value instanceof Float) {
					attribute = DBAttribute.create(fieldName, DBAttribute.TYPE.FLOAT);
				} else if (value instanceof Double) {
					attribute = DBAttribute.create(fieldName, DBAttribute.TYPE.DOUBLE);
				} else if (value instanceof String) {
					attribute = DBAttribute.create(fieldName, DBAttribute.TYPE.STRING);
				}
				if (attribute != null) {
					attribute.value(value);
				}

				return attribute;
			}
		} finally {
			field.setAccessible(access);
		}
		return null;
	}

	private String getPrefixedFieldName(final Field annotation, final java.lang.reflect.Field field, final String namePrefix) {
		String fieldName = getFieldName(field, annotation);
		if (namePrefix != null) {
			fieldName = namePrefix + "." + annotation.name();
		}
		return fieldName;
	}
	
	public String getFieldName (final java.lang.reflect.Field field, final com.thorstenmarx.webtools.api.entities.annotations.Field annotation) {
		if (annotation.name() == null || "".equals(annotation.name())) {
			return field.getName();
		} else {
			return annotation.name();
		}
	}

	public DBAttribute fieldToAttribute(final java.lang.reflect.Field field, final Field annotation, final Object entity) throws IllegalArgumentException, IllegalAccessException {
		return fieldToAttribute(field, annotation, entity, null);
	}
}
