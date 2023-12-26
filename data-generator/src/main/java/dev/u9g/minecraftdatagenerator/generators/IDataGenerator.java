package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonElement;

public interface IDataGenerator {

    String getDataName();

    JsonElement generateDataJson();
}
