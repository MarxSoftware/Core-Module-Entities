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

import com.thorstenmarx.webtools.api.entities.Entities;
import com.thorstenmarx.webtools.api.entities.Result;
import com.thorstenmarx.webtools.api.entities.Store;
import com.thorstenmarx.webtools.api.entities.criteria.Restrictions;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author marx
 */
public class EntitiesTest {

	Entities entities;

	@BeforeMethod
	public void before() {
		File file = new File("./target/db-" + System.currentTimeMillis());
		file.mkdirs();
		entities = new EntitiesImpl(file);
		((EntitiesImpl) entities).open();
	}

	@AfterMethod
	public void shutdown() throws Exception {
		((EntitiesImpl) entities).close();
	}

	@Test
	public void testSaveAndGet() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setVorname("Thorsten");
		final String id = store.save(c);

		Content c2 = store.get(id);
		Assertions.assertThat(c2).isNotNull();
		Assertions.assertThat(c2.getVorname()).isEqualTo("Thorsten");
	}

	@Test(invocationCount = 10)
	public void testUpdate() {
		Store<Content> store = entities.store(Content.class);

		int size = store.size();

		Content c = new Content().setVorname("Holger");
		final String id = store.save(c);

		List<Content> result = store.criteria().add(Restrictions.EQ.eq("firstname", "Holger")).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);

		store.save(c);
		result = store.criteria().add(Restrictions.EQ.eq("firstname", "Holger")).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
	}

	@Test()
	public void testCriteria() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setVorname("Thorsten");
		store.save(c);

		List<Content> result = store.criteria().add(Restrictions.EQ.eq("firstname", "Thorsten")).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
	}

	@Test(description = "Criteria should not return a result")
	public void testCriteria_no_result() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setVorname("Hans");
		store.save(c);

		List<Content> result = store.criteria().add(Restrictions.EQ.eq("firstname", "Hans2")).query();
		Assertions.assertThat(result).isEmpty();
	}

	@Test(description = "Test integer restriction")
	public void testCriteria_integer_restriction() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setVorname("Hans").setAge(25);
		store.save(c);

		List<Content> result = store.criteria().add(Restrictions.LT.lt("age", 25)).query();
		Assertions.assertThat(result).isEmpty();

		result = store.criteria().add(Restrictions.LTE.lte("age", 25)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);

		result = store.criteria().add(Restrictions.GT.gt("age", 25)).query();
		Assertions.assertThat(result).isEmpty();

		result = store.criteria().add(Restrictions.GTE.gte("age", 25)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
		
		result = store.criteria().add(Restrictions.EQ.eq("age", 25)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
	}

	@Test(description = "Test Float restriction")
	public void testCriteria_float_restriction() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setVorname("Hans").setLength(25.0f);
		store.save(c);

		List<Content> result = store.criteria().add(Restrictions.LT.lt("length", 25.0f)).query();
		Assertions.assertThat(result).isEmpty();

		result = store.criteria().add(Restrictions.LTE.lte("length", 25.0f)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);

		result = store.criteria().add(Restrictions.GT.gt("length", 25.0f)).query();
		Assertions.assertThat(result).isEmpty();

		result = store.criteria().add(Restrictions.GTE.gte("length", 25.f)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);

		result = store.criteria().add(Restrictions.EQ.eq("length", 25.f)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
	}

	@Test(description = "Test boolean restriction")
	public void testCriteria_boolean_restriction() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setMarried(true);
		store.save(c);

		List<Content> result = store.criteria().add(Restrictions.EQ.eq("married", false)).query();
		Assertions.assertThat(result).isEmpty();

		result = store.criteria().add(Restrictions.EQ.eq("married", true)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);

		c = new Content().setMarried(false);
		store.save(c);

		result = store.criteria().add(Restrictions.EQ.eq("married", false)).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
	}

	@Test(description = "Test nested content")
	public void test_nested_content() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setMarried(true);
		SubContent subC = new SubContent();
		subC.setAge(25);
		subC.setName("Thorsten");
		c.setSubContent(subC);
		final String id = store.save(c);

		c = store.get(id);

		Assertions.assertThat(c).isNotNull();
		Assertions.assertThat(c.getSubContent()).isNotNull();

		subC = c.getSubContent();
		Assertions.assertThat(subC.getAge()).isEqualTo(25);
		Assertions.assertThat(subC.getName()).isEqualTo("Thorsten");

		List<Content> result = store.criteria().add(Restrictions.EQ.eq("subcontent.name", "Thorsten")).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);
	}

	@Test(description = "Test nested collection")
	public void test_nested_collection() {
		Store<Content> store = entities.store(Content.class);

		Content c = new Content().setMarried(true);
		SubContent subC = new SubContent();
		subC.setAge(25);
		subC.setName("Thorsten");
		List<SubContent> sub = new ArrayList<>();
		sub.add(subC);
		c.setSubContent2(sub);

		final String id = store.save(c);
		c = store.get(id);

		Assertions.assertThat(c).isNotNull();
		Assertions.assertThat(c.getSubContent2()).isNotNull().hasSize(1);
		Assertions.assertThat(c.getSubContent2().iterator().next().getAge()).isEqualTo(25);
		Assertions.assertThat(c.getSubContent2().iterator().next().getName()).isEqualTo("Thorsten");

		List<Content> result = store.criteria().add(Restrictions.EQ.eq("subcontent2.name", "Thorsten")).query();
		Assertions.assertThat(result).isNotEmpty().hasSize(1);

		result = store.criteria().add(Restrictions.EQ.eq("subcontent2.name", "Thorsten Marx")).query();
		Assertions.assertThat(result).isEmpty();
	}

	@Test(description = "Test list entities with two items")
	public void test_list() {
		Store<Content> store = entities.store(Content.class);
		store.save(new Content());
		store.save(new Content());

		Result<Content> result = store.list(0, 2);

		Assertions.assertThat(result).isNotEmpty().hasSize(2);
	}

	@Test(description = "Test list entities with no result")
	public void test_list_notresult() {
		Store<Content> store = entities.store(Content.class);
		store.save(new Content());
		store.save(new Content());

		Result<Content> result = store.list(0, 0);

		Assertions.assertThat(result).isEmpty();
	}

	@Test(description = "Test list entities with skipped first to elements")
	public void test_list_skip_two() {
		Store<Content> store = entities.store(Content.class);
		store.save(new Content());
		store.save(new Content());
		store.save(new Content());
		store.save(new Content());

		Result<Content> result = store.list(2, 2);

		Assertions.assertThat(result).isNotEmpty().hasSize(2);
	}

	@Test(description = "Test clear entity type")
	public void test_clear() {
		Store<Content> store = entities.store(Content.class);
		store.save(new Content());
		store.save(new Content());
		store.save(new Content());
		store.save(new Content());

		Assertions.assertThat(store.size()).isEqualTo(4);

		List<Content> result = store.criteria().query();
		Assertions.assertThat(result.size()).isEqualTo(4);

		store.clear();

		Assertions.assertThat(store.size()).isEqualTo(0);
		result = store.criteria().query();
		Assertions.assertThat(result.size()).isEqualTo(0);
	}

	@Test(description = "Test 1000 entities")
	public void test_1000_entities() {
		Store<Content> store = entities.store(Content.class);

		long before = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			final Content content = new Content();
			content.setAge(25).setVorname("Thorsten " + i);
			store.save(content);
		}
		long after = System.currentTimeMillis();
		System.out.println("took: " + (after - before) + "ms");
	}

	@Test(description = "Test 1000 entities in batch")
	public void test_1000_entities_batch() {
		Store<Content> store = entities.store(Content.class);

		long before = System.currentTimeMillis();
		List<Content> entities = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			final Content content = new Content();
			content.setAge(25).setVorname("Thorsten " + i);
			entities.add(content);
		}
		store.save(entities);
		long after = System.currentTimeMillis();
		System.out.println("took: " + (after - before) + "ms");
		System.out.println(store.size() + "size");
	}
}
