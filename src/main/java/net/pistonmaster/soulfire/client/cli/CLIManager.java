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
package net.pistonmaster.soulfire.client.cli;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.client.ClientCommandManager;
import net.pistonmaster.soulfire.client.grpc.RPCClient;
import net.pistonmaster.soulfire.client.settings.SettingsManager;
import net.pistonmaster.soulfire.grpc.generated.*;
import net.pistonmaster.soulfire.server.settings.lib.property.PropertyKey;
import net.pistonmaster.soulfire.util.ShutdownManager;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
@SuppressWarnings("unchecked")
public class CLIManager {
    private final RPCClient rpcClient;
    private final ClientCommandManager clientCommandManager;
    private final Injector injector = new InjectorBuilder()
            .addDefaultHandlers("net.pistonmaster.soulfire")
            .create();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ShutdownManager shutdownManager = new ShutdownManager(this::shutdownHook);
    private final SettingsManager settingsManager = new SettingsManager();

    public CLIManager(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
        injector.register(CLIManager.class, this);
        injector.register(RPCClient.class, rpcClient);
        injector.register(ShutdownManager.class, shutdownManager);
        injector.register(SettingsManager.class, settingsManager);

        this.clientCommandManager = injector.getSingleton(ClientCommandManager.class);
    }

    private static String escapeFormatSpecifiers(String input) {
        return input.replace("%", "%%");
    }

    public void initCLI(String[] args) {
        var soulFireCommand = new SWCommandDefinition(this);
        var commandLine = new CommandLine(soulFireCommand);
        soulFireCommand.commandLine(commandLine);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setUsageHelpAutoWidth(true);
        commandLine.setUsageHelpLongOptionsMaxWidth(30);
        commandLine.setExecutionExceptionHandler((ex, cmdLine, parseResult) -> {
            log.error("Exception while executing command", ex);
            return 1;
        });

        var targetCommandSpec = commandLine.getCommandSpec();
        for (var page : rpcClient.configStubBlocking()
                .getUIClientData(ClientDataRequest.getDefaultInstance())
                .getPluginSettingsList()) {
            for (var entry : page.getEntriesList()) {
                switch (entry.getValueCase()) {
                    case SINGLE -> {
                        var singleEntry = entry.getSingle();
                        var description = escapeFormatSpecifiers(singleEntry.getDescription());

                        var propertyKey = new PropertyKey(page.getNamespace(), singleEntry.getKey());

                        var settingType = singleEntry.getType();
                        targetCommandSpec.addOption(switch (settingType.getValueCase()) {
                            case STRING -> {
                                var stringEntry = settingType.getString();
                                var reference = new AtomicReference<String>();
                                var optionSpec = CommandLine.Model.OptionSpec.builder(singleEntry.getCliFlagsList().toArray(new String[0]))
                                        .description(description)
                                        .type(String.class)
                                        .initialValue(stringEntry.getDef())
                                        .hasInitialValue(true)
                                        .setter(new CommandLine.Model.ISetter() {
                                            @Override
                                            public <T> T set(T value) {
                                                return (T) reference.getAndSet((String) value);
                                            }
                                        })
                                        .build();

                                settingsManager.registerListener(propertyKey, s -> reference.set(s.getAsString()));
                                settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

                                yield optionSpec;
                            }
                            case INT -> {
                                var intEntry = settingType.getInt();
                                yield addIntSetting(propertyKey, settingsManager, description,
                                        singleEntry.getCliFlagsList().toArray(new String[0]), intEntry);
                            }
                            case DOUBLE -> {
                                var doubleEntry = settingType.getDouble();
                                yield addDoubleSetting(propertyKey, settingsManager, description,
                                        singleEntry.getCliFlagsList().toArray(new String[0]), doubleEntry);
                            }
                            case BOOL -> {
                                var boolEntry = settingType.getBool();
                                var reference = new AtomicReference<Boolean>();
                                var optionSpec = CommandLine.Model.OptionSpec.builder(singleEntry.getCliFlagsList().toArray(new String[0]))
                                        .description(description)
                                        .type(boolean.class)
                                        .initialValue(boolEntry.getDef())
                                        .hasInitialValue(true)
                                        .setter(new CommandLine.Model.ISetter() {
                                            @Override
                                            public <T> T set(T value) {
                                                return (T) reference.getAndSet((boolean) value);
                                            }
                                        })
                                        .build();

                                settingsManager.registerListener(propertyKey, s -> reference.set(s.getAsBoolean()));
                                settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

                                yield optionSpec;
                            }
                            case COMBO -> {
                                var comboEntry = settingType.getCombo();
                                var reference = new AtomicReference<String>();

                                var optionSpec = CommandLine.Model.OptionSpec.builder(singleEntry.getCliFlagsList().toArray(new String[0]))
                                        .description(description)
                                        .typeInfo(new ComboTypeInfo(comboEntry))
                                        .initialValue(comboEntry.getOptionsList().get(comboEntry.getDef()).getId())
                                        .hasInitialValue(true)
                                        .setter(new CommandLine.Model.ISetter() {
                                            @Override
                                            public <T> T set(T value) {
                                                return (T) reference.getAndSet((String) value);
                                            }
                                        })
                                        .build();

                                settingsManager.registerListener(propertyKey,
                                        s -> reference.set(comboEntry.getOptionsList().stream()
                                                .filter(o -> o.getId().equals(s.getAsString()))
                                                .findFirst()
                                                .orElseThrow()
                                                .getId()
                                        ));
                                settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

                                yield optionSpec;
                            }
                            case VALUE_NOT_SET ->
                                    throw new IllegalStateException("Unexpected value: " + settingType.getValueCase());
                        });
                    }
                    case MINMAXPAIR -> {
                        var minMaxEntry = entry.getMinMaxPair();

                        var min = minMaxEntry.getMin();
                        var minDescription = escapeFormatSpecifiers(min.getDescription());
                        var minPropertyKey = new PropertyKey(page.getNamespace(), min.getKey());
                        targetCommandSpec.addOption(addIntSetting(minPropertyKey, settingsManager, minDescription,
                                min.getCliFlagsList().toArray(new String[0]), min.getIntSetting()));

                        var max = minMaxEntry.getMax();
                        var maxDescription = escapeFormatSpecifiers(max.getDescription());
                        var maxPropertyKey = new PropertyKey(page.getNamespace(), max.getKey());
                        targetCommandSpec.addOption(addIntSetting(maxPropertyKey, settingsManager, maxDescription,
                                max.getCliFlagsList().toArray(new String[0]), max.getIntSetting()));
                    }
                    case VALUE_NOT_SET -> throw new IllegalStateException("Unexpected value: " + entry.getValueCase());
                }
            }
        }

        commandLine.execute(args);
    }

    private CommandLine.Model.OptionSpec addIntSetting(PropertyKey propertyKey, SettingsManager settingsManager, String cliDescription, String[] cliNames, IntSetting intEntry) {
        var reference = new AtomicInteger();
        var optionSpec = CommandLine.Model.OptionSpec.builder(cliNames)
                .description(cliDescription)
                .type(int.class)
                .initialValue(intEntry.getDef())
                .hasInitialValue(true)
                .setter(new CommandLine.Model.ISetter() {
                    @Override
                    public <T> T set(T value) {
                        return (T) (Integer) reference.getAndSet((int) value);
                    }
                })
                .build();

        settingsManager.registerListener(propertyKey, s -> reference.set(s.getAsInt()));
        settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

        return optionSpec;
    }

    private CommandLine.Model.OptionSpec addDoubleSetting(PropertyKey propertyKey, SettingsManager settingsManager, String cliDescription, String[] cliNames, DoubleSetting doubleSetting) {
        var reference = new AtomicDouble();
        var optionSpec = CommandLine.Model.OptionSpec.builder(cliNames)
                .description(cliDescription)
                .type(double.class)
                .initialValue(doubleSetting.getDef())
                .hasInitialValue(true)
                .setter(new CommandLine.Model.ISetter() {
                    @Override
                    public <T> T set(T value) {
                        return (T) (Double) reference.getAndSet((double) value);
                    }
                })
                .build();

        settingsManager.registerListener(propertyKey, s -> reference.set(s.getAsDouble()));
        settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(reference.get()));

        return optionSpec;
    }

    private void shutdownHook() {
        threadPool.shutdown();
    }

    public void shutdown() {
        shutdownManager.shutdownSoftware(true);
    }

    private record ComboTypeInfo(ComboSetting comboSetting) implements CommandLine.Model.ITypeInfo {
        @Override
        public boolean isBoolean() {
            return false;
        }

        @Override
        public boolean isMultiValue() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public boolean isMap() {
            return false;
        }

        @Override
        public boolean isEnum() {
            return true;
        }

        @Override
        public List<String> getEnumConstantNames() {
            return comboSetting.getOptionsList().stream().map(ComboOption::getId).toList();
        }

        @Override
        public String getClassName() {
            return null;
        }

        @Override
        public String getClassSimpleName() {
            return null;
        }

        @Override
        public List<CommandLine.Model.ITypeInfo> getAuxiliaryTypeInfos() {
            return null;
        }

        @Override
        public List<String> getActualGenericTypeArguments() {
            return null;
        }

        @Override
        public Class<?> getType() {
            return null;
        }

        @Override
        public Class<?>[] getAuxiliaryTypes() {
            return new Class[0];
        }
    }
}
