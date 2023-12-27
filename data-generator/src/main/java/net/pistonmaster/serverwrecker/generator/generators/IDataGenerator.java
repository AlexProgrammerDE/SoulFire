package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonElement;

public interface IDataGenerator {

    String getDataName();

    JsonElement generateDataJson();
}
