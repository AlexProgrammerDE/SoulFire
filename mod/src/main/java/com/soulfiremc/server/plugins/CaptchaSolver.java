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
package com.soulfiremc.server.plugins;

import com.google.gson.JsonParser;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;
import com.soulfiremc.grpc.generated.StringSetting;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.InternalPluginClass;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.ControllingTask;
import com.soulfiremc.server.renderer.RenderConstants;
import com.soulfiremc.server.renderer.SoftwareRenderer;
import com.soulfiremc.server.settings.instance.AISettings;
import com.soulfiremc.server.settings.lib.BotSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.util.MouseClickHelper;
import com.soulfiremc.server.util.SFHelpers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@InternalPluginClass
public final class CaptchaSolver extends InternalPlugin {
  private static final String MARK_PASSED_TOOL = "mark_passed";

  @SuppressWarnings("unchecked")
  private static final MetadataKey<List<String>> CHAT_HISTORY =
    MetadataKey.of("captcha_solver", "chat_history", (Class<List<String>>) (Class<?>) List.class);

  public CaptchaSolver() {
    super(new PluginInfo(
      "captcha-solver",
      "1.0.0",
      "Solve captchas using AI agent with tool calling",
      "AlexProgrammerDE",
      "AGPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  // === Image Helpers ===

  private static String toBase64PNG(BufferedImage image) {
    try (var os = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", os);
      return Base64.getEncoder().encodeToString(os.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to convert image to base64", e);
    }
  }

  private static String renderPov(BotConnection connection) {
    var settingsSource = connection.settingsSource();
    var player = connection.minecraft().player;
    var level = connection.minecraft().level;
    if (player == null || level == null) {
      throw new IllegalStateException("Player or level is null");
    }

    var width = settingsSource.get(CaptchaSolverSettings.POV_RENDER_WIDTH);
    var height = settingsSource.get(CaptchaSolverSettings.POV_RENDER_HEIGHT);
    var renderDistanceChunks = connection.minecraft().options.getEffectiveRenderDistance();
    var maxDistance = renderDistanceChunks * 16;

    var image = SoftwareRenderer.render(
      level, player, width, height, RenderConstants.DEFAULT_FOV, maxDistance
    );
    return toBase64PNG(image);
  }

  private static Optional<String> getMapImage(BotConnection connection) {
    var player = connection.minecraft().player;
    var level = connection.minecraft().level;
    if (player == null || level == null) {
      return Optional.empty();
    }

    var item = player.getItemInHand(InteractionHand.MAIN_HAND);
    var mapId = item.get(DataComponents.MAP_ID);
    if (mapId == null) {
      return Optional.empty();
    }

    var mapState = level.getMapData(mapId);
    if (mapState == null) {
      return Optional.empty();
    }

    return Optional.of(toBase64PNG(SFHelpers.toBufferedImage(mapState)));
  }

  // === Context Building ===

  private static String buildInventoryContext(BotConnection connection) {
    var player = connection.minecraft().player;
    if (player == null) {
      return "Inventory: unavailable";
    }

    var sb = new StringBuilder("Inventory:\n");
    var container = player.inventoryMenu;
    for (var slot : container.slots) {
      var item = slot.getItem();
      if (!item.isEmpty()) {
        var name = item.getItemHolder().getRegisteredName();
        var count = item.getCount();
        var displayName = item.has(DataComponents.CUSTOM_NAME)
          ? " (%s)".formatted(item.getHoverName().getString())
          : "";
        sb.append("  Slot %d: %dx %s%s\n".formatted(slot.index, count, name, displayName));
      }
    }
    sb.append("Selected hotbar slot: %d\n".formatted(player.getInventory().getSelectedSlot()));
    return sb.toString();
  }

  private static String buildStatusContext(BotConnection connection) {
    var player = connection.minecraft().player;
    if (player == null) {
      return "Status: unavailable";
    }

    return """
      Status:
        Position: %.1f, %.1f, %.1f
        Health: %.1f/%.1f
        Food: %d
        Yaw: %.1f, Pitch: %.1f
      """.formatted(
      player.getX(), player.getY(), player.getZ(),
      player.getHealth(), player.getMaxHealth(),
      player.getFoodData().getFoodLevel(),
      player.getYRot(), player.getXRot()
    );
  }

  private static ChatCompletionMessageParam buildContextMessage(BotConnection connection) {
    var parts = new ArrayList<ChatCompletionContentPart>();

    // Text context: chat history + inventory + status
    var textContext = new StringBuilder();

    // Chat history
    var history = connection.metadata().getOrSet(CHAT_HISTORY, () -> Collections.synchronizedList(new ArrayList<>()));
    List<String> chatSnapshot;
    synchronized (history) {
      chatSnapshot = new ArrayList<>(history);
    }
    if (!chatSnapshot.isEmpty()) {
      textContext.append("Recent chat messages:\n");
      for (var msg : chatSnapshot) {
        textContext.append("  ").append(msg).append("\n");
      }
    } else {
      textContext.append("No recent chat messages.\n");
    }
    textContext.append("\n");

    // Inventory
    textContext.append(buildInventoryContext(connection));
    textContext.append("\n");

    // Status
    textContext.append(buildStatusContext(connection));

    parts.add(ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
      .text(textContext.toString())
      .build()));

    // POV image
    try {
      var povBase64 = renderPov(connection);
      parts.add(ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
          .url("data:image/png;base64,%s".formatted(povBase64))
          .build())
        .build()));
    } catch (Exception e) {
      log.warn("Failed to render POV for captcha solver", e);
    }

    // Map image (if holding one)
    try {
      getMapImage(connection).ifPresent(mapBase64 ->
        parts.add(ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
          .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
            .url("data:image/png;base64,%s".formatted(mapBase64))
            .build())
          .build())));
    } catch (Exception e) {
      log.warn("Failed to get map image for captcha solver", e);
    }

    return ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
      .content(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(parts))
      .build());
  }

  // === Tool Definitions ===

  private static ChatCompletionTool makeTool(String name, String description, Object properties, List<String> required) {
    var paramsBuilder = FunctionParameters.builder()
      .putAdditionalProperty("type", JsonValue.from("object"))
      .putAdditionalProperty("properties", JsonValue.from(properties));
    if (!required.isEmpty()) {
      paramsBuilder.putAdditionalProperty("required", JsonValue.from(required));
    }

    return ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder()
      .function(FunctionDefinition.builder()
        .name(name)
        .description(description)
        .parameters(paramsBuilder.build())
        .build())
      .build());
  }

  private static List<ChatCompletionTool> buildEnabledTools(BotSettingsSource settings) {
    var tools = new ArrayList<ChatCompletionTool>();

    if (settings.get(CaptchaSolverSettings.TOOL_SEND_CHAT_MESSAGE)) {
      tools.add(makeTool("send_chat_message",
        "Send a chat message or command in Minecraft",
        Map.<String, Object>of("message", Map.of("type", "string", "description", "The message to send")),
        List.of("message")));
    }

    if (settings.get(CaptchaSolverSettings.TOOL_SET_MOVEMENT)) {
      tools.add(makeTool("set_movement",
        "Set movement key states. Only specified keys are changed, others keep their current state.",
        Map.<String, Object>of(
          "forward", Map.of("type", "boolean", "description", "W key (move forward)"),
          "backward", Map.of("type", "boolean", "description", "S key (move backward)"),
          "left", Map.of("type", "boolean", "description", "A key (strafe left)"),
          "right", Map.of("type", "boolean", "description", "D key (strafe right)"),
          "jump", Map.of("type", "boolean", "description", "Space key (jump)"),
          "sneak", Map.of("type", "boolean", "description", "Shift key (sneak)"),
          "sprint", Map.of("type", "boolean", "description", "Ctrl key (sprint)")
        ),
        List.of()));
    }

    if (settings.get(CaptchaSolverSettings.TOOL_STOP_MOVEMENT)) {
      tools.add(makeTool("stop_movement",
        "Stop all movement by releasing all movement keys",
        Map.of(),
        List.of()));
    }

    if (settings.get(CaptchaSolverSettings.TOOL_SET_ROTATION)) {
      tools.add(makeTool("set_rotation",
        "Set the bot's view direction. Yaw: 0=South, 90=West, -90=East, 180=North. Pitch: -90=up, 0=horizon, 90=down.",
        Map.<String, Object>of(
          "yaw", Map.of("type", "number", "description", "Horizontal rotation (-180 to 180)"),
          "pitch", Map.of("type", "number", "description", "Vertical rotation (-90 to 90)")
        ),
        List.of("yaw", "pitch")));
    }

    if (settings.get(CaptchaSolverSettings.TOOL_CLICK_INVENTORY_SLOT)) {
      tools.add(makeTool("click_inventory_slot",
        "Click an inventory slot. Slot indices: 0-4 crafting, 5-8 armor, 9-35 main inventory, 36-44 hotbar, 45 offhand.",
        Map.<String, Object>of(
          "slot", Map.of("type", "integer", "description", "The slot index to click"),
          "click_type", Map.<String, Object>of("type", "string", "description", "Click type: 'left' (default), 'right', or 'shift_left'")
        ),
        List.of("slot")));
    }

    if (settings.get(CaptchaSolverSettings.TOOL_SELECT_HOTBAR_SLOT)) {
      tools.add(makeTool("select_hotbar_slot",
        "Select a hotbar slot (0-8)",
        Map.<String, Object>of("slot", Map.of("type", "integer", "description", "Hotbar slot index (0-8)")),
        List.of("slot")));
    }

    if (settings.get(CaptchaSolverSettings.TOOL_MOUSE_CLICK)) {
      tools.add(makeTool("mouse_click",
        "Simulate a mouse click in the game world. Left click attacks/breaks, right click uses/interacts.",
        Map.<String, Object>of("button", Map.<String, Object>of("type", "string", "description", "Mouse button: 'left' or 'right'")),
        List.of("button")));
    }

    // mark_passed is always available
    tools.add(makeTool(MARK_PASSED_TOOL,
      "Call this when the captcha has been successfully solved/passed",
      Map.<String, Object>of("reason", Map.of("type", "string", "description", "Why you believe the captcha is passed")),
      List.of()));

    return tools;
  }

  // === Tool Execution ===

  private static String executeTool(BotConnection connection, String name, String argsJson) {
    var args = JsonParser.parseString(argsJson).getAsJsonObject();

    return switch (name) {
      case "send_chat_message" -> {
        var message = args.get("message").getAsString();
        connection.sendChatMessage(message);
        yield "Message sent: " + message;
      }
      case "set_movement" -> {
        var state = connection.controlState();
        if (args.has("forward")) state.up(args.get("forward").getAsBoolean());
        if (args.has("backward")) state.down(args.get("backward").getAsBoolean());
        if (args.has("left")) state.left(args.get("left").getAsBoolean());
        if (args.has("right")) state.right(args.get("right").getAsBoolean());
        if (args.has("jump")) state.jump(args.get("jump").getAsBoolean());
        if (args.has("sneak")) state.shift(args.get("sneak").getAsBoolean());
        if (args.has("sprint")) state.sprint(args.get("sprint").getAsBoolean());
        yield "Movement state updated";
      }
      case "stop_movement" -> {
        connection.controlState().resetAll();
        yield "All movement stopped";
      }
      case "set_rotation" -> {
        var yaw = args.get("yaw").getAsFloat();
        var pitch = Math.clamp(args.get("pitch").getAsFloat(), -90f, 90f);
        connection.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
          var player = connection.minecraft().player;
          if (player != null) {
            player.setYRot(yaw);
            player.setXRot(pitch);
          }
        }));
        yield "Rotation set to yaw=%.1f, pitch=%.1f".formatted(yaw, pitch);
      }
      case "click_inventory_slot" -> {
        var slot = args.get("slot").getAsInt();
        var clickTypeStr = args.has("click_type") ? args.get("click_type").getAsString() : "left";
        int mouseButton;
        ClickType clickType;
        switch (clickTypeStr) {
          case "right" -> {
            mouseButton = 1;
            clickType = ClickType.PICKUP;
          }
          case "shift_left" -> {
            mouseButton = 0;
            clickType = ClickType.QUICK_MOVE;
          }
          default -> {
            mouseButton = 0;
            clickType = ClickType.PICKUP;
          }
        }
        var finalMouseButton = mouseButton;
        var finalClickType = clickType;
        connection.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
          var player = connection.minecraft().player;
          var gameMode = connection.minecraft().gameMode;
          if (player != null && gameMode != null) {
            gameMode.handleInventoryMouseClick(
              player.containerMenu.containerId, slot, finalMouseButton, finalClickType, player
            );
          }
        }));
        yield "Inventory slot %d clicked (%s)".formatted(slot, clickTypeStr);
      }
      case "select_hotbar_slot" -> {
        var slot = Math.clamp(args.get("slot").getAsInt(), 0, 8);
        connection.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
          var player = connection.minecraft().player;
          if (player != null) {
            player.getInventory().setSelectedSlot(slot);
          }
        }));
        yield "Hotbar slot %d selected".formatted(slot);
      }
      case "mouse_click" -> {
        var button = args.get("button").getAsString();
        connection.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
          var player = connection.minecraft().player;
          var level = connection.minecraft().level;
          var gameMode = connection.minecraft().gameMode;
          if (player != null && level != null && gameMode != null) {
            if ("left".equals(button)) {
              MouseClickHelper.performLeftClick(player, level, gameMode);
            } else {
              MouseClickHelper.performRightClick(player, level, gameMode);
            }
          }
        }));
        yield "Mouse %s click performed".formatted(button);
      }
      case MARK_PASSED_TOOL -> {
        var reason = args.has("reason") ? args.get("reason").getAsString() : "no reason given";
        yield "Captcha marked as passed: " + reason;
      }
      default -> "Unknown tool: " + name;
    };
  }

  // === Agent Loop ===

  private static void runAgentLoop(BotConnection connection) {
    var settingsSource = connection.settingsSource();
    var api = AISettings.create(settingsSource);
    var model = settingsSource.get(CaptchaSolverSettings.MODEL);
    var maxIterations = settingsSource.get(CaptchaSolverSettings.MAX_ITERATIONS);
    var tools = buildEnabledTools(settingsSource);

    var paramsBuilder = ChatCompletionCreateParams.builder()
      .model(model)
      .maxCompletionTokens(1024)
      .addMessage(ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
        .content(ChatCompletionSystemMessageParam.Content.ofText(settingsSource.get(CaptchaSolverSettings.PROMPT)))
        .build()));

    for (var tool : tools) {
      paramsBuilder.addTool(tool);
    }

    // Add initial context
    paramsBuilder.addMessage(buildContextMessage(connection));

    try {
      for (var i = 0; i < maxIterations; i++) {
        log.debug("Captcha agent iteration {}/{}", i + 1, maxIterations);

        var completion = api.chat().completions().create(paramsBuilder.build());
        var choice = completion.choices().stream().findFirst().orElse(null);
        if (choice == null) {
          log.warn("No choices returned from AI");
          break;
        }

        var message = choice.message();

        // Add assistant message to conversation
        paramsBuilder.addMessage(message);

        // Log any text response
        message.content().ifPresent(content -> {
          if (!content.isBlank()) {
            log.debug("AI text response: {}", content);
          }
        });

        // Check for tool calls
        var toolCalls = message.toolCalls();
        if (toolCalls.isEmpty() || toolCalls.get().isEmpty()) {
          log.debug("No tool calls, ending agent loop");
          break;
        }

        // Execute tools
        var passed = false;
        for (var toolCall : toolCalls.get()) {
          var fnToolCall = toolCall.asFunction();
          var functionName = fnToolCall.function().name();
          var arguments = fnToolCall.function().arguments();
          log.debug("Executing tool: {}({})", functionName, arguments);

          try {
            var result = executeTool(connection, functionName, arguments);
            log.debug("Tool result: {}", result);

            paramsBuilder.addMessage(ChatCompletionToolMessageParam.builder()
              .toolCallId(fnToolCall.id())
              .content(result)
              .build());

            if (MARK_PASSED_TOOL.equals(functionName)) {
              passed = true;
            }
          } catch (Exception e) {
            log.error("Tool execution failed: {}", functionName, e);
            paramsBuilder.addMessage(ChatCompletionToolMessageParam.builder()
              .toolCallId(fnToolCall.id())
              .content("Error: " + e.getMessage())
              .build());
          }
        }

        if (passed) {
          log.info("Captcha marked as passed by AI agent");
          break;
        }

        // Small delay to let tick-based actions execute
        Thread.sleep(200);

        // Add fresh context for next iteration
        try {
          paramsBuilder.addMessage(buildContextMessage(connection));
        } catch (Exception e) {
          log.warn("Failed to build context for next iteration", e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.debug("Captcha agent loop interrupted");
    } catch (Exception e) {
      log.error("Captcha agent loop failed", e);
    } finally {
      // Cleanup: stop all movement
      connection.controlState().resetAll();
      log.debug("Captcha agent loop finished");
    }
  }

  // === Text-Based Handler (non-AI) ===

  private static void handleTextInput(BotConnection connection, String captchaText) {
    var settingsSource = connection.settingsSource();
    var response = settingsSource.get(CaptchaSolverSettings.RESPONSE_COMMAND).formatted(captchaText);
    log.debug("Extracted captcha text: {}", captchaText);
    connection.sendChatMessage(response);
  }

  // === Event Handlers ===

  @EventHandler
  public static void onMessage(ChatMessageReceiveEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    if (!settingsSource.get(CaptchaSolverSettings.ENABLED)) {
      return;
    }

    // Always update chat history
    var maxHistoryLength = settingsSource.get(CaptchaSolverSettings.CHAT_HISTORY_LENGTH);
    var history = connection.metadata().getOrSet(CHAT_HISTORY, () -> Collections.synchronizedList(new ArrayList<>()));
    synchronized (history) {
      history.add(event.parseToPlainText());
      while (history.size() > maxHistoryLength) {
        history.removeFirst();
      }
    }

    var trigger = settingsSource.get(CaptchaSolverSettings.CAPTCHA_TRIGGER, CaptchaSolverSettings.CaptchaTrigger.class);

    connection.scheduler().execute(() -> {
      try {
        var plainMessage = event.parseToPlainText();

        if (trigger == CaptchaSolverSettings.CaptchaTrigger.CHAT_MESSAGE) {
          var textTrigger = settingsSource.get(CaptchaSolverSettings.TEXT_TRIGGER);
          if (plainMessage.contains(textTrigger)) {
            runAgentLoop(connection);
          }
        } else if (trigger == CaptchaSolverSettings.CaptchaTrigger.TEXT_BASED) {
          var regex = settingsSource.get(CaptchaSolverSettings.CAPTCHA_REGEX);
          var pattern = Pattern.compile(regex);
          var matcher = pattern.matcher(plainMessage);
          if (matcher.find() && matcher.groupCount() >= 1) {
            var captchaText = matcher.group(1);
            handleTextInput(connection, captchaText);
          }
        }
      } catch (Exception e) {
        log.error("Failed to handle captcha", e);
      }
    });
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsPageRegistry().addPluginPage(CaptchaSolverSettings.class, "captcha-solver", "Captcha Solver", this, "eye", CaptchaSolverSettings.ENABLED);
  }

  // === Settings ===

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class CaptchaSolverSettings implements SettingsObject {
    private static final String NAMESPACE = "captcha-solver";

    public static final BooleanProperty<SettingsSource.Bot> ENABLED =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Captcha Solver")
        .description("Enable the Captcha Solver")
        .defaultValue(false)
        .build();

    public static final StringProperty<SettingsSource.Bot> PROMPT =
      ImmutableStringProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("prompt")
        .uiName("AI System prompt")
        .description("System prompt for the AI agent")
        .defaultValue("""
          You are a Minecraft bot solving a captcha challenge.
          You can see the game through the POV image and map image (if holding one).
          You have access to tools to interact with the game world.
          Analyze the situation, take actions to solve the captcha, and call mark_passed when done.
          Keep actions minimal and purposeful.""")
        .type(StringSetting.InputType.TEXTAREA)
        .build();

    public static final StringProperty<SettingsSource.Bot> MODEL =
      ImmutableStringProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("model")
        .uiName("AI Model")
        .description("AI model to use (must support vision and tool calling)")
        .defaultValue("gpt-4o")
        .build();

    public static final StringProperty<SettingsSource.Bot> RESPONSE_COMMAND =
      ImmutableStringProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("response-command")
        .uiName("Response Command")
        .description("Response format for text-based mode. Omit / to send a normal message")
        .defaultValue("%s")
        .build();

    public static final ComboProperty<SettingsSource.Bot> CAPTCHA_TRIGGER =
      ImmutableComboProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
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

    public static final StringProperty<SettingsSource.Bot> TEXT_TRIGGER =
      ImmutableStringProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("text-trigger")
        .uiName("Text Trigger")
        .description("Text that must be in the message to trigger AI captcha solving")
        .defaultValue("/captcha")
        .build();

    public static final StringProperty<SettingsSource.Bot> CAPTCHA_REGEX =
      ImmutableStringProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("captcha-regex")
        .uiName("Captcha Regex")
        .description("Regex with capturing group for text-based captcha extraction")
        .defaultValue("/captcha (\\w+)")
        .build();

    public static final IntProperty<SettingsSource.Bot> POV_RENDER_WIDTH =
      ImmutableIntProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("pov-render-width")
        .uiName("POV Render Width")
        .description("Width of the POV render image in pixels")
        .defaultValue(RenderConstants.DEFAULT_WIDTH)
        .minValue(1)
        .maxValue(3840)
        .stepValue(1)
        .build();

    public static final IntProperty<SettingsSource.Bot> POV_RENDER_HEIGHT =
      ImmutableIntProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("pov-render-height")
        .uiName("POV Render Height")
        .description("Height of the POV render image in pixels")
        .defaultValue(RenderConstants.DEFAULT_HEIGHT)
        .minValue(1)
        .maxValue(2160)
        .stepValue(1)
        .build();

    public static final IntProperty<SettingsSource.Bot> MAX_ITERATIONS =
      ImmutableIntProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("max-iterations")
        .uiName("Max Iterations")
        .description("Maximum number of agent loop iterations before giving up")
        .defaultValue(20)
        .minValue(1)
        .maxValue(100)
        .stepValue(1)
        .build();

    public static final IntProperty<SettingsSource.Bot> CHAT_HISTORY_LENGTH =
      ImmutableIntProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("chat-history-length")
        .uiName("Chat History Length")
        .description("Number of recent chat messages to include as context")
        .defaultValue(20)
        .minValue(0)
        .maxValue(100)
        .stepValue(1)
        .build();

    // Tool toggles
    public static final BooleanProperty<SettingsSource.Bot> TOOL_SEND_CHAT_MESSAGE =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-send-chat-message")
        .uiName("Tool: Send Chat Message")
        .description("Allow the AI to send chat messages")
        .defaultValue(true)
        .build();

    public static final BooleanProperty<SettingsSource.Bot> TOOL_SET_MOVEMENT =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-set-movement")
        .uiName("Tool: Set Movement")
        .description("Allow the AI to control movement keys")
        .defaultValue(true)
        .build();

    public static final BooleanProperty<SettingsSource.Bot> TOOL_STOP_MOVEMENT =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-stop-movement")
        .uiName("Tool: Stop Movement")
        .description("Allow the AI to stop all movement")
        .defaultValue(true)
        .build();

    public static final BooleanProperty<SettingsSource.Bot> TOOL_SET_ROTATION =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-set-rotation")
        .uiName("Tool: Set Rotation")
        .description("Allow the AI to change view direction")
        .defaultValue(true)
        .build();

    public static final BooleanProperty<SettingsSource.Bot> TOOL_CLICK_INVENTORY_SLOT =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-click-inventory-slot")
        .uiName("Tool: Click Inventory Slot")
        .description("Allow the AI to click inventory slots")
        .defaultValue(true)
        .build();

    public static final BooleanProperty<SettingsSource.Bot> TOOL_SELECT_HOTBAR_SLOT =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-select-hotbar-slot")
        .uiName("Tool: Select Hotbar Slot")
        .description("Allow the AI to change hotbar selection")
        .defaultValue(true)
        .build();

    public static final BooleanProperty<SettingsSource.Bot> TOOL_MOUSE_CLICK =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("tool-mouse-click")
        .uiName("Tool: Mouse Click")
        .description("Allow the AI to perform mouse clicks in the game world")
        .defaultValue(true)
        .build();

    enum CaptchaTrigger {
      CHAT_MESSAGE,
      TEXT_BASED
    }
  }
}
