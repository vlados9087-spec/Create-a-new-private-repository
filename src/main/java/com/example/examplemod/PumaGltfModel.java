
package com.example.examplemod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PumaGltfModel {

    private final List<Part> parts;

    private PumaGltfModel(List<Part> parts) {
        this.parts = Collections.unmodifiableList(parts);
    }

    public List<Part> getParts() {
        return parts;
    }

    public static PumaGltfModel load(InputStream input) throws IOException {
        JsonObject root;

        try (Reader reader = new InputStreamReader(
                input, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonArray buffers = root.getAsJsonArray("buffers");
        if (buffers == null || buffers.size() == 0) {
            throw new IOException("GLTF: нет буфера");
        }

        String uri = buffers.get(0)
                .getAsJsonObject().get("uri").getAsString();

        if (!uri.startsWith("data:") || !uri.contains(";base64,")) {
            throw new IOException("GLTF: неподдерживаемый URI буфера");
        }

        byte[] binary;
        try {
            binary = Base64.getDecoder().decode(
                    uri.substring(uri.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            throw new IOException("GLTF: ошибка Base64", e);
        }

        int expectedSize = buffers.get(0)
                .getAsJsonObject().get("byteLength").getAsInt();

        if (binary.length < expectedSize) {
            throw new IOException("GLTF: буфер повреждён");
        }

        JsonArray views = root.getAsJsonArray("bufferViews");
        JsonArray accessors = root.getAsJsonArray("accessors");
        JsonArray meshes = root.getAsJsonArray("meshes");
        JsonArray nodes = root.getAsJsonArray("nodes");
        JsonArray materials = root.getAsJsonArray("materials");
        JsonArray textures = root.getAsJsonArray("textures");
        JsonArray images = root.getAsJsonArray("images");

        List<Part> result = new ArrayList<>();

        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            JsonObject node = nodes.get(nodeIndex).getAsJsonObject();

            if (!node.has("mesh")) {
                continue;
            }

            int meshIndex = node.get("mesh").getAsInt();
            JsonArray primitives = meshes.get(meshIndex)
                    .getAsJsonObject().getAsJsonArray("primitives");

            float[] translation = vector(
                    node, "translation", new float[]{0, 0, 0});
            float[] rotation = vector(
                    node, "rotation", new float[]{0, 0, 0, 1});
            float[] scale = vector(
                    node, "scale", new float[]{1, 1, 1});

            for (int primitiveIndex = 0;
                 primitiveIndex < primitives.size();
                 primitiveIndex++) {

                JsonObject primitive =
                        primitives.get(primitiveIndex).getAsJsonObject();

                if (primitive.has("mode")
                        && primitive.get("mode").getAsInt() != 4) {
                    throw new IOException(
                            "GLTF: поддерживаются только треугольники");
                }

                JsonObject attributes =
                        primitive.getAsJsonObject("attributes");

                float[] positions = readFloats(
                        views, accessors, binary,
                        attributes.get("POSITION").getAsInt(), 3);

                float[] normals = readFloats(
                        views, accessors, binary,
                        attributes.get("NORMAL").getAsInt(), 3);

                float[] uv = readFloats(
                        views, accessors, binary,
                        attributes.get("TEXCOORD_0").getAsInt(), 2);

                int[] indices = readIndices(
                        views, accessors, binary,
                        primitive.get("indices").getAsInt());

                int materialIndex = primitive.has("material")
                        ? primitive.get("material").getAsInt() : -1;

                MaterialData material = readMaterial(
                        materialIndex, materials, textures, images);

                String name = node.has("name")
                        ? node.get("name").getAsString()
                        : "part_" + nodeIndex;

                result.add(new Part(
                        name, nodeIndex,
                        positions, normals, uv, indices,
                        translation.clone(), rotation.clone(),
                        scale.clone(),
                        material.texturePath,
                        material.color,
                        material.doubleSided,
                        material.alphaMode
                ));
            }
        }

        if (result.isEmpty()) {
            throw new IOException("GLTF: модель не содержит деталей");
        }

        return new PumaGltfModel(result);
    }

    private static float[] readFloats(
            JsonArray views,
            JsonArray accessors,
            byte[] binary,
            int accessorIndex,
            int components) throws IOException {

        JsonObject accessor =
                accessors.get(accessorIndex).getAsJsonObject();

        if (accessor.get("componentType").getAsInt() != 5126) {
            throw new IOException("GLTF: ожидался FLOAT");
        }

        String expectedType = components == 2 ? "VEC2" : "VEC3";
        if (!accessor.get("type").getAsString().equals(expectedType)) {
            throw new IOException("GLTF: неверный формат вершин");
        }

        int count = accessor.get("count").getAsInt();
        JsonObject view = views.get(
                accessor.get("bufferView").getAsInt()).getAsJsonObject();

        int viewOffset = number(view, "byteOffset", 0);
        int accessorOffset = number(accessor, "byteOffset", 0);
        int stride = number(view, "byteStride", components * 4);

        ByteBuffer data = ByteBuffer.wrap(binary)
                .order(ByteOrder.LITTLE_ENDIAN);

        float[] result = new float[count * components];
        int start = viewOffset + accessorOffset;

        for (int i = 0; i < count; i++) {
            for (int c = 0; c < components; c++) {
                result[i * components + c] =
                        data.getFloat(start + i * stride + c * 4);
            }
        }

        return result;
    }

    private static int[] readIndices(
            JsonArray views,
            JsonArray accessors,
            byte[] binary,
            int accessorIndex) throws IOException {

        JsonObject accessor =
                accessors.get(accessorIndex).getAsJsonObject();

        if (!accessor.get("type").getAsString().equals("SCALAR")) {
            throw new IOException("GLTF: индексы должны быть SCALAR");
        }

        int componentType =
                accessor.get("componentType").getAsInt();

        int componentSize;
        if (componentType == 5121) {
            componentSize = 1;
        } else if (componentType == 5123) {
            componentSize = 2;
        } else if (componentType == 5125) {
            componentSize = 4;
        } else {
            throw new IOException("GLTF: неизвестный тип индексов");
        }

        int count = accessor.get("count").getAsInt();
        JsonObject view = views.get(
                accessor.get("bufferView").getAsInt()).getAsJsonObject();

        int start = number(view, "byteOffset", 0)
                + number(accessor, "byteOffset", 0);

        int stride = number(view, "byteStride", componentSize);

        ByteBuffer data = ByteBuffer.wrap(binary)
                .order(ByteOrder.LITTLE_ENDIAN);

        int[] result = new int[count];

        for (int i = 0; i < count; i++) {
            int offset = start + i * stride;

            if (componentType == 5121) {
                result[i] = Byte.toUnsignedInt(data.get(offset));
            } else if (componentType == 5123) {
                result[i] = Short.toUnsignedInt(data.getShort(offset));
            } else {
                result[i] = data.getInt(offset);
            }
        }

        return result;
    }

    private static MaterialData readMaterial(
            int index,
            JsonArray materials,
            JsonArray textures,
            JsonArray images) throws IOException {

        if (index < 0) {
            return MaterialData.DEFAULT;
        }

        JsonObject material =
                materials.get(index).getAsJsonObject();

        JsonObject pbr =
                material.getAsJsonObject("pbrMetallicRoughness");

        float[] color = pbr != null
                ? vector(pbr, "baseColorFactor",
                    new float[]{1, 1, 1, 1})
                : new float[]{1, 1, 1, 1};

        String texturePath = null;

        if (pbr != null && pbr.has("baseColorTexture")) {
            int textureIndex = pbr.getAsJsonObject("baseColorTexture")
                    .get("index").getAsInt();

            int imageIndex = textures.get(textureIndex)
                    .getAsJsonObject().get("source").getAsInt();

            if (imageIndex < 0 || imageIndex >= images.size()) {
                throw new IOException("GLTF: неверный индекс изображения");
            }

            texturePath = String.format(
                    Locale.ROOT,
                    "textures/gltf/image_%03d.png",
                    imageIndex);
        }

        boolean doubleSided = !material.has("doubleSided")
                || material.get("doubleSided").getAsBoolean();

        String alphaMode = material.has("alphaMode")
                ? material.get("alphaMode").getAsString()
                : "OPAQUE";

        return new MaterialData(
                texturePath, color, doubleSided, alphaMode);
    }

    private static float[] vector(
            JsonObject object, String key, float[] fallback) {

        if (!object.has(key)) {
            return fallback.clone();
        }

        JsonArray array = object.getAsJsonArray(key);
        float[] result = new float[array.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsFloat();
        }

        return result;
    }

    private static int number(
            JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static final class MaterialData {
        static final MaterialData DEFAULT = new MaterialData(
                null, new float[]{1, 1, 1, 1}, true, "OPAQUE");

        final String texturePath;
        final float[] color;
        final boolean doubleSided;
        final String alphaMode;

        MaterialData(String texturePath, float[] color,
                     boolean doubleSided, String alphaMode) {
            this.texturePath = texturePath;
            this.color = color;
            this.doubleSided = doubleSided;
            this.alphaMode = alphaMode;
        }
    }

    public static final class Part {
        public final String name;
        public final int nodeIndex;
        public final float[] positions;
        public final float[] normals;
        public final float[] uv;
        public final int[] indices;
        public final float[] translation;
        public final float[] rotation;
        public final float[] scale;
        public final String texturePath;
        public final float[] color;
        public final boolean doubleSided;
        public final String alphaMode;

        private Part(String name, int nodeIndex,
                     float[] positions, float[] normals, float[] uv,
                     int[] indices, float[] translation,
                     float[] rotation, float[] scale,
                     String texturePath, float[] color,
                     boolean doubleSided, String alphaMode) {

            this.name = name;
            this.nodeIndex = nodeIndex;
            this.positions = positions;
            this.normals = normals;
            this.uv = uv;
            this.indices = indices;
            this.translation = translation;
            this.rotation = rotation;
            this.scale = scale;
            this.texturePath = texturePath;
            this.color = color;
            this.doubleSided = doubleSided;
            this.alphaMode = alphaMode;
        }
    }
}
