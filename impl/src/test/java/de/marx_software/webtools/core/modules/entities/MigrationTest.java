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
import java.io.File;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author marx
 */
public class MigrationTest {

	Entities entities;

	@BeforeMethod
	public void before() {
		File file = new File("./test_data");
		file.mkdirs();
		entities = new EntitiesImpl(file);
		((EntitiesImpl) entities).open();
	}

	@AfterMethod
	public void shutdown() throws Exception {
		((EntitiesImpl) entities).close();
	}

	@Test(enabled = false)
	public void testSaveAndGet() {
	}
}
