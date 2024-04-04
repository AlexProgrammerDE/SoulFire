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
package com.soulfiremc.client.gui.navigation;

import com.google.gson.JsonPrimitive;
import com.soulfiremc.client.gui.libs.JMinMaxHelper;
import com.soulfiremc.client.gui.libs.SFSwingUtils;
import com.soulfiremc.client.settings.ClientSettingsManager;
import com.soulfiremc.grpc.generated.ClientPluginSettingsPage;
import com.soulfiremc.grpc.generated.ComboOption;
import com.soulfiremc.grpc.generated.DoubleSetting;
import com.soulfiremc.grpc.generated.IntSetting;
import com.soulfiremc.settings.PropertyKey;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.lenni0451.commons.swing.GBC;

public class GeneratedPanel extends NavigationItem {
  private final ClientPluginSettingsPage settingsPage;

  public GeneratedPanel(
    ClientSettingsManager clientSettingsManager, ClientPluginSettingsPage settingsPage) {
    this.settingsPage = settingsPage;

    setLayout(new GridBagLayout());

    addComponents(this, settingsPage, clientSettingsManager);
  }

  private static JSpinner createIntObject(
    PropertyKey propertyKey, ClientSettingsManager clientSettingsManager, IntSetting intSetting) {
    var spinner =
      new JSpinner(
        new SpinnerNumberModel(
          intSetting.getDef(),
          intSetting.getMin(),
          intSetting.getMax(),
          intSetting.getStep()));
    if (intSetting.hasFormat()) {
      spinner.setEditor(new JSpinner.NumberEditor(spinner, intSetting.getFormat()));
    }

    clientSettingsManager.registerListener(propertyKey, s -> spinner.setValue(s.getAsInt()));
    clientSettingsManager.registerProvider(
      propertyKey, () -> new JsonPrimitive((int) spinner.getValue()));

    return spinner;
  }

  private static JSpinner createDoubleObject(
    PropertyKey propertyKey,
    ClientSettingsManager clientSettingsManager,
    DoubleSetting doubleSetting) {
    var spinner =
      new JSpinner(
        new SpinnerNumberModel(
          doubleSetting.getDef(),
          doubleSetting.getMin(),
          doubleSetting.getMax(),
          doubleSetting.getStep()));
    if (doubleSetting.hasFormat()) {
      spinner.setEditor(new JSpinner.NumberEditor(spinner, doubleSetting.getFormat()));
    }

    clientSettingsManager.registerListener(propertyKey, s -> spinner.setValue(s.getAsDouble()));
    clientSettingsManager.registerProvider(
      propertyKey, () -> new JsonPrimitive((double) spinner.getValue()));

    return spinner;
  }

  public static void addComponents(
    JPanel panel,
    ClientPluginSettingsPage settingsPage,
    ClientSettingsManager clientSettingsManager) {
    var row = 0;
    for (var settingEntry : settingsPage.getEntriesList()) {
      switch (settingEntry.getValueCase()) {
        case SINGLE -> {
          var singleEntry = settingEntry.getSingle();
          var propertyKey = new PropertyKey(settingsPage.getNamespace(), singleEntry.getKey());

          GBC.create(panel)
            .grid(0, row)
            .anchor(GBC.LINE_START)
            .add(
              new JLabel(singleEntry.getUiName()),
              label -> label.setToolTipText(singleEntry.getDescription()));
          var settingType = singleEntry.getType();
          Component component =
            switch (settingType.getValueCase()) {
              case STRING -> {
                var stringEntry = settingType.getString();
                var textField =
                  stringEntry.getSecret()
                    ? new JPasswordField(stringEntry.getDef())
                    : new JTextField(stringEntry.getDef());
                clientSettingsManager.registerListener(
                  propertyKey, s -> textField.setText(s.getAsString()));
                clientSettingsManager.registerProvider(
                  propertyKey, () -> new JsonPrimitive(textField.getText()));

                SFSwingUtils.addUndoRedo(textField);

                yield textField;
              }
              case INT -> {
                var intEntry = settingType.getInt();
                yield createIntObject(propertyKey, clientSettingsManager, intEntry);
              }
              case DOUBLE -> {
                var doubleEntry = settingType.getDouble();
                yield createDoubleObject(propertyKey, clientSettingsManager, doubleEntry);
              }
              case BOOL -> {
                var boolEntry = settingType.getBool();
                var checkBox = new JCheckBox();
                checkBox.setSelected(boolEntry.getDef());
                clientSettingsManager.registerListener(
                  propertyKey, s -> checkBox.setSelected(s.getAsBoolean()));
                clientSettingsManager.registerProvider(
                  propertyKey, () -> new JsonPrimitive(checkBox.isSelected()));

                yield checkBox;
              }
              case COMBO -> {
                var comboEntry = settingType.getCombo();
                var options = comboEntry.getOptionsList();
                @SuppressWarnings("Convert2Diamond")
                var comboBox = new JComboBox<ComboOption>(options.toArray(new ComboOption[0]));
                comboBox.setRenderer(new ComboRenderer());
                comboBox.setSelectedItem(options.get(comboEntry.getDef()));
                clientSettingsManager.registerListener(
                  propertyKey,
                  s ->
                    comboBox.setSelectedItem(
                      options.stream()
                        .filter(o -> o.getId().equals(s.getAsString()))
                        .findFirst()
                        .orElseThrow()));
                clientSettingsManager.registerProvider(
                  propertyKey,
                  () ->
                    new JsonPrimitive(
                      ((ComboOption) Objects.requireNonNull(comboBox.getSelectedItem()))
                        .getId()));

                yield comboBox;
              }
              case VALUE_NOT_SET -> throw new IllegalStateException(
                "Unexpected value: " + settingType.getValueCase());
            };
          GBC.create(panel)
            .grid(1, row++)
            .insets(0, 10, 0, 0)
            .fill(GBC.HORIZONTAL)
            .weightx(1)
            .add(component);
        }
        case MINMAXPAIR -> {
          var minMaxEntry = settingEntry.getMinMaxPair();

          var minPropertyKey =
            new PropertyKey(settingsPage.getNamespace(), minMaxEntry.getMin().getKey());
          var min = minMaxEntry.getMin();
          GBC.create(panel)
            .grid(0, row)
            .anchor(GBC.LINE_START)
            .add(
              new JLabel(min.getUiName()), label -> label.setToolTipText(min.getDescription()));
          var minSpinner =
            createIntObject(minPropertyKey, clientSettingsManager, min.getIntSetting());
          GBC.create(panel)
            .grid(1, row++)
            .insets(0, 10, 0, 0)
            .fill(GBC.HORIZONTAL)
            .weightx(1)
            .add(minSpinner);

          var maxPropertyKey =
            new PropertyKey(settingsPage.getNamespace(), minMaxEntry.getMax().getKey());
          var max = minMaxEntry.getMax();
          GBC.create(panel)
            .grid(0, row)
            .anchor(GBC.LINE_START)
            .add(
              new JLabel(max.getUiName()), label -> label.setToolTipText(max.getDescription()));
          var maxSpinner =
            createIntObject(maxPropertyKey, clientSettingsManager, max.getIntSetting());
          GBC.create(panel)
            .grid(1, row++)
            .insets(0, 10, 0, 0)
            .fill(GBC.HORIZONTAL)
            .weightx(1)
            .add(maxSpinner);

          JMinMaxHelper.applyLink(minSpinner, maxSpinner);
        }
        case VALUE_NOT_SET -> throw new IllegalStateException("Unexpected value: " + settingEntry.getValueCase());
      }
    }

    GBC.fillVerticalSpace(panel);
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
    public Component getListCellRendererComponent(
      JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value instanceof ComboOption option) {
        setText(option.getDisplayName());
      }

      return this;
    }
  }
}
