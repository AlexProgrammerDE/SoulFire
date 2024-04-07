package com.soulfiremc.server.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record LootPoolEntry(@SerializedName("bonus_rolls") double bonusRolls, double rolls, List<LootEntry> entries) {
  public record LootEntry() {
  }
}
