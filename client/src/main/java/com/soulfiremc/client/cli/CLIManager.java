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
package com.soulfiremc.client.cli;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.client.ClientCommandManager;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.client.settings.ClientSettingsManager;
import com.soulfiremc.client.settings.PropertyKey;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.util.structs.ShutdownManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import picocli.CommandLine;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public class CLIManager {
  private final RPCClient rpcClient;
  private final ClientCommandManager clientCommandManager;
  private final Injector injector =
    new InjectorBuilder().addDefaultHandlers("com.soulfiremc").create();
  private final ExecutorService threadPool = Executors.newCachedThreadPool();
  private final ShutdownManager shutdownManager;
  private final ClientSettingsManager clientSettingsManager;
  private UUID cliInstanceId;

  public CLIManager(RPCClient rpcClient, PluginManager pluginManager) {
    injector.register(CLIManager.class, this);
    injector.register(RPCClient.class, rpcClient);

    this.shutdownManager = new ShutdownManager(this::shutdownHook, pluginManager);
    injector.register(ShutdownManager.class, shutdownManager);

    this.rpcClient = rpcClient;
    this.clientSettingsManager = injector.getSingleton(ClientSettingsManager.class);
    this.clientCommandManager = injector.getSingleton(ClientCommandManager.class);
  }

  private static String escapeFormatSpecifiers(String input) {
    return input.replace("%", "%%");
  }

  public void initCLI(String[] args) {
    var cliInstance = rpcClient.instanceStubBlocking()
      .listInstances(InstanceListRequest.newBuilder().build())
      .getInstancesList()
      .stream()
      .filter(instance -> instance.getFriendlyName().equals("cli-attack"))
      .map(InstanceListResponse.Instance::getId)
      .map(UUID::fromString)
      .findFirst();

    cliInstanceId = cliInstance.orElseGet(() -> UUID.fromString(rpcClient.instanceStubBlocking()
      .createInstance(
        InstanceCreateRequest.newBuilder().setFriendlyName("cli-attack").build())
      .getId()));

    var soulFireCommand = new SFCommandDefinition(this);
    var commandLine = new CommandLine(soulFireCommand);
    soulFireCommand.commandLine(commandLine);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.setUsageHelpAutoWidth(true);
    commandLine.setUsageHelpLongOptionsMaxWidth(30);
    commandLine.setExecutionExceptionHandler(
      (ex, cmdLine, parseResult) -> {
        log.error("Exception while executing command", ex);
        return 1;
      });

    registerOptions(commandLine.getCommandSpec());

    commandLine.execute(args);
  }

  @SuppressWarnings("unchecked")
  private void registerOptions(CommandLine.Model.CommandSpec targetCommandSpec) {
    for (var page :
      rpcClient
        .configStubBlocking()
        .getClientData(ClientDataRequest.getDefaultInstance())
        .getInstanceSettingsList()) {
      for (var entry : page.getEntriesList()) {
        var propertyKey = new PropertyKey(page.getNamespace(), entry.getKey());

        var baseArg = "--%s-%s".formatted(page.getNamespace(), entry.getKey());
        switch (entry.getValueCase()) {
          case STRING -> {
            var stringEntry = entry.getString();
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
            var intEntry = entry.getInt();
            var description = escapeFormatSpecifiers(intEntry.getDescription());

            targetCommandSpec.addOption(addIntSetting(
              propertyKey,
              clientSettingsManager,
              description,
              new String[]{baseArg},
              intEntry));
          }
          case DOUBLE -> {
            var doubleEntry = entry.getDouble();
            var description = escapeFormatSpecifiers(doubleEntry.getDescription());

            targetCommandSpec.addOption(addDoubleSetting(
              propertyKey,
              clientSettingsManager,
              description,
              new String[]{baseArg},
              doubleEntry));
          }
          case BOOL -> {
            var boolEntry = entry.getBool();
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
            var comboEntry = entry.getCombo();
            var description = escapeFormatSpecifiers(comboEntry.getDescription());

            var reference = new AtomicReference<String>();
            var optionSpec =
              CommandLine.Model.OptionSpec.builder(new String[]{baseArg})
                .description(description)
                .type(String.class)
                .initialValue(comboEntry.getDef())
                .hasInitialValue(true)
                .completionCandidates(
                  comboEntry.getOptionsList().stream().map(ComboOption::getId)
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
            var stringListEntry = entry.getStringList();
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
            var minMaxEntry = entry.getMinMax();

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
          case VALUE_NOT_SET -> throw new IllegalStateException(
            "Unexpected value: " + entry.getValueCase());
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
}
