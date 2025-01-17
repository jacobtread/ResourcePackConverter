package net.hypixel.resourcepack.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hypixel.resourcepack.Converter;
import net.hypixel.resourcepack.PackConverter;
import net.hypixel.resourcepack.Util;
import net.hypixel.resourcepack.pack.Pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ModelConverter extends Converter {

    public ModelConverter(PackConverter packConverter) {
        super(packConverter);
    }

    @Override
    public void convert(Pack pack) throws IOException {
        Path models = pack.getMinecraftPath().resolve("models");
        remapModelJson(models.resolve("block"));
        remapModelJson(models.resolve("item"));
    }

    protected void remapModelJson(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.list(path)
                .filter(path1 -> path1.toString().endsWith(".json"))
                .forEach(model -> {
                    try {
                        JsonObject jsonObject = Util.readJson(packConverter.getGson(), model);

                        // minify the JSON, so we can replace spaces in paths easily
                        // TODO Improvement: handle this in a cleaner way?
                        String content = jsonObject.toString();
                        content = content.replaceAll("items/", "item/");
                        content = content.replaceAll("blocks/", "block/");
                        content = content.replaceAll(" ", "_");

                        Files.write(model, content.getBytes(StandardCharsets.UTF_8));

                        // handle the remapping of textures, for models that use default texture names
                        jsonObject = Util.readJson(packConverter.getGson(), model);
                        if (jsonObject.has("textures")) {
                            NameConverter nameConverter = packConverter.getConverter(NameConverter.class);

                            JsonObject textureObject = jsonObject.getAsJsonObject("textures");
                            for (Map.Entry<String, JsonElement> entry : textureObject.entrySet()) {
                                String value = entry.getValue().getAsString();
                                if (value.startsWith("block/")) {
                                    textureObject.addProperty(entry.getKey(), "block/" + nameConverter.getBlockMapping().remap(value.substring("block/".length())));
                                } else if (value.startsWith("item/")) {
                                    textureObject.addProperty(entry.getKey(), "item/" + nameConverter.getItemMapping().remap(value.substring("item/".length())));
                                }
                            }
                        }
                        Files.write(model, packConverter.getGson()
                                .toJson(jsonObject)
                                .getBytes(StandardCharsets.UTF_8)
                        );
                    } catch (IOException e) {
                        throw Util.propagate(e);
                    }
                });
    }
}