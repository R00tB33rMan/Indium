package link.infra.indium.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import link.infra.indium.Indium;
import link.infra.indium.renderer.render.TerrainRenderContext;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

/**
 * The main injection point into Sodium - here we stop Sodium from rendering FRAPI block models, and do it ourselves
 */
@Mixin(ChunkRenderRebuildTask.class)
public abstract class MixinChunkRenderRebuildTask extends ChunkRenderBuildTask {
	@Inject(method = "performBuild", at = @At("HEAD"), remap = false)
	public void beforePerformBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult> cir) {
		// Set up our rendering context
		TerrainRenderContext.get(buildContext).prepare(buildContext);
	}

	@Inject(method = "performBuild", at = @At("RETURN"), remap = false)
	public void afterPerformBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult> cir) {
		// Tear down our rendering context
		TerrainRenderContext.get(buildContext).release();
	}

	@Redirect(method = "performBuild", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;)Z", remap = false), remap = false)
	public boolean onRenderBlock(BlockRenderer blockRenderer, BlockRenderContext ctx, ChunkModelBuilder buffers, ChunkBuildContext buildContext, CancellationSource cancellationSource) {
		// We need to get the model with a bit more context than BlockRenderer has, so we do it here
		if (!Indium.ALWAYS_TESSELLATE_INDIUM && ((FabricBakedModel) ctx.model()).isVanillaAdapter()) {
			return blockRenderer.renderModel(ctx, buffers);
		} else {
			Vec3d modelOffset = ctx.state().getModelOffset(ctx.world(), ctx.pos());
			return TerrainRenderContext.get(buildContext).tessellateBlock(ctx.state(), ctx.pos(), ctx.origin(), ctx.model(), modelOffset);
		}
	}
}
