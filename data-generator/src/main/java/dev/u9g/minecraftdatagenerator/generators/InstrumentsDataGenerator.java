package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.enums.Instrument;

public class InstrumentsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "instruments";
    }

    @Override
    public JsonElement generateDataJson() {
        JsonArray array = new JsonArray();
        for (Instrument instrument : Instrument.values()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", instrument.ordinal());
            object.addProperty("name", instrument.asString());
            array.add(object);
        }
        return array;
    }
}
