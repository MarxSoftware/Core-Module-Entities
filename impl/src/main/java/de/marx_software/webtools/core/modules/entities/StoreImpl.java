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

import de.marx_software.webtools.api.entities.Store;
import de.marx_software.webtools.api.entities.Result;
import de.marx_software.webtools.api.entities.Serializer;
import de.marx_software.webtools.core.modules.entities.annotations.AnnotationHelper;
import de.marx_software.webtools.api.entities.annotations.Entity;
import de.marx_software.webtools.api.entities.criteria.Criteria;
import de.marx_software.webtools.api.model.Pair;
import de.marx_software.webtools.core.modules.entities.criteria.LuceneCriteria;
import de.marx_software.webtools.core.modules.entities.store.DB;
import de.marx_software.webtools.core.modules.entities.store.DBAttribute;
import de.marx_software.webtools.core.modules.entities.store.DBEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author marx
 * @param <T>
 */
public class StoreImpl<T> implements Store<T> {

	final DB db;
	final Class<T> typeClass;
	final String type;

	final AnnotationHelper<T> annotationHelper;

	final Serializer<T> serializer;

	protected StoreImpl(final DB db, final Class<T> typeClass, final Serializer<T> serializer) {
		this.db = db;
		this.typeClass = typeClass;
		this.serializer = serializer;
		type = typeClass.getAnnotation(Entity.class).type();

		annotationHelper = new AnnotationHelper<>(typeClass);
	}

	@Override
	public Criteria criteria() {
		return new LuceneCriteria(type, typeClass, db, annotationHelper, serializer);
	}

	@Override
	public int size() {
		return db.count(type);
	}

	@Override
	public void clear() {
		db.clear(type);
	}

	@Override
	public void delete(final T entity) {
		db.delete(annotationHelper.getId(entity), type);
	}

	@Override
	public T get(final String id) {
		DBEntity entity = db.get(id, type);
		if (entity == null) {
			return null;
		}
		final T instance = fromJSON(entity);
		return instance;
	}

	private T fromJSON(final DBEntity entity) {
		final T right = serializer.deserialize(entity.getVersion(), entity.getContent()).right;
		annotationHelper.setId(right, entity.getId());
		return right;
	}

	@Override
	public Result<T> list(final int offset, final int limit) {
		Result<DBEntity> entities = db.list(type, offset, limit);
		Result<T> resultList = new DefaultResult<>(entities.totalSize(), entities.offset(), entities.limit());

		entities.stream().map(this::fromJSON).forEach(resultList::add);

		return resultList;
	}

	@Override
	public List<String> save(final List<T> entities) {
		if (entities == null || entities.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		if (!typeClass.isAssignableFrom(entities.get(0).getClass()) || !entities.get(0).getClass().isAnnotationPresent(Entity.class)) {
			throw new IllegalArgumentException("Entity annotation not present");
		}
		try {
			List<DBEntity> dbEntities = new ArrayList<>();
			for (final T entity : entities) {

				String id = annotationHelper.getId(entity);
				boolean update = false;
				if (id == null || id.equals("")) {
					id = UUID.randomUUID().toString();
					annotationHelper.setId(entity, id);
				} else {
					update = true;
				}

				Pair<String, String> content = serializer.serialize(entity);
				DBEntity storeEntity = new DBEntity(type, content.left);
				storeEntity.setUpdate(update);
				storeEntity.setId(id);
				storeEntity.setContent(content.right);

				
				for (final Field field : entity.getClass().getDeclaredFields()) {
					if (field.isAnnotationPresent(de.marx_software.webtools.api.entities.annotations.Field.class)) {
						de.marx_software.webtools.api.entities.annotations.Field annotation = field.getAnnotation(de.marx_software.webtools.api.entities.annotations.Field.class);


						boolean accessible = field.canAccess(entity);
						field.setAccessible(true);

						try {
							DBAttribute attribute = annotationHelper.fieldToAttribute(field, annotation, entity);
							if (attribute != null) {
								storeEntity.addAttribute(attribute);
							} else if (field.get(entity) != null) {
								Object value = field.get(entity);
								if (value instanceof Collection) {
									addAttributes(annotationHelper.getFieldName(field, annotation), (Collection) value, storeEntity);
								} else {
									addAttributes(annotationHelper.getFieldName(field, annotation), value, storeEntity);
								}
							}
						} finally {
							field.setAccessible(accessible);
						}
					}
				}
				dbEntities.add(storeEntity);
			}

			db.batch(dbEntities);
			Function<? super DBEntity, ? extends String> fnctn = (DBEntity t) -> t.getId();

			return dbEntities.stream().map(fnctn).collect(Collectors.toList());
		} catch (IllegalAccessException illae) {
			throw new RuntimeException(illae);
		}
	}

	@Override
	public String save(final T entity) {
		if (!typeClass.isAssignableFrom(entity.getClass()) || !entity.getClass().isAnnotationPresent(Entity.class)) {
			throw new IllegalArgumentException("Entity annotation not present");
		}
		try {

			String id = annotationHelper.getId(entity);
			boolean update = false;
			if (id == null || id.equals("")) {
				id = UUID.randomUUID().toString();
				annotationHelper.setId(entity, id);
			} else {
				update = true;
			}

			Pair<String, String> content = serializer.serialize(entity);
			DBEntity storeEntity = new DBEntity(type, content.left);
			storeEntity.setUpdate(update);
			storeEntity.setId(id);
			storeEntity.setContent(content.right);

			for (final Field field : entity.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(de.marx_software.webtools.api.entities.annotations.Field.class)) {
					de.marx_software.webtools.api.entities.annotations.Field annotation = field.getAnnotation(de.marx_software.webtools.api.entities.annotations.Field.class);

					boolean accessible = field.canAccess(entity);
					field.setAccessible(true);

					try {
						DBAttribute attribute = annotationHelper.fieldToAttribute(field, annotation, entity);
						if (attribute != null) {
							storeEntity.addAttribute(attribute);
						} else if (field.get(entity) != null) {
							Object value = field.get(entity);
							if (value instanceof Collection) {
								addAttributes(annotationHelper.getFieldName(field, annotation), (Collection) value, storeEntity);
							} else {
								addAttributes(annotationHelper.getFieldName(field, annotation), value, storeEntity);
							}
						}
					} finally {
						field.setAccessible(accessible);
					}
				}
			}
			db.add(storeEntity);

			return storeEntity.getId();
		} catch (IllegalAccessException illae) {
			throw new RuntimeException(illae);
		}
	}

	private void addAttributes(final String namePrefix, final Collection entityCollection, final DBEntity storeEntity) throws IllegalArgumentException, IllegalAccessException {
		for (Object value : entityCollection) {
			addAttributes(namePrefix, value, storeEntity);
		}
	}

	private void addAttributes(final String namePrefix, final Object entity, final DBEntity storeEntity) throws IllegalArgumentException, IllegalAccessException {
		if (!entity.getClass().isAnnotationPresent(Entity.class)) {
			return;
		}
		for (final Field field : entity.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(de.marx_software.webtools.api.entities.annotations.Field.class)) {
				de.marx_software.webtools.api.entities.annotations.Field annotation = field.getAnnotation(de.marx_software.webtools.api.entities.annotations.Field.class);
				boolean accessible = field.isAccessible();
				field.setAccessible(true);

				try {
					field.setAccessible(true);

					DBAttribute attribute = annotationHelper.fieldToAttribute(field, annotation, entity, namePrefix);
					if (attribute != null) {
						storeEntity.addAttribute(attribute);
					} else {
						Object value = field.get(entity);
						if (value instanceof Collection) {
							addAttributes(annotationHelper.getFieldName(field, annotation), (Collection) value, storeEntity);
						} else {
							addAttributes(annotationHelper.getFieldName(field, annotation), value, storeEntity);
						}
					}
				} finally {
					field.setAccessible(accessible);
				}
			}
		}
	}

}
