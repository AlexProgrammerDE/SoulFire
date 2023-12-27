package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class InstrumentsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "instruments.json";
    }

    @Override
    public JsonElement generateDataJson() {
        var array = new JsonArray();
        for (var instrument : NoteBlockInstrument.values()) {
            var object = new JsonObject();
            object.addProperty("id", instrument.ordinal());
            object.addProperty("name", instrument.getSerializedName());
            array.add(object);
        }
        return array;
    }
}
