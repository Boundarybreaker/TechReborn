/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package techreborn;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.block.DispenserBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reborncore.common.recipes.RecipeCrafter;
import reborncore.common.registration.RegistrationManager;
import reborncore.common.util.Torus;
import techreborn.blockentity.fusionReactor.FusionControlComputerBlockEntity;
import techreborn.client.GuiHandler;
import techreborn.events.ModRegistry;
import techreborn.init.*;
import techreborn.init.recipes.FluidGeneratorRecipes;
import techreborn.init.recipes.FusionReactorRecipes;
import techreborn.packets.ClientboundPackets;
import techreborn.packets.ServerboundPackets;
import techreborn.utils.BehaviorDispenseScrapbox;
import techreborn.world.WorldGenerator;

public class TechReborn implements ModInitializer {

	public static final String MOD_ID = "techreborn";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static TechReborn INSTANCE;

	public static ItemGroup ITEMGROUP = FabricItemGroupBuilder.build(
			new Identifier("techreborn", "item_group"),
			() -> new ItemStack(TRContent.NUKE));

	@Override
	public void onInitialize() {
		INSTANCE = this;
		@SuppressWarnings("unused")
		RegistrationManager registrationManager = new RegistrationManager("techreborn", getClass());

		// Done to force the class to load
		ModRecipes.GRINDER.getName();

		ClientboundPackets.init();
		ServerboundPackets.init();

		ModRegistry.setupShit();
		RecipeCrafter.soundHanlder = new ModSounds.SoundHandler();
		ModLoot.init();
		WorldGenerator.initBiomeFeatures();
		GuiHandler.register();
		FluidGeneratorRecipes.init();
		FusionReactorRecipes.init();


		// Scrapbox
		if (BehaviorDispenseScrapbox.dispenseScrapboxes) {
			DispenserBlock.registerBehavior(TRContent.SCRAP_BOX, new BehaviorDispenseScrapbox());
		}

		Torus.genSizeMap(FusionControlComputerBlockEntity.maxCoilSize);

		CommandRegistry.INSTANCE.register(false, dispatcher -> dispatcher.register(CommandManager.literal("recipe").executes(context -> {
			try {
				RecipeTemplate.generateFromInv(context.getSource().getPlayer());
			} catch (Exception e){
				e.printStackTrace();
			}
			return 0;
		})));

		LOGGER.info("TechReborn setup done!");

	}

}
