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
package net.pistonmaster.soulfire.client.gui.navigation;

import com.google.gson.JsonPrimitive;
import net.pistonmaster.soulfire.client.gui.libs.JMinMaxHelper;
import net.pistonmaster.soulfire.client.gui.libs.SwingTextUtils;
import net.pistonmaster.soulfire.client.settings.SettingsManager;
import net.pistonmaster.soulfire.grpc.generated.ClientPluginSettingsPage;
import net.pistonmaster.soulfire.grpc.generated.ComboOption;
import net.pistonmaster.soulfire.grpc.generated.DoubleSetting;
import net.pistonmaster.soulfire.grpc.generated.IntSetting;
import net.pistonmaster.soulfire.server.settings.lib.property.PropertyKey;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.Objects;

public class GeneratedPanel extends NavigationItem {
    private final ClientPluginSettingsPage settingsPage;

    public GeneratedPanel(SettingsManager settingsManager, ClientPluginSettingsPage settingsPage) {
        this.settingsPage = settingsPage;

        setLayout(new GridLayout(0, 2));

        addComponents(this, settingsPage, settingsManager);
    }

    private static JSpinner createIntObject(PropertyKey propertyKey, SettingsManager settingsManager, IntSetting intSetting) {
        var spinner = new JSpinner(new SpinnerNumberModel(intSetting.getDef(), intSetting.getMin(), intSetting.getMax(), intSetting.getStep()));
        if (intSetting.hasFormat()) {
            spinner.setEditor(new JSpinner.NumberEditor(spinner, intSetting.getFormat()));
        }

        settingsManager.registerListener(propertyKey, s -> spinner.setValue(s.getAsInt()));
        settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive((int) spinner.getValue()));

        return spinner;
    }

    private static JSpinner createDoubleObject(PropertyKey propertyKey, SettingsManager settingsManager, DoubleSetting doubleSetting) {
        var spinner = new JSpinner(new SpinnerNumberModel(doubleSetting.getDef(), doubleSetting.getMin(), doubleSetting.getMax(), doubleSetting.getStep()));
        if (doubleSetting.hasFormat()) {
            spinner.setEditor(new JSpinner.NumberEditor(spinner, doubleSetting.getFormat()));
        }

        settingsManager.registerListener(propertyKey, s -> spinner.setValue(s.getAsDouble()));
        settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive((double) spinner.getValue()));

        return spinner;
    }

    public static void addComponents(JPanel panel, ClientPluginSettingsPage settingsPage, SettingsManager settingsManager) {
        for (var settingEntry : settingsPage.getEntriesList()) {
            switch (settingEntry.getValueCase()) {
                case SINGLE -> {
                    var singleEntry = settingEntry.getSingle();
                    var propertyKey = new PropertyKey(settingsPage.getNamespace(), singleEntry.getKey());

                    var label = new JLabel(singleEntry.getUiName());
                    label.setToolTipText(singleEntry.getDescription());
                    panel.add(label);
                    var settingType = singleEntry.getType();
                    panel.add(switch (settingType.getValueCase()) {
                        case STRING -> {
                            var stringEntry = settingType.getString();
                            var textField = stringEntry.getSecret() ? new JPasswordField(stringEntry.getDef())
                                    : new JTextField(stringEntry.getDef());
                            settingsManager.registerListener(propertyKey, s -> textField.setText(s.getAsString()));
                            settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(textField.getText()));

                            SwingTextUtils.addUndoRedo(textField);

                            yield textField;
                        }
                        case INT -> {
                            var intEntry = settingType.getInt();
                            yield createIntObject(propertyKey, settingsManager, intEntry);
                        }
                        case DOUBLE -> {
                            var doubleEntry = settingType.getDouble();
                            yield createDoubleObject(propertyKey, settingsManager, doubleEntry);
                        }
                        case BOOL -> {
                            var boolEntry = settingType.getBool();
                            var checkBox = new JCheckBox();
                            checkBox.setSelected(boolEntry.getDef());
                            settingsManager.registerListener(propertyKey, s -> checkBox.setSelected(s.getAsBoolean()));
                            settingsManager.registerProvider(propertyKey, () -> new JsonPrimitive(checkBox.isSelected()));

                            yield checkBox;
                        }
                        case COMBO -> {
                            var comboEntry = settingType.getCombo();
                            var options = comboEntry.getOptionsList();
                            @SuppressWarnings("Convert2Diamond")
                            var comboBox = new JComboBox<ComboOption>(options.toArray(new ComboOption[0]));
                            comboBox.setRenderer(new ComboRenderer());
                            comboBox.setSelectedItem(options.get(comboEntry.getDef()));
                            settingsManager.registerListener(propertyKey,
                                    s -> comboBox.setSelectedItem(options.stream()
                                            .filter(o -> o.getId().equals(s.getAsString()))
                                            .findFirst()
                                            .orElseThrow()
                                    ));
                            settingsManager.registerProvider(propertyKey,
                                    () -> new JsonPrimitive(((ComboOption) Objects.requireNonNull(comboBox.getSelectedItem())).getId()));

                            yield comboBox;
                        }
                        case VALUE_NOT_SET ->
                                throw new IllegalStateException("Unexpected value: " + settingType.getValueCase());
                    });
                }
                case MINMAXPAIR -> {
                    var minMaxEntry = settingEntry.getMinMaxPair();

                    var minPropertyKey = new PropertyKey(settingsPage.getNamespace(), minMaxEntry.getMin().getKey());
                    var min = minMaxEntry.getMin();
                    var minLabel = new JLabel(min.getUiName());
                    minLabel.setToolTipText(min.getDescription());
                    panel.add(minLabel);
                    var minSpinner = createIntObject(minPropertyKey, settingsManager, min.getIntSetting());
                    panel.add(minSpinner);

                    var maxPropertyKey = new PropertyKey(settingsPage.getNamespace(), minMaxEntry.getMax().getKey());
                    var max = minMaxEntry.getMax();
                    var maxLabel = new JLabel(max.getUiName());
                    maxLabel.setToolTipText(max.getDescription());
                    panel.add(maxLabel);
                    var maxSpinner = createIntObject(maxPropertyKey, settingsManager, max.getIntSetting());
                    panel.add(maxSpinner);

                    JMinMaxHelper.applyLink(minSpinner, maxSpinner);
                }
                case VALUE_NOT_SET ->
                        throw new IllegalStateException("Unexpected value: " + settingEntry.getValueCase());
            }
        }
    }

    @Override
    public String getNavigationName() {
        return settingsPage.getPageName();
    }

    @Override
    public String getNavigationId() {
        return settingsPage.getNamespace();
    }

    private static class ComboRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ComboOption option) {
                setText(option.getDisplayName());
            }

            return this;
        }
    }
}
