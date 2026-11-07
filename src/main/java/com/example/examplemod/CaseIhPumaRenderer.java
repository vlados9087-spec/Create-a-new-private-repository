
package com.example.examplemod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Optional;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class CaseIhPumaRenderer
        extends EntityRenderer<CaseIhPumaEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation MODEL_LOCATION =
            new ResourceLocation(
                    ExampleMod.MODID, "models/puma185.gltf");

    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation(
                    ExampleMod.MODID, "textures/gltf/white.png");

    private static final float MODEL_SCALE_X = 4.4F;
private static final float MODEL_SCALE_Y = 4.4F;
private static final float MODEL_SCALE_Z = 4.4F;

    private final PumaGltfModel model;private static boolean loggedRenderMode = false;

        public CaseIhPumaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = loadModel();
    }

    private PumaGltfModel loadModel() {
        Optional<Resource> found =
                Minecraft.getInstance()
                        .getResourceManager()
                        .getResource(MODEL_LOCATION);

        if (!found.isPresent()) {
            LOGGER.error("Puma GLTF model not found: {}", MODEL_LOCATION);
            return null;
        }

        try (java.io.InputStream stream = found.get().open()) {
            return PumaGltfModel.load(stream);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load Puma 185 model", e);
            return null;
        }
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                ExampleMod.CASE_IH_PUMA_ENTITY.get(),
                CaseIhPumaRenderer::new
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            CaseIhPumaEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public void render(
            CaseIhPumaEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {

        if (model != null) {
            poseStack.pushPose();

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(180.0F - entityYaw));

            poseStack.scale(
        MODEL_SCALE_X,
        MODEL_SCALE_Y,
        MODEL_SCALE_Z
);

            // Position the model near the ground and center it.
            poseStack.translate(0.0F, -0.09375F, 0.035204F);

            for (PumaGltfModel.Part part : model.getParts()) {
                renderPart(
                        part, poseStack, buffer, packedLight);
            }

            poseStack.popPose();
        }

        super.render(
                entity, entityYaw, partialTicks,
                poseStack, buffer, packedLight);
    }

    private void renderPart(
            PumaGltfModel.Part part,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {

        poseStack.pushPose();

        poseStack.translate(
                part.translation[0],
                part.translation[1],
                part.translation[2]
        );

        poseStack.mulPose(new Quaternionf(
                part.rotation[0],
                part.rotation[1],
                part.rotation[2],
                part.rotation[3]
        ));

        poseStack.scale(
                part.scale[0],
                part.scale[1],
                part.scale[2]
        );

        ResourceLocation texture = part.texturePath == null
                ? WHITE_TEXTURE
                : new ResourceLocation(
                        ExampleMod.MODID, part.texturePath);
RenderType renderType =
        "BLEND".equals(part.alphaMode)
                ? RenderType.entityTranslucent(texture)
                : (part.doubleSided
                    ? RenderType.entityCutoutNoCull(texture)
                    : RenderType.entityCutout(texture));

if (!loggedRenderMode) {
    LOGGER.info("Puma render type mode: {}", renderType.mode());
    loggedRenderMode = true;
}
        


        PoseStack.Pose pose = poseStack.last();

        int red = channel(part.color[0]);
        int green = channel(part.color[1]);
        int blue = channel(part.color[2]);
        int alpha = channel(part.color[3]);

         VertexConsumer consumer = buffer.getBuffer(renderType);

        for (int triangle = 0;
             triangle + 2 < part.indices.length;
             triangle += 3) {

            int[] quadIndices = {
                    part.indices[triangle],
                    part.indices[triangle + 1],
                    part.indices[triangle + 2],
                    part.indices[triangle + 2]
            };

            for (int index : quadIndices) {
                int p = index * 3;
                int t = index * 2;

                consumer.vertex(
                                pose.pose(),
                                part.positions[p],
                                part.positions[p + 1],
                                part.positions[p + 2]
                        )
                        .color(red, green, blue, alpha)
                        .uv(part.uv[t], part.uv[t + 1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(
                                pose.normal(),
                                part.normals[p],
                                part.normals[p + 1],
                                part.normals[p + 2]
                        )
                        .endVertex();
            }
        }

        poseStack.popPose();
    }

    private static int channel(float value) {
        return Math.max(
                0,
                Math.min(255, Math.round(value * 255.0F))
        );
    }
}
