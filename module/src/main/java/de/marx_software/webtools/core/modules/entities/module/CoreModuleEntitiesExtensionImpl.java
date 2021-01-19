package de.marx_software.webtools.core.modules.entities.module;

import com.thorstenmarx.modules.api.annotation.Extension;
import de.marx_software.webtools.api.entities.Entities;
import de.marx_software.webtools.api.extensions.core.CoreEntitiesExtension;

/**
 *
 * @author marx
 */
@Extension(CoreEntitiesExtension.class)
public class CoreModuleEntitiesExtensionImpl extends CoreEntitiesExtension {

	@Override
	public String getName() {
		return "CoreModule Configuration";
	}

	@Override
	public Entities getEntities() {
		return CoreModuleEntitiesModuleLifeCycle.entities;
	}

	@Override
	public void init() {
	}
	
}
