package com.thorstenmarx.webtools.core.modules.entities.criteria;

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

import com.thorstenmarx.webtools.api.entities.criteria.Restriction;
import com.thorstenmarx.webtools.api.entities.criteria.Criteria;
import com.thorstenmarx.webtools.api.entities.Serializer;
import com.thorstenmarx.webtools.api.entities.criteria.restrictions.BooleanRestriction;
import com.thorstenmarx.webtools.api.entities.criteria.restrictions.DoubleRestriction;
import com.thorstenmarx.webtools.api.entities.criteria.restrictions.FloatRestriction;
import com.thorstenmarx.webtools.api.entities.criteria.restrictions.IntegerRestriction;
import com.thorstenmarx.webtools.api.entities.criteria.restrictions.LongRestriction;
import com.thorstenmarx.webtools.api.entities.criteria.restrictions.StringRestriction;
import com.thorstenmarx.webtools.core.modules.entities.annotations.AnnotationHelper;
import com.thorstenmarx.webtools.core.modules.entities.store.DB;
import com.thorstenmarx.webtools.core.modules.entities.store.DBEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marx
 */
public class LuceneCriteria<T> implements Criteria<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LuceneCriteria.class);

	final String type;
//	final Class<T> typeClass;
	final DB db;

	private final Set<Restriction> restrictions;

//	private final AnnotationHelper<T> annotationHelper;
	
	private final Serializer<T> serializer;

	public LuceneCriteria(final String type, final Class<T> typeClass, final DB db, final AnnotationHelper<T> annotationHelper, final Serializer<T> serializer) {
		this.type = type;
//		this.typeClass = typeClass;
		this.db = db;
//		this.annotationHelper = annotationHelper;
		this.serializer = serializer;

		restrictions = new HashSet<>();
	}

	@Override
	public Criteria add(final Restriction restriction) {
		restrictions.add(restriction);
		return this;
	}

	@Override
	public List<T> query() {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

		queryBuilder.add(new TermQuery(new Term("db_type", type)), BooleanClause.Occur.MUST);

		addRestrictions(queryBuilder);

		BooleanQuery query = queryBuilder.build();

		try {
			List<DBEntity> entities = db.query(query);
			List<T> result = new ArrayList<>();
			for (final DBEntity entity : entities) {
//				result.add(annotationHelper.createInstance(entity));
				result.add(serializer.deserialize(entity.getVersion(), entity.getContent()).right);
			}
			return Collections.unmodifiableList(result);
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}

		return Collections.emptyList();
	}

	private void addRestrictions(final BooleanQuery.Builder queryBuilder) {
		restrictions.forEach((restriction) -> {
			if (restriction instanceof StringRestriction) {
				handleStringRestriction(queryBuilder, (StringRestriction) restriction);
			} else if (restriction instanceof IntegerRestriction) {
				handleIntegerRestriction(queryBuilder, (IntegerRestriction) restriction);
			} else if (restriction instanceof LongRestriction) {
				handleLongRestriction(queryBuilder, (LongRestriction) restriction);
			} else if (restriction instanceof FloatRestriction) {
				handleFloatRestriction(queryBuilder, (FloatRestriction) restriction);
			} else if (restriction instanceof DoubleRestriction) {
				handleDoubleRestriction(queryBuilder, (DoubleRestriction) restriction);
			} else if (restriction instanceof BooleanRestriction) {
				handleBooleanRestriction(queryBuilder, (BooleanRestriction) restriction);
			}
		});
	}

	private void handleStringRestriction(final BooleanQuery.Builder queryBuilder, final StringRestriction restriction) {
		queryBuilder.add(new TermQuery(new Term(restriction.name, restriction.value)), BooleanClause.Occur.MUST);
	}
	
	private void handleBooleanRestriction(final BooleanQuery.Builder queryBuilder, final BooleanRestriction restriction) {
		queryBuilder.add(new TermQuery(new Term(restriction.name, String.valueOf(restriction.value))), BooleanClause.Occur.MUST);
	}

	private void handleIntegerRestriction(final BooleanQuery.Builder queryBuilder, final IntegerRestriction restriction) {
		switch (restriction.mode) {
			case EQ:
				queryBuilder.add(IntPoint.newExactQuery(restriction.name, restriction.value), BooleanClause.Occur.MUST);
				break;
			case GT:
				queryBuilder.add(IntPoint.newRangeQuery(restriction.name, Math.addExact(restriction.value, 1), Integer.MAX_VALUE), BooleanClause.Occur.MUST);
				break;
			case GTE:
				queryBuilder.add(IntPoint.newRangeQuery(restriction.name, restriction.value, Integer.MAX_VALUE), BooleanClause.Occur.MUST);
				break;
			case LT:
				queryBuilder.add(IntPoint.newRangeQuery(restriction.name, Integer.MIN_VALUE, Math.addExact(restriction.value, -1)), BooleanClause.Occur.MUST);
				break;
			case LTE:
				queryBuilder.add(IntPoint.newRangeQuery(restriction.name, Integer.MIN_VALUE, restriction.value), BooleanClause.Occur.MUST);
				break;
		}
	}

	private void handleLongRestriction(final BooleanQuery.Builder queryBuilder, final LongRestriction restriction) {
		switch (restriction.mode) {
			case EQ:
				queryBuilder.add(LongPoint.newExactQuery(restriction.name, restriction.value), BooleanClause.Occur.MUST);
				break;
			case GT:
				queryBuilder.add(LongPoint.newRangeQuery(restriction.name, Math.addExact(restriction.value, 1), Long.MAX_VALUE), BooleanClause.Occur.MUST);
				break;
			case GTE:
				queryBuilder.add(LongPoint.newRangeQuery(restriction.name, restriction.value, Long.MAX_VALUE), BooleanClause.Occur.MUST);
				break;
			case LT:
				queryBuilder.add(LongPoint.newRangeQuery(restriction.name, Long.MIN_VALUE, Math.addExact(restriction.value, -1)), BooleanClause.Occur.MUST);
				break;
			case LTE:
				queryBuilder.add(LongPoint.newRangeQuery(restriction.name, Long.MIN_VALUE, restriction.value), BooleanClause.Occur.MUST);
				break;
		}
	}

	private void handleDoubleRestriction(final BooleanQuery.Builder queryBuilder, final DoubleRestriction restriction) {
		switch (restriction.mode) {
			case EQ:
				queryBuilder.add(DoublePoint.newExactQuery(restriction.name, restriction.value), BooleanClause.Occur.MUST);
				break;
			case GT:
				queryBuilder.add(DoublePoint.newRangeQuery(restriction.name, Double.sum(restriction.value, 1), Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST);
				break;
			case GTE:
				queryBuilder.add(DoublePoint.newRangeQuery(restriction.name, restriction.value, Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST);
				break;
			case LT:
				queryBuilder.add(DoublePoint.newRangeQuery(restriction.name, Double.NEGATIVE_INFINITY, Double.sum(restriction.value, 1)), BooleanClause.Occur.MUST);
				break;
			case LTE:
				queryBuilder.add(DoublePoint.newRangeQuery(restriction.name, Double.NEGATIVE_INFINITY, restriction.value), BooleanClause.Occur.MUST);
				break;
		}
	}

	private void handleFloatRestriction(final BooleanQuery.Builder queryBuilder, final FloatRestriction restriction) {
		switch (restriction.mode) {
			case EQ:
				queryBuilder.add(FloatPoint.newExactQuery(restriction.name, restriction.value), BooleanClause.Occur.MUST);
				break;
			case GT:
				queryBuilder.add(FloatPoint.newRangeQuery(restriction.name, Float.sum(restriction.value, 1f), Float.POSITIVE_INFINITY), BooleanClause.Occur.MUST);
				break;
			case GTE:
				queryBuilder.add(FloatPoint.newRangeQuery(restriction.name, restriction.value, Float.POSITIVE_INFINITY), BooleanClause.Occur.MUST);
				break;
			case LT:
				queryBuilder.add(FloatPoint.newRangeQuery(restriction.name, Float.NEGATIVE_INFINITY, (restriction.value  - 1f)), BooleanClause.Occur.MUST);
				break;
			case LTE:
				queryBuilder.add(FloatPoint.newRangeQuery(restriction.name, Float.NEGATIVE_INFINITY, restriction.value), BooleanClause.Occur.MUST);
				break;
		}
	}
}
