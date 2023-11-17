/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.gui.navigation;

import net.pistonmaster.serverwrecker.grpc.generated.ClientPluginSettingsPage;
import net.pistonmaster.serverwrecker.grpc.generated.ComboOption;
import net.pistonmaster.serverwrecker.grpc.generated.IntSetting;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;

public class PluginSettingsPanel extends NavigationItem {
    private final ClientPluginSettingsPage settingsPage;

    public PluginSettingsPanel(ClientPluginSettingsPage settingsPage) {
        this.settingsPage = settingsPage;

        setLayout(new GridLayout(0, 2));

        for (var settingEntry : settingsPage.getEntriesList()) {
            switch (settingEntry.getValueCase()) {
                case SINGLE -> {
                    var singleEntry = settingEntry.getSingle();

                    add(new JLabel(singleEntry.getName()));
                    var settingType = singleEntry.getType();
                    add(switch (settingType.getValueCase()) {
                        case STRING -> {
                            var stringEntry = settingType.getString();
                            yield new JTextField(stringEntry.getDef());
                        }
                        case INT -> {
                            var intEntry = settingType.getInt();
                            yield createSpinner(intEntry);
                        }
                        case BOOL -> {
                            var boolEntry = settingType.getBool();
                            yield new PresetJCheckBox(boolEntry.getDef());
                        }
                        case COMBO -> {
                            var comboEntry = settingType.getCombo();
                            var options = comboEntry.getOptionsList();
                            @SuppressWarnings("Convert2Diamond")
                            var comboBox = new JComboBox<ComboOption>(options.toArray(new ComboOption[0]));
                            comboBox.setRenderer(new ComboRenderer());
                            comboBox.setSelectedItem(options.get(comboEntry.getDef()));

                            yield comboBox;
                        }
                        case VALUE_NOT_SET ->
                                throw new IllegalStateException("Unexpected value: " + settingType.getValueCase());
                    });
                }
                case MINMAXPAIR -> {
                    var minMaxEntry = settingEntry.getMinMaxPair();

                    var min = minMaxEntry.getMin();
                    add(new JLabel(min.getName()));
                    var minSpinner = createSpinner(min.getIntSetting());
                    add(minSpinner);

                    var max = minMaxEntry.getMax();
                    add(new JLabel(max.getName()));
                    var maxSpinner = createSpinner(max.getIntSetting());
                    add(maxSpinner);

                    JMinMaxHelper.applyLink(minSpinner, maxSpinner);
                }
                case VALUE_NOT_SET ->
                        throw new IllegalStateException("Unexpected value: " + settingEntry.getValueCase());
            }
        }
    }

    private static JSpinner createSpinner(IntSetting intSetting) {
        return new JSpinner(new SpinnerNumberModel(intSetting.getDef(), intSetting.getMin(), intSetting.getMax(), intSetting.getStep()));
    }

    @Override
    public String getNavigationName() {
        return settingsPage.getPageName();
    }

    @Override
    public String getNavigationId() {
        return settingsPage.getPageId();
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
