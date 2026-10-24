package com.sablednah.apocalypsetoolkit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sablednah.apocalypsetoolkit.turret.SentryBlock;
import com.sablednah.apocalypsetoolkit.turret.SentryBlockEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the sentry's head (the block's {@code head=true} model) turned to where it is aiming, smoothly. */
public class SentryRenderer implements BlockEntityRenderer<SentryBlockEntity, SentryRenderer.State> {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver models;

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState head = new BlockModelRenderState();
        float yaw;
    }

    public SentryRenderer(BlockEntityRendererProvider.Context ctx) {
        this.models = ctx.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SentryBlockEntity be, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, crumbling);
        models.update(state.head, be.getBlockState().setValue(SentryBlock.HEAD, true), CONTEXT);
        state.yaw = be.yaw(partial);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (state.head.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.translate(-0.5, 0, -0.5);
        state.head.submit(pose, out, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
