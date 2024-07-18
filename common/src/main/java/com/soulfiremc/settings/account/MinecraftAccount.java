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
package com.soulfiremc.settings.account;

import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.settings.account.service.AccountData;
import com.soulfiremc.settings.account.service.BedrockData;
import com.soulfiremc.settings.account.service.OfflineJavaData;
import com.soulfiremc.settings.account.service.OnlineChainJavaData;
import com.soulfiremc.settings.account.service.OnlineJavaDataLike;
import com.soulfiremc.settings.account.service.OnlineSimpleJavaData;
import java.util.UUID;
import lombok.NonNull;

/**
 * Represents an authenticated MC account. This can be a premium, offline or bedrock account. Beware
 * that the profileId is not a valid online UUID for offline and bedrock accounts.
 *
 * @param authType      The type of authentication
 * @param profileId     Identifier that uniquely identifies the account
 * @param lastKnownName The last known name of the account
 * @param accountData   The data of the account (values depend on the authType)
 */
public record MinecraftAccount(
  @NonNull AuthType authType,
  @NonNull UUID profileId,
  @NonNull String lastKnownName,
  @NonNull AccountData accountData) {
  public static MinecraftAccount fromProto(MinecraftAccountProto account) {
    return new MinecraftAccount(
      AuthType.valueOf(account.getType().name()),
      UUID.fromString(account.getProfileId()),
      account.getLastKnownName(),
      switch (account.getAccountDataCase()) {
        case ONLINESIMPLEJAVADATA -> OnlineSimpleJavaData.fromProto(account.getOnlineSimpleJavaData());
        case ONLINECHAINJAVADATA -> OnlineChainJavaData.fromProto(account.getOnlineChainJavaData());
        case OFFLINEJAVADATA -> OfflineJavaData.fromProto(account.getOfflineJavaData());
        case BEDROCKDATA -> BedrockData.fromProto(account.getBedrockData());
        case ACCOUNTDATA_NOT_SET -> throw new IllegalArgumentException("AccountData not set");
      });
  }

  @Override
  public String toString() {
    return "MinecraftAccount(authType=%s, profileId=%s, lastKnownName=%s)"
      .formatted(authType, profileId, lastKnownName);
  }

  public boolean isPremiumJava() {
    return accountData instanceof OnlineJavaDataLike;
  }

  public boolean isPremiumBedrock() {
    return accountData instanceof BedrockData;
  }

  public MinecraftAccountProto toProto() {
    var builder =
      MinecraftAccountProto.newBuilder()
        .setType(MinecraftAccountProto.AccountTypeProto.valueOf(authType.name()))
        .setProfileId(profileId.toString())
        .setLastKnownName(lastKnownName);

    switch (accountData) {
      case BedrockData bedrockData -> builder.setBedrockData(bedrockData.toProto());
      case OfflineJavaData offlineJavaData -> builder.setOfflineJavaData(offlineJavaData.toProto());
      case OnlineSimpleJavaData onlineSimpleJavaData -> builder.setOnlineSimpleJavaData(onlineSimpleJavaData.toProto());
      case OnlineChainJavaData onlineChainJavaData -> builder.setOnlineChainJavaData(onlineChainJavaData.toProto());
    }

    return builder.build();
  }
}
