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
package com.soulfiremc.server.plugins;

import com.openai.models.chat.completions.*;
import com.soulfiremc.grpc.generated.StringSetting;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.InternalPluginClass;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.settings.instance.AISettings;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.util.SFHelpers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@InternalPluginClass
public final class CaptchaSolver extends InternalPlugin {
  public CaptchaSolver() {
    super(new PluginInfo(
      "captcha-solver",
      "1.0.0",
      "Solve captchas",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  private static void handleTextInput(BotConnection connection, String captchaText) {
    var settingsSource = connection.settingsSource();
    var response = String.format(settingsSource.get(CaptchaSolverSettings.RESPONSE_COMMAND), captchaText);
    log.debug("Extracted captcha text: {}", captchaText);
    connection.sendChatMessage(response);
  }

  private static void handleImageInput(BotConnection connection, String img) {
    var settingsSource = connection.settingsSource();
    var api = AISettings.create(settingsSource);
    var model = settingsSource.get(CaptchaSolverSettings.MODEL);

    var requestModel = ChatCompletionCreateParams.builder()
      .model(model)
      .maxCompletionTokens(64)
      .addMessage(ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
        .content(ChatCompletionSystemMessageParam.Content.ofText(settingsSource.get(CaptchaSolverSettings.PROMPT)))
        .build()))
      .addMessage(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
        .content(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(List.of(
          ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
            .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
              .url("data:image/png;base64,%s".formatted(img))
              .build())
            .build())
        )))
        .build()
      ))
      .build();

    log.debug("Detecting captcha: {}", requestModel);
    var chatResult = api.chat().completions().create(requestModel);
    var response = chatResult.choices().stream().findFirst()
      .flatMap(f -> f.message().content())
      .orElse("No text response");

    if (response.isBlank()) {
      log.debug("AI response is empty");
      return;
    }

    if (response.length() > 32) {
      response = response.substring(0, 32);
    }

    response = SFHelpers.stripForChat(response);

    response = String.format(settingsSource.get(CaptchaSolverSettings.RESPONSE_COMMAND), response);

    log.debug("AI response: {}", response);
    connection.sendChatMessage(response);
  }

  private static String toBase64PNG(BufferedImage image) {
    try (var os = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", os);
      return Base64.getEncoder().encodeToString(os.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to convert image to base64", e);
    }
  }

  private static String getImageFromSource(BotConnection connection) {
    var settingsSource = connection.settingsSource();
    var imageSource = settingsSource.get(CaptchaSolverSettings.IMAGE_SOURCE, CaptchaSolverSettings.ImageSource.class);

    return switch (imageSource) {
      case MAP_IN_HAND -> {
        var player = connection.minecraft().player;
        var level = connection.minecraft().level;
        if (player == null || level == null) {
          throw new IllegalStateException("Player or level is null, cannot get map image");
        }

        var item = player.getItemInHand(InteractionHand.MAIN_HAND);
        var mapId = item.get(DataComponents.MAP_ID);
        if (mapId == null) {
          throw new IllegalStateException("No map item in hand");
        }

        var mapState = level.getMapData(mapId);
        if (mapState == null) {
          throw new IllegalStateException("No map data found for id: " + mapId);
        }

        yield toBase64PNG(SFHelpers.toBufferedImage(mapState));
      }
    };
  }

  @EventHandler
  public static void onMessage(ChatMessageReceiveEvent event) {
    var settingsSource = event.connection().settingsSource();
    if (!settingsSource.get(CaptchaSolverSettings.ENABLED)) {
      return;
    }

    var trigger = settingsSource.get(CaptchaSolverSettings.CAPTCHA_TRIGGER, CaptchaSolverSettings.CaptchaTrigger.class);

    event.connection().scheduler().execute(() -> {
      try {
        var plainMessage = event.parseToPlainText();

        switch (trigger) {
          case CHAT_MESSAGE -> {
            var textTrigger = settingsSource.get(CaptchaSolverSettings.TEXT_TRIGGER);
            if (plainMessage.contains(textTrigger)) {
              handleImageInput(event.connection(), getImageFromSource(event.connection()));
            }
          }
          case TEXT_BASED -> {
            var regex = settingsSource.get(CaptchaSolverSettings.CAPTCHA_REGEX);
            var pattern = Pattern.compile(regex);
            var matcher = pattern.matcher(plainMessage);
            if (matcher.find() && matcher.groupCount() >= 1) {
              var captchaText = matcher.group(1);
              handleTextInput(event.connection(), captchaText);
            }
          }
        }
      } catch (Exception e) {
        log.error("Failed to detect captcha", e);
      }
    });
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(CaptchaSolverSettings.class, "Captcha Solver", this, "eye", CaptchaSolverSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class CaptchaSolverSettings implements SettingsObject {
    private static final String NAMESPACE = "captcha-solver";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Captcha Solver")
        .description("Enable the Captcha Solver")
        .defaultValue(false)
        .build();
    public static final StringProperty PROMPT =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("prompt")
        .uiName("AI System prompt")
        .description("What the bots instruction is")
        .defaultValue("""
          Extract the text from the CAPTCHA image.
          Only respond with the text exactly like in the image.
          Do not write anything except the text.""")
        .type(StringSetting.InputType.TEXTAREA)
        .build();
    public static final StringProperty MODEL =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("model")
        .uiName("AI Model")
        .description("What AI model should be used for detecting the text in the CAPTCHA image")
        .defaultValue("llava")
        .build();
    public static final StringProperty RESPONSE_COMMAND =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("response-command")
        .uiName("Response Command")
        .description("What command should be ran using the response. Omit / to send a normal message")
        .defaultValue("%s")
        .build();
    public static final ComboProperty IMAGE_SOURCE =
      ImmutableComboProperty.builder()
        .namespace(NAMESPACE)
        .key("image-source")
        .uiName("Image Source")
        .description("Where should the captcha images be taken from")
        .defaultValue(ImageSource.MAP_IN_HAND.name())
        .addOptions(ComboProperty.optionsFromEnum(ImageSource.values(), ComboProperty::capitalizeEnum, e -> switch (e) {
          case MAP_IN_HAND -> "map";
        }))
        .build();
    public static final ComboProperty CAPTCHA_TRIGGER =
      ImmutableComboProperty.builder()
        .namespace(NAMESPACE)
        .key("captcha-trigger")
        .uiName("Captcha Trigger")
        .description("What triggers the captcha solver")
        .defaultValue(CaptchaTrigger.CHAT_MESSAGE.name())
        .addOptions(ComboProperty.optionsFromEnum(CaptchaTrigger.values(), ComboProperty::capitalizeEnum, e -> switch (e) {
          case CHAT_MESSAGE -> "message-circle";
          case TEXT_BASED -> "type";
        }))
        .build();
    public static final StringProperty TEXT_TRIGGER =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("text-trigger")
        .uiName("Text Trigger")
        .description("Text that must be contained in the message to trigger image-based captcha solving")
        .defaultValue("/captcha")
        .build();
    public static final StringProperty CAPTCHA_REGEX =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("captcha-regex")
        .uiName("Captcha Regex")
        .description("Regex pattern with a capturing group to extract the captcha text from the message")
        .defaultValue("/captcha (\\w+)")
        .build();

    enum ImageSource {
      MAP_IN_HAND
    }

    enum CaptchaTrigger {
      CHAT_MESSAGE,
      TEXT_BASED
    }
  }
}
