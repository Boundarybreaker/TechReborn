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

package techreborn.init;

import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.Material;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import reborncore.common.fluid.*;
import techreborn.TechReborn;

public enum ModFluids {
	BERYLLIUM,
	CALCIUM,
	CALCIUM_CARBONATE,
	CARBON,
	CARBON_FIBER,
	CHLORITE,
	COMPRESSED_AIR,
	DEUTERIUM,
	DIESEL,
	ELECTROLYZED_WATER,
	GLYCERYL,
	HELIUM,
	HELIUM3,
	HELIUMPLASMA,
	HYDROGEN,
	LITHIUM,
	MERCURY,
	METHANE,
	NITRO_CARBON,
	NITRO_DIESEL,
	NITROCOAL_FUEL,
	NITROFUEL,
	NITROGEN,
	NITROGEN_DIOXIDE,
	OIL,
	POTASSIUM,
	SILICON,
	SODIUM,
	SODIUM_SULFIDE,
	SODIUM_PERSULFATE,
	SULFUR,
	SULFURIC_ACID,
	TRITIUM,
	WOLFRAMIUM;

	private RebornFluid stillFluid;
	private RebornFluid flowingFluid;

	private RebornFluidBlock block;
	private RebornBucketItem bucket;
	private final Identifier identifier;

	ModFluids() {
		this.identifier = new Identifier(TechReborn.MOD_ID, this.toString().toLowerCase());

		FluidSettings fluidSettings = FluidSettings.create();

		Identifier texture = new Identifier(TechReborn.MOD_ID, "block/fluids/" + this.toString().toLowerCase() + "_flowing");

		fluidSettings.setStillTexture(texture);
		fluidSettings.setFlowingTexture(texture);

		stillFluid = new RebornFluid(true, fluidSettings, () -> block, () -> bucket, () -> flowingFluid, () -> stillFluid) {
		};
		flowingFluid = new RebornFluid(false, fluidSettings, () -> block, () -> bucket, () -> flowingFluid, () -> stillFluid) {
		};

		block = new RebornFluidBlock(stillFluid, FabricBlockSettings.of(Material.WATER).noCollision().hardness(100.0F).dropsNothing().build());
		bucket = new RebornBucketItem(stillFluid, new Item.Settings().group(TechReborn.ITEMGROUP).recipeRemainder(Items.BUCKET).maxCount(1));
	}

	public void register() {
		RebornFluidManager.register(stillFluid, identifier);
		RebornFluidManager.register(flowingFluid, new Identifier(TechReborn.MOD_ID, identifier.getPath() + "_flowing"));

		Registry.register(Registry.BLOCK, identifier, block);
		Registry.register(Registry.ITEM, new Identifier(TechReborn.MOD_ID, identifier.getPath() + "_bucket"), bucket);
	}

	public RebornFluid getFluid() {
		return stillFluid;
	}

	public RebornFluidBlock getBlock() {
		return block;
	}

	public Identifier getIdentifier() {
		return identifier;
	}
}
