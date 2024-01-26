/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.Objects;

public class EffectsDataGenerator implements IDataGenerator {

    public static JsonObject generateEffect(MobEffect statusEffect) {
        var effectDesc = new JsonObject();

        effectDesc.addProperty("id", BuiltInRegistries.MOB_EFFECT.getId(statusEffect));
        effectDesc.addProperty("name", Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(statusEffect)).getPath());

        effectDesc.addProperty("type", statusEffect.isBeneficial() ? "good" : "bad");
        return effectDesc;
    }

    @Override
    public String getDataName() {
        return "effects.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultsArray = new JsonArray();
        BuiltInRegistries.MOB_EFFECT.forEach(effect -> resultsArray.add(generateEffect(effect)));
        return resultsArray;
    }
}