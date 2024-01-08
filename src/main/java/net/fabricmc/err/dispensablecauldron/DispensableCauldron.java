package net.fabricmc.err.dispensablecauldron;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.err.dispensablecauldron.commands.Commands;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispensableCauldron implements ModInitializer
{
	public static final String MODID = "dispensable-cauldron";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	private static Boolean loaded = false;

	@Override
	public void onInitialize()
	{
		Config.initialize();
		Commands.initialize();

		// handle server reload
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener()
		{
			@Override
			public Identifier getFabricId()
			{
				return new Identifier(MODID, "load_config");
			}

			@Override
			public void reload(ResourceManager manager)
			{
				if(loaded) LOGGER.info("Load config");
				Config.load();
				loaded = true;
				LOGGER.info("Status: " + (Config.enable() ? "enabled" : "disabled"));
			}
		});
	}
}
