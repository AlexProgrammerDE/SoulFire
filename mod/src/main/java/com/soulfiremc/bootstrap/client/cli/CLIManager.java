/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.bootstrap.client.cli;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.bootstrap.client.ClientCommandManager;
import com.soulfiremc.bootstrap.client.grpc.RPCClient;
import com.soulfiremc.bootstrap.client.settings.ClientSettingsManager;
import com.soulfiremc.bootstrap.client.settings.PropertyKey;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.util.structs.ShutdownManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public final class CLIManager {
  private final RPCClient rpcClient;
  private final ClientCommandManager clientCommandManager;
  private final ExecutorService threadPool = Executors.newCachedThreadPool();
  private final ShutdownManager shutdownManager;
  private final ClientSettingsManager clientSettingsManager;
  private UUID cliInstanceId;

  public CLIManager(RPCClient rpcClient) {
    this.shutdownManager = new ShutdownManager(this::shutdownHook);

    this.rpcClient = rpcClient;
    this.clientSettingsManager = new ClientSettingsManager(this.rpcClient);
    this.clientCommandManager = new ClientCommandManager(this.rpcClient, this);
  }

  private static String escapeFormatSpecifiers(String input) {
    return input.replace("%", "%%");
  }

  public void initCLI(String[] args) {
    var cliInstance = rpcClient.instanceStubBlocking()
      .listInstances(InstanceListRequest.newBuilder().build())
      .getInstancesList()
      .stream()
      .filter(instance -> "CLI Attack".equals(instance.getFriendlyName()))
      .map(InstanceListResponse.Instance::getId)
      .map(UUID::fromString)
      .findFirst();

    cliInstanceId = cliInstance.orElseGet(() -> UUID.fromString(rpcClient.instanceStubBlocking()
      .createInstance(
        InstanceCreateRequest.newBuilder().setFriendlyName("CLI Attack").build())
      .getId()));

    var soulFireCommand = new SFCommandDefinition(this);
    var commandLine = new CommandLine(soulFireCommand);
    soulFireCommand.commandLine(commandLine);
    commandLine.setResourceBundle(new MapResourceBundle(
      Map.of(
        "usage.description.0",
        BuildData.DESCRIPTION
      )
    ));
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.setUsageHelpAutoWidth(true);
    commandLine.setUsageHelpLongOptionsMaxWidth(30);
    commandLine.setExecutionExceptionHandler(
      (ex, _, _) -> {
        log.error("Exception while executing command", ex);
        return 1;
      });

    registerOptions(commandLine.getCommandSpec());

    commandLine.execute(args);
  }

  @SuppressWarnings("unchecked")
  private void registerOptions(CommandLine.Model.CommandSpec targetCommandSpec) {
    var instanceInfo = rpcClient.instanceStubBlocking()
      .getInstanceInfo(InstanceInfoRequest.newBuilder().setId(cliInstanceId.toString()).build());

    // Build a lookup map for settings definitions by their identifier
    var definitionsMap = new HashMap<String, SettingsDefinition>();
    for (var definition : instanceInfo.getSettingsDefinitionsList()) {
      var id = definition.getId();
      var key = id.getNamespace() + ":" + id.getKey();
      definitionsMap.put(key, definition);
    }

    for (var page : instanceInfo.getInstanceSettingsList()) {
      for (var entryId : page.getEntriesList()) {
        var propertyKey = new PropertyKey(entryId.getNamespace(), entryId.getKey());
        var definitionKey = entryId.getNamespace() + ":" + entryId.getKey();
        var definition = definitionsMap.get(definitionKey);

        if (definition == null) {
          log.warn("No definition found for setting: {}", definitionKey);
          continue;
        }

        var baseArg = "--%s-%s".formatted(entryId.getNamespace(), entryId.getKey());
        switch (definition.getTypeCase()) {
          case STRING -> {
            var stringEntry = definition.getString();
            var description = escapeFormatSpecifiers(stringEntry.getDescription());

            var reference = new AtomicReference<String>();
            var optionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg})
                .description(description)
                .type(String.class)
                .initialValue(stringEntry.getDef())
                .hasInitialValue(true)
                .setter(
                  new CommandLine.Model.ISetter() {
                    @Override
                    public <T> T set(T value) {
                      return (T) reference.getAndSet((String) value);
                    }
                  })
                .build();

            clientSettingsManager.registerListener(
              propertyKey, s -> reference.set(s.getAsString()));
            clientSettingsManager.registerProvider(
              propertyKey, () -> new JsonPrimitive(reference.get()));

            targetCommandSpec.addOption(optionSpec);
          }
          case INT -> {
            var intEntry = definition.getInt();
            var description = escapeFormatSpecifiers(intEntry.getDescription());

            targetCommandSpec.addOption(addIntSetting(
              propertyKey,
              clientSettingsManager,
              description,
              new String[]{baseArg},
              intEntry));
          }
          case DOUBLE -> {
            var doubleEntry = definition.getDouble();
            var description = escapeFormatSpecifiers(doubleEntry.getDescription());

            targetCommandSpec.addOption(addDoubleSetting(
              propertyKey,
              clientSettingsManager,
              description,
              new String[]{baseArg},
              doubleEntry));
          }
          case BOOL -> {
            var boolEntry = definition.getBool();
            var description = escapeFormatSpecifiers(boolEntry.getDescription());

            var reference = new AtomicReference<Boolean>();
            var optionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg})
                .description(description)
                .type(boolean.class)
                .initialValue(boolEntry.getDef())
                .hasInitialValue(true)
                .setter(
                  new CommandLine.Model.ISetter() {
                    @Override
                    public <T> T set(T value) {
                      return (T) reference.getAndSet((boolean) value);
                    }
                  })
                .build();

            clientSettingsManager.registerListener(
              propertyKey, s -> reference.set(s.getAsBoolean()));
            clientSettingsManager.registerProvider(
              propertyKey, () -> new JsonPrimitive(reference.get()));

            targetCommandSpec.addOption(optionSpec);
          }
          case COMBO -> {
            var comboEntry = definition.getCombo();
            var description = escapeFormatSpecifiers(comboEntry.getDescription());

            var reference = new AtomicReference<String>();
            var optionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg})
                .description(description)
                .type(String.class)
                .initialValue(comboEntry.getDef())
                .hasInitialValue(true)
                .completionCandidates(
                  comboEntry.getOptionsList().stream().map(ComboSetting.Option::getId)
                    ::iterator)
                .setter(
                  new CommandLine.Model.ISetter() {
                    @Override
                    public <T> T set(T value) {
                      return (T) reference.getAndSet((String) value);
                    }
                  })
                .build();

            clientSettingsManager.registerListener(
              propertyKey, s -> reference.set(s.getAsString()));
            clientSettingsManager.registerProvider(
              propertyKey, () -> new JsonPrimitive(reference.get()));

            targetCommandSpec.addOption(optionSpec);
          }
          case STRING_LIST -> {
            var stringListEntry = definition.getStringList();
            var description = escapeFormatSpecifiers(stringListEntry.getDescription());

            var reference = new AtomicReference<String[]>();
            var optionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg})
                .description(description)
                .type(String[].class)
                .initialValue(stringListEntry.getDefList().toArray(new String[0]))
                .hasInitialValue(true)
                .setter(
                  new CommandLine.Model.ISetter() {
                    @Override
                    public <T> T set(T value) {
                      return (T) reference.getAndSet((String[]) value);
                    }
                  })
                .build();

            clientSettingsManager.registerListener(
              propertyKey, s -> reference.set(s.getAsJsonArray()
                .asList()
                .stream()
                .map(JsonElement::getAsString)
                .toArray(String[]::new)));
            clientSettingsManager.registerProvider(
              propertyKey, () -> {
                var array = new JsonArray();
                for (var element : reference.get()) {
                  array.add(new JsonPrimitive(element));
                }

                return array;
              });

            targetCommandSpec.addOption(optionSpec);
          }
          case MIN_MAX -> {
            var minMaxEntry = definition.getMinMax();

            var minEntry = minMaxEntry.getMinEntry();
            var minRef = new AtomicInteger();
            var minOptionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg + "-min"})
                .description(escapeFormatSpecifiers(minEntry.getDescription()))
                .type(int.class)
                .initialValue(minEntry.getDef())
                .hasInitialValue(true)
                .setter(
                  new CommandLine.Model.ISetter() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T set(T value) {
                      return (T) (Integer) minRef.getAndSet((int) value);
                    }
                  })
                .build();

            var maxEntry = minMaxEntry.getMaxEntry();
            var maxRef = new AtomicInteger();
            var maxOptionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg + "-max"})
                .description(escapeFormatSpecifiers(maxEntry.getDescription()))
                .type(int.class)
                .initialValue(maxEntry.getDef())
                .hasInitialValue(true)
                .setter(
                  new CommandLine.Model.ISetter() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T set(T value) {
                      return (T) (Integer) maxRef.getAndSet((int) value);
                    }
                  })
                .build();

            clientSettingsManager.registerListener(propertyKey, s -> {
              minRef.set(s.getAsJsonObject().get("min").getAsInt());
              maxRef.set(s.getAsJsonObject().get("max").getAsInt());
            });
            clientSettingsManager.registerProvider(propertyKey, () -> {
              var obj = new JsonObject();
              obj.addProperty("min", minRef.get());
              obj.addProperty("max", maxRef.get());
              return obj;
            });

            targetCommandSpec.addOption(minOptionSpec);
            targetCommandSpec.addOption(maxOptionSpec);
          }
          case TYPE_NOT_SET -> throw new IllegalStateException(
            "Unexpected value: " + definition.getTypeCase());
        }
      }
    }
  }

  private CommandLine.Model.OptionSpec addIntSetting(
    PropertyKey propertyKey,
    ClientSettingsManager clientSettingsManager,
    String cliDescription,
    String[] cliNames,
    IntSetting intEntry) {
    var reference = new AtomicInteger();
    var optionSpec =
      CommandLine.Model.OptionSpec.builder(cliNames)
        .description(cliDescription)
        .type(int.class)
        .initialValue(intEntry.getDef())
        .hasInitialValue(true)
        .setter(
          new CommandLine.Model.ISetter() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T set(T value) {
              return (T) (Integer) reference.getAndSet((int) value);
            }
          })
        .build();

    clientSettingsManager.registerListener(propertyKey, s -> reference.set(s.getAsInt()));
    clientSettingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

    return optionSpec;
  }

  private CommandLine.Model.OptionSpec addDoubleSetting(
    PropertyKey propertyKey,
    ClientSettingsManager clientSettingsManager,
    String cliDescription,
    String[] cliNames,
    DoubleSetting doubleSetting) {
    var reference = new AtomicDouble();
    var optionSpec =
      CommandLine.Model.OptionSpec.builder(cliNames)
        .description(cliDescription)
        .type(double.class)
        .initialValue(doubleSetting.getDef())
        .hasInitialValue(true)
        .setter(
          new CommandLine.Model.ISetter() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T set(T value) {
              return (T) (Double) reference.getAndSet((double) value);
            }
          })
        .build();

    clientSettingsManager.registerListener(propertyKey, s -> reference.set(s.getAsDouble()));
    clientSettingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

    return optionSpec;
  }

  private void shutdownHook() {
    threadPool.shutdown();
  }

  public void shutdown() {
    shutdownManager.shutdownSoftware(true);
  }

  public static class MapResourceBundle extends ResourceBundle {
    private final Map<String, Object> lookup;

    public MapResourceBundle(Map<String, Object> values) {
      this.lookup = Map.copyOf(values);
    }

    @Override
    protected Object handleGetObject(String key) {
      return lookup.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
      return Collections.enumeration(lookup.keySet());
    }
  }
}
