package techreborn.client.render;

import io.github.prospector.silk.fluid.FluidInstance;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ExtendedBlockView;
import net.minecraft.world.World;
import reborncore.common.fluid.container.GenericFluidContainer;
import reborncore.common.fluid.container.ItemFluidInfo;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class DynamicCellBakedModel implements BakedModel, FabricBakedModel {

	Sprite base;

	public DynamicCellBakedModel() {
		base = MinecraftClient.getInstance().getSpriteAtlas().getSprite(new Identifier("techreborn:item/cell_base"));
	}

	@Override
	public void emitBlockQuads(ExtendedBlockView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {

	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
		Fluid fluid = Fluids.EMPTY;
		if(stack.getItem() instanceof ItemFluidInfo){
			ItemFluidInfo fluidInfo = (ItemFluidInfo) stack.getItem();
			fluid = fluidInfo.getFluid(stack);

		}
		context.meshConsumer().accept(getMesh(fluid));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, Random random) {
		return Collections.emptyList();
	}

	//I have no idea what im doing, this works good enough for me, if you want to make it nicer a PR or tips would be appreciated, thanks.
	private Mesh getMesh(Fluid fluid){
		Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		MeshBuilder builder = renderer.meshBuilder();
		QuadEmitter emitter = builder.getEmitter();

		RenderMaterial mat = renderer.materialFinder().disableDiffuse(0, true).find();

		//TODO make the base texture 3d somehow
		emitter.square(Direction.SOUTH, 0, 0, 1, 1, 0)
			.material(mat)
			.spriteColor(0, -1, -1, -1, -1)
			.spriteBake(0, base, MutableQuadView.BAKE_LOCK_UV).emit();

		if(fluid != Fluids.EMPTY){
			FluidRenderHandler fluidRenderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
			if(fluidRenderHandler != null){
				int color = fluidRenderHandler.getFluidColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN, fluid.getDefaultState());
				//Does maths that works
				color = new Color((float)(color >> 16 & 255) / 255.0F, (float)(color >> 8 & 255) / 255.0F,(float)(color & 255) / 255.0F).getRGB();

				Sprite fluidSprite = fluidRenderHandler.getFluidSprites(MinecraftClient.getInstance().world, BlockPos.ORIGIN, fluid.getDefaultState())[0];
				emitter.square(Direction.SOUTH, 0.4F, 0.25F, 0.6F, 0.75F, -0.0001F)
					.material(mat)
					.spriteColor(0, color, color, color, color)
					.spriteBake(0, fluidSprite, MutableQuadView.BAKE_LOCK_UV).emit();
			}
		}

		return builder.build();
	}


	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean hasDepthInGui() {
		return true;
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public Sprite getSprite() {
		return base;
	}

	@Override
	public ModelTransformation getTransformation() {
		return ModelHelper.HANDHELD_ITEM_TRANSFORMS;
	}

	protected class ItemProxy extends ModelItemPropertyOverrideList {
		public ItemProxy() {
			super(null, null, null, Collections.emptyList());
		}

		@Override
		public BakedModel apply(BakedModel bakedModel, ItemStack itemStack, World world, LivingEntity livingEntity) {
			return DynamicCellBakedModel.this;
		}
	}

	@Override
	public ModelItemPropertyOverrideList getItemPropertyOverrides() {
		return new ItemProxy();
	}
}
