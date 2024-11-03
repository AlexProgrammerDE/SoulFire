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
package com.soulfiremc.generator.generators;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.struct.DirectoryContextSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PacketsGenerator {
  private static final Set<Class<? extends Packet<?>>> PACKETS = Set.of(
    net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class,
    net.minecraft.network.protocol.common.ClientboundDisconnectPacket.class,
    net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket.class,
    net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket.class,
    net.minecraft.network.protocol.common.ServerboundResourcePackPacket.class,
    net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket.class,
    net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket.class,
    net.minecraft.network.protocol.game.ClientboundCooldownPacket.class,
    net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket.class,
    net.minecraft.network.protocol.game.ClientboundLoginPacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerChatPacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.class,
    net.minecraft.network.protocol.game.ClientboundRespawnPacket.class,
    net.minecraft.network.protocol.game.ClientboundServerDataPacket.class,
    net.minecraft.network.protocol.game.ClientboundSystemChatPacket.class,
    net.minecraft.network.protocol.game.ClientboundTabListPacket.class,
    net.minecraft.network.protocol.game.ClientboundEntityEventPacket.class,
    net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.class,
    net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket.class,
    net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket.class,
    net.minecraft.network.protocol.game.ClientboundRotateHeadPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class,
    net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket.class,
    net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.class,
    net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket.class,
    net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket.class,
    net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetExperiencePacket.class,
    net.minecraft.network.protocol.game.ClientboundSetHealthPacket.class,
    net.minecraft.network.protocol.game.ClientboundAddEntityPacket.class,
    net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket.class,
    net.minecraft.network.protocol.game.ClientboundContainerClosePacket.class,
    net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket.class,
    net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket.class,
    net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket.class,
    net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket.class,
    net.minecraft.network.protocol.game.ClientboundOpenBookPacket.class,
    net.minecraft.network.protocol.game.ClientboundOpenScreenPacket.class,
    net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket.class,
    net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket.class,
    net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket.class,
    net.minecraft.network.protocol.game.ClientboundGameEventPacket.class,
    net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket.class,
    net.minecraft.network.protocol.game.ClientboundMapItemDataPacket.class,
    net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket.class,
    net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket.class,
    net.minecraft.network.protocol.game.ClientboundSetTimePacket.class,
    net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket.class,
    net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket.class,
    net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket.class,
    net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket.class,
    net.minecraft.network.protocol.game.ServerboundClientCommandPacket.class,
    net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket.class,
    net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.class,
    net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket.class,
    net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket.class,
    net.minecraft.network.protocol.common.ClientboundKeepAlivePacket.class,
    net.minecraft.network.protocol.common.ClientboundPingPacket.class,
    net.minecraft.network.protocol.common.ServerboundKeepAlivePacket.class,
    net.minecraft.network.protocol.common.ServerboundPongPacket.class,
    net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket.class,
    net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks.class,
    net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.class,
    net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks.class,
    net.minecraft.network.protocol.handshake.ClientIntentionPacket.class,
    net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket.class,
    net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket.class,
    net.minecraft.network.protocol.login.ClientboundHelloPacket.class,
    net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket.class,
    net.minecraft.network.protocol.login.ServerboundHelloPacket.class,
    net.minecraft.network.protocol.login.ServerboundKeyPacket.class,
    net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket.class,
    net.minecraft.network.protocol.ping.ClientboundPongResponsePacket.class,
    net.minecraft.network.protocol.status.ClientboundStatusResponsePacket.class,
    net.minecraft.network.protocol.ping.ServerboundPingRequestPacket.class,
    net.minecraft.network.protocol.status.ServerboundStatusRequestPacket.class
  );

  @SneakyThrows
  public static void generatePackets(Path outDir) {
    log.info("Dumping .class data");
    var classDir = outDir.resolve("classes");
    Files.createDirectories(classDir);
    for (var packet : PACKETS) {
      var classesToWrite = new ArrayList<Class<?>>();
      classesToWrite.add(packet);
      classesToWrite.addAll(Arrays.asList(packet.getDeclaredClasses()));

      for (var classToWrite : classesToWrite) {
        var packetBytes = Objects.requireNonNull(PacketsGenerator.class.getClassLoader().getResourceAsStream(classToWrite.getName().replace('.', '/') + ".class")).readAllBytes();
        var fileName = classToWrite.getName().replace('.', '/') + ".class";
        var filePath = classDir.resolve(fileName);
        Files.createDirectories(filePath.getParent());

        Files.write(filePath, packetBytes);
      }
    }

    log.info("Decompiling .class data");
    var sourcesDir = outDir.resolve("sources");
    Files.createDirectories(sourcesDir);
    Decompiler.builder()
      .libraries(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(Paths::get).map(Path::toFile).toArray(File[]::new))
      .inputs(new DirectoryContextSource(null, classDir.toFile()))
      .output(new DirectoryResultSaver(sourcesDir.toFile()))
      .build()
      .decompile();

    log.info("Processing decompiled sources");
    var finalDir = outDir.resolve("final");
    Files.createDirectories(finalDir);
    Files.walk(sourcesDir)
      .filter(Files::isRegularFile)
      .forEach(p -> processPacket(finalDir, p));

    log.info("Done generating packets");
  }

  @SneakyThrows
  private static void processPacket(Path finalDir, Path path) {
    StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_18);
    var className = path.getFileName().toString().replace(".java", "");
    log.debug("Processing packet {}", className);

    var unit = StaticJavaParser.parse(path);
    var packageName = unit.getPackageDeclaration().map(p -> p.getName().asString()).orElseThrow();
    var mainClass = unit.getTypes().stream().filter(type -> type.getNameAsString().equals(className)).findFirst().map(t -> (Node) t).orElseThrow();

    // Find all static int fields and create a map for int -> field name
    var fields = mainClass.findAll(FieldDeclaration.class).stream()
      .filter(field -> field.isStatic() && field.isFinal() && field.getElementType().isPrimitiveType() && field.getElementType().asPrimitiveType().getType().asString().equals("int"))
      .collect(Collectors.toMap(field -> Integer.parseInt(field.getVariable(0).getInitializer().orElseThrow().toString()), field -> field.getVariable(0).getNameAsString()));

    log.debug("Found fields: {}", fields);

    // Reverse constant folding by replacing the uses of the int constants with the field names
    unit.findAll(Node.class).forEach(node -> {
      // Skip if it's the field declaration itself
      if (node.getParentNode().map(parent -> parent instanceof VariableDeclarator).orElse(false)) {
        return;
      }

      if (node instanceof IntegerLiteralExpr integerLiteralExpr) {
        var number = (Integer) integerLiteralExpr.asNumber();
        if (!fields.containsKey(number)) {
          return;
        }

        node.replace(StaticJavaParser.parseExpression(fields.get(number)));
        log.debug("Replaced integer literal {} with {}", integerLiteralExpr, fields.get(number));
      } else if (node instanceof UnaryExpr unaryExpr && unaryExpr.getOperator() == UnaryExpr.Operator.MINUS && isInteger(unaryExpr.toString())) {
        var number = Integer.parseInt(unaryExpr.toString());
        if (!fields.containsKey(number)) {
          return;
        }

        node.replace(StaticJavaParser.parseExpression("-" + fields.get(number)));
        log.debug("Replaced unary {} with {}", unaryExpr, fields.get(number));
      }
    });

    // Delete all methods called "handle" and "type" as they are not needed
    mainClass.findAll(MethodDeclaration.class).forEach(method -> {
      if (method.getNameAsString().equals("handle") || method.getNameAsString().equals("type")) {
        method.remove();
        log.debug("Removed method {}", method.getNameAsString());
      }
    });

    // Write back the file
    packageName = packageName.replace("net.minecraft.network.protocol", "com.soulfiremc.protocol.packets");
    var finalPackage = finalDir.resolve(packageName.replace(".", "/"));
    Files.createDirectories(finalPackage);

    var finalPath = finalPackage.resolve(path.getFileName());

    var printerConfiguration = new DefaultPrinterConfiguration();
    printerConfiguration.addOption(new DefaultConfigurationOption(DefaultPrinterConfiguration.ConfigOption.SPACE_AROUND_OPERATORS, false));
    var printer = new DefaultPrettyPrinter(printerConfiguration);
    var javaFileData = printer.print(unit);

    javaFileData = javaFileData.replace("net.minecraft.network.protocol", "com.soulfiremc.protocol.packets");
    javaFileData = javaFileData.replace("net.minecraft", "com.soulfiremc");
    javaFileData = javaFileData.replace("RegistryFriendlyByteBuf", "FriendlyByteBuf");
    javaFileData = javaFileData.replace("<ClientGamePacketListener>", "");

    Files.writeString(finalPath, javaFileData);
  }

  private static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
