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
import com.google.gson.JsonPrimitive;
import com.soulfiremc.client.ClientCommandManager;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.client.settings.ClientSettingsManager;
import com.soulfiremc.grpc.generated.ClientDataRequest;
import com.soulfiremc.grpc.generated.ComboOption;
import com.soulfiremc.grpc.generated.DoubleSetting;
import com.soulfiremc.grpc.generated.IntSetting;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.util.CommandHistoryManager;
import com.soulfiremc.util.SFPathConstants;
import com.soulfiremc.util.ShutdownManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import picocli.CommandLine;

@Slf4j
@Getter
public class CLIManager {
  private final RPCClient rpcClient;
  private final ClientCommandManager clientCommandManager;
  private final CommandHistoryManager commandHistoryManager = new CommandHistoryManager(SFPathConstants.CLIENT_DATA_DIRECTORY);
  private final Injector injector =
    new InjectorBuilder().addDefaultHandlers("com.soulfiremc").create();
  private final ExecutorService threadPool = Executors.newCachedThreadPool();
  private final ShutdownManager shutdownManager;
  private final ClientSettingsManager clientSettingsManager;

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
        .getPluginSettingsList()) {
      for (var entry : page.getEntriesList()) {
        switch (entry.getValueCase()) {
          case SINGLE -> {
            var singleEntry = entry.getSingle();
            var description = escapeFormatSpecifiers(singleEntry.getDescription());

            var propertyKey = new PropertyKey(page.getNamespace(), singleEntry.getKey());

            var settingType = singleEntry.getType();
            targetCommandSpec.addOption(
              switch (settingType.getValueCase()) {
                case STRING -> {
                  var stringEntry = settingType.getString();
                  var reference = new AtomicReference<String>();
                  var optionSpec =
                    CommandLine.Model.OptionSpec.builder(
                        singleEntry.getCliFlagsList().toArray(new String[0]))
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

                  yield optionSpec;
                }
                case INT -> {
                  var intEntry = settingType.getInt();
                  yield addIntSetting(
                    propertyKey,
                    clientSettingsManager,
                    description,
                    singleEntry.getCliFlagsList().toArray(new String[0]),
                    intEntry);
                }
                case DOUBLE -> {
                  var doubleEntry = settingType.getDouble();
                  yield addDoubleSetting(
                    propertyKey,
                    clientSettingsManager,
                    description,
                    singleEntry.getCliFlagsList().toArray(new String[0]),
                    doubleEntry);
                }
                case BOOL -> {
                  var boolEntry = settingType.getBool();
                  var reference = new AtomicReference<Boolean>();
                  var optionSpec =
                    CommandLine.Model.OptionSpec.builder(
                        singleEntry.getCliFlagsList().toArray(new String[0]))
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

                  yield optionSpec;
                }
                case COMBO -> {
                  var comboEntry = settingType.getCombo();
                  var reference = new AtomicReference<String>();

                  var optionSpec =
                    CommandLine.Model.OptionSpec.builder(
                        singleEntry.getCliFlagsList().toArray(new String[0]))
                      .description(description)
                      .type(String.class)
                      .initialValue(
                        comboEntry.getOptionsList().get(comboEntry.getDef()).getId())
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

                  yield optionSpec;
                }
                case VALUE_NOT_SET -> throw new IllegalStateException(
                  "Unexpected value: " + settingType.getValueCase());
              });
          }
          case MINMAXPAIR -> {
            var minMaxEntry = entry.getMinMaxPair();

            var min = minMaxEntry.getMin();
            var minDescription = escapeFormatSpecifiers(min.getDescription());
            var minPropertyKey = new PropertyKey(page.getNamespace(), min.getKey());
            targetCommandSpec.addOption(
              addIntSetting(
                minPropertyKey,
                clientSettingsManager,
                minDescription,
                min.getCliFlagsList().toArray(new String[0]),
                min.getIntSetting()));

            var max = minMaxEntry.getMax();
            var maxDescription = escapeFormatSpecifiers(max.getDescription());
            var maxPropertyKey = new PropertyKey(page.getNamespace(), max.getKey());
            targetCommandSpec.addOption(
              addIntSetting(
                maxPropertyKey,
                clientSettingsManager,
                maxDescription,
                max.getCliFlagsList().toArray(new String[0]),
                max.getIntSetting()));
          }
          case VALUE_NOT_SET -> throw new IllegalStateException("Unexpected value: " + entry.getValueCase());
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
