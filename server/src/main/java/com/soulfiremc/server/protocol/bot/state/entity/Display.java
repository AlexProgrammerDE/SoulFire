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
package com.soulfiremc.server.protocol.bot.state.entity;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.NamedEntityData;
import com.soulfiremc.server.protocol.bot.block.GlobalBlockPalette;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.EntityMetadataState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.JOMLUtil;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.mcstructs.*;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;

public abstract class Display extends Entity {
  private static final IntSet RENDER_STATE_IDS = IntSet.of(
    NamedEntityData.DISPLAY__TRANSLATION.networkId(),
    NamedEntityData.DISPLAY__SCALE.networkId(),
    NamedEntityData.DISPLAY__LEFT_ROTATION.networkId(),
    NamedEntityData.DISPLAY__RIGHT_ROTATION.networkId(),
    NamedEntityData.DISPLAY__BILLBOARD_RENDER_CONSTRAINTS.networkId(),
    NamedEntityData.DISPLAY__BRIGHTNESS_OVERRIDE.networkId(),
    NamedEntityData.DISPLAY__SHADOW_RADIUS.networkId(),
    NamedEntityData.DISPLAY__SHADOW_STRENGTH.networkId()
  );
  protected boolean updateRenderState;
  private long interpolationStartClientTick = -2147483648L;
  private int interpolationDuration;
  private float lastProgress;
  private AABB cullingBoundingBox;
  private boolean noCulling = true;
  private boolean updateStartTick;
  private boolean updateInterpolationDuration;
  private Display.RenderState renderState;
  private Display.PosRotInterpolationTarget posRotInterpolationTarget;

  public Display(EntityType type, Level level) {
    super(type, level);
    this.noPhysics = true;
    this.cullingBoundingBox = this.getBoundingBox();
  }

  private static Transformation createTransformation(EntityMetadataState synchedEntityData) {
    var translation = JOMLUtil.fromCloudburst(synchedEntityData.get(NamedEntityData.DISPLAY__TRANSLATION, MetadataTypes.VECTOR3));
    var leftRotation = JOMLUtil.toQuaternion(synchedEntityData.get(NamedEntityData.DISPLAY__LEFT_ROTATION, MetadataTypes.QUATERNION));
    var scale = JOMLUtil.fromCloudburst(synchedEntityData.get(NamedEntityData.DISPLAY__SCALE, MetadataTypes.VECTOR3));
    var rightRotation = JOMLUtil.toQuaternion(synchedEntityData.get(NamedEntityData.DISPLAY__RIGHT_ROTATION, MetadataTypes.QUATERNION));
    return new Transformation(translation, leftRotation, scale, rightRotation);
  }

  public Vector3d getTransformedPosition() {
    if (this.renderState == null) {
      return Vector3d.ZERO;
    }

    var transformation = renderState.transformation().get(1.0f).getTranslation();
    return Vector3d.from(
      transformation.x,
      transformation.y,
      transformation.z
    );
  }

  @Override
  public void onSyncedDataUpdated(NamedEntityData entityData) {
    super.onSyncedDataUpdated(entityData);
    if (NamedEntityData.DISPLAY__HEIGHT.equals(entityData) || NamedEntityData.DISPLAY__WIDTH.equals(entityData)) {
      this.updateCulling();
    }

    if (NamedEntityData.DISPLAY__TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS.equals(entityData)) {
      this.updateStartTick = true;
    }

    if (NamedEntityData.DISPLAY__TRANSFORMATION_INTERPOLATION_DURATION.equals(entityData)) {
      this.updateInterpolationDuration = true;
    }

    if (RENDER_STATE_IDS.contains(entityData.networkId())) {
      this.updateRenderState = true;
    }
  }

  @Override
  public void tick() {
    if (this.updateStartTick) {
      this.updateStartTick = false;
      var i = this.getTransformationInterpolationDelay();
      this.interpolationStartClientTick = this.tickCount + i;
    }

    if (this.updateInterpolationDuration) {
      this.updateInterpolationDuration = false;
      this.interpolationDuration = this.getTransformationInterpolationDuration();
    }

    if (this.updateRenderState) {
      this.updateRenderState = false;
      var interpolate = this.interpolationDuration != 0;
      if (interpolate && this.renderState != null) {
        this.renderState = this.createInterpolatedRenderState(this.renderState, this.lastProgress);
      } else {
        this.renderState = this.createFreshRenderState();
      }

      this.updateRenderSubState(interpolate, this.lastProgress);
    }

    if (this.posRotInterpolationTarget != null) {
      if (this.posRotInterpolationTarget.steps == 0) {
        this.posRotInterpolationTarget.applyTargetPosAndRot(this);
        this.setOldPosAndRot();
        this.posRotInterpolationTarget = null;
      } else {
        this.posRotInterpolationTarget.applyLerpStep(this);
        this.posRotInterpolationTarget.steps--;
        if (this.posRotInterpolationTarget.steps == 0) {
          this.posRotInterpolationTarget = null;
        }
      }
    }
  }

  protected abstract void updateRenderSubState(boolean interpolate, float partialTick);

  @Override
  public void cancelLerp() {
    this.posRotInterpolationTarget = null;
  }

  @Override
  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    var posSteps = this.getPosRotInterpolationDuration();
    this.posRotInterpolationTarget = new Display.PosRotInterpolationTarget(posSteps, x, y, z, yRot, xRot);
  }

  @Override
  public double lerpTargetX() {
    return this.posRotInterpolationTarget != null ? this.posRotInterpolationTarget.targetX : this.x();
  }

  @Override
  public double lerpTargetY() {
    return this.posRotInterpolationTarget != null ? this.posRotInterpolationTarget.targetY : this.y();
  }

  @Override
  public double lerpTargetZ() {
    return this.posRotInterpolationTarget != null ? this.posRotInterpolationTarget.targetZ : this.z();
  }

  @Override
  public float lerpTargetXRot() {
    return this.posRotInterpolationTarget != null ? (float) this.posRotInterpolationTarget.targetXRot : this.xRot();
  }

  @Override
  public float lerpTargetYRot() {
    return this.posRotInterpolationTarget != null ? (float) this.posRotInterpolationTarget.targetYRot : this.yRot();
  }

  public AABB getBoundingBoxForCulling() {
    return this.cullingBoundingBox;
  }

  public boolean affectedByCulling() {
    return !this.noCulling;
  }

  public Display.RenderState renderState() {
    return this.renderState;
  }

  private int getTransformationInterpolationDuration() {
    return this.entityData.get(NamedEntityData.DISPLAY__TRANSFORMATION_INTERPOLATION_DURATION, MetadataTypes.INT);
  }

  private int getTransformationInterpolationDelay() {
    return this.entityData.get(NamedEntityData.DISPLAY__TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS, MetadataTypes.INT);
  }

  private int getPosRotInterpolationDuration() {
    return this.entityData.get(NamedEntityData.DISPLAY__POS_ROT_INTERPOLATION_DURATION, MetadataTypes.INT);
  }

  private Display.BillboardConstraints getBillboardConstraints() {
    return Display.BillboardConstraints.fromId(this.entityData.get(NamedEntityData.DISPLAY__BILLBOARD_RENDER_CONSTRAINTS, MetadataTypes.BYTE));
  }

  @Nullable
  private Brightness getBrightnessOverride() {
    int i = this.entityData.get(NamedEntityData.DISPLAY__BRIGHTNESS_OVERRIDE, MetadataTypes.INT);
    return i != -1 ? Brightness.unpack(i) : null;
  }

  private int getPackedBrightnessOverride() {
    return this.entityData.get(NamedEntityData.DISPLAY__BRIGHTNESS_OVERRIDE, MetadataTypes.INT);
  }

  private float getViewRange() {
    return this.entityData.get(NamedEntityData.DISPLAY__VIEW_RANGE, MetadataTypes.FLOAT);
  }

  private float getShadowRadius() {
    return this.entityData.get(NamedEntityData.DISPLAY__SHADOW_RADIUS, MetadataTypes.FLOAT);
  }

  private float getShadowStrength() {
    return this.entityData.get(NamedEntityData.DISPLAY__SHADOW_STRENGTH, MetadataTypes.FLOAT);
  }

  private float getWidth() {
    return this.entityData.get(NamedEntityData.DISPLAY__WIDTH, MetadataTypes.FLOAT);
  }

  private int getGlowColorOverride() {
    return this.entityData.get(NamedEntityData.DISPLAY__GLOW_COLOR_OVERRIDE, MetadataTypes.INT);
  }

  public float calculateInterpolationProgress(float partialTick) {
    var i = this.interpolationDuration;
    if (i <= 0) {
      return 1.0F;
    } else {
      var interpolationProgress = (float) ((long) this.tickCount - this.interpolationStartClientTick);
      var withPartial = interpolationProgress + partialTick;
      var lerpedProgress = MathHelper.clamp(MathHelper.inverseLerp(withPartial, 0.0F, (float) i), 0.0F, 1.0F);
      this.lastProgress = lerpedProgress;
      return lerpedProgress;
    }
  }

  private float getHeight() {
    return this.entityData.get(NamedEntityData.DISPLAY__HEIGHT, MetadataTypes.FLOAT);
  }

  @Override
  public void setPos(double x, double y, double z) {
    super.setPos(x, y, z);
    this.updateCulling();
  }

  private void updateCulling() {
    var width = this.getWidth();
    var height = this.getHeight();
    this.noCulling = width == 0.0F || height == 0.0F;
    var halfWidth = width / 2.0F;
    var x = this.x();
    var y = this.y();
    var z = this.z();
    this.cullingBoundingBox = new AABB(x - (double) halfWidth, y, z - (double) halfWidth, x + (double) halfWidth, y + (double) height, z + (double) halfWidth);
  }

  @Override
  public int getTeamColor() {
    var i = this.getGlowColorOverride();
    return i != -1 ? i : super.getTeamColor();
  }

  private Display.RenderState createFreshRenderState() {
    return new Display.RenderState(
      Display.GenericInterpolator.constant(createTransformation(this.entityData)),
      this.getBillboardConstraints(),
      this.getPackedBrightnessOverride(),
      Display.FloatInterpolator.constant(this.getShadowRadius()),
      Display.FloatInterpolator.constant(this.getShadowStrength()),
      this.getGlowColorOverride()
    );
  }

  private Display.RenderState createInterpolatedRenderState(Display.RenderState renderState, float partialTick) {
    var transform = renderState.transformation.get(partialTick);
    var shadowRadius = renderState.shadowRadius.get(partialTick);
    var shadowStrength = renderState.shadowStrength.get(partialTick);
    return new Display.RenderState(
      new Display.TransformationInterpolator(transform, createTransformation(this.entityData)),
      this.getBillboardConstraints(),
      this.getPackedBrightnessOverride(),
      new Display.LinearFloatInterpolator(shadowRadius, this.getShadowRadius()),
      new Display.LinearFloatInterpolator(shadowStrength, this.getShadowStrength()),
      this.getGlowColorOverride()
    );
  }

  @RequiredArgsConstructor
  public enum BillboardConstraints {
    FIXED((byte) 0),
    VERTICAL((byte) 1),
    HORIZONTAL((byte) 2),
    CENTER((byte) 3);

    private final byte id;

    public static BillboardConstraints fromId(final byte id) {
      for (var constraints : values()) {
        if (constraints.id == id) {
          return constraints;
        }
      }

      throw new IllegalArgumentException("Unknown billboard constraints id: " + id);
    }
  }

  @FunctionalInterface
  public interface FloatInterpolator {
    static Display.FloatInterpolator constant(float f) {
      return g -> f;
    }

    float get(float partialTick);
  }

  @FunctionalInterface
  public interface GenericInterpolator<T> {
    static <T> Display.GenericInterpolator<T> constant(T object) {
      return f -> object;
    }

    T get(float partialTick);
  }

  @FunctionalInterface
  public interface IntInterpolator {
    static Display.IntInterpolator constant(int i) {
      return f -> i;
    }

    int get(float partialTick);
  }

  public static class BlockDisplay extends Display {
    private Display.BlockDisplay.BlockRenderState blockRenderState;

    public BlockDisplay(EntityType type, Level level) {
      super(type, level);
    }

    @Override
    public void onSyncedDataUpdated(NamedEntityData entityData) {
      super.onSyncedDataUpdated(entityData);
      if (entityData.equals(NamedEntityData.BLOCK_DISPLAY__BLOCK_STATE)) {
        this.updateRenderState = true;
      }
    }

    private BlockState getBlockState() {
      return GlobalBlockPalette.INSTANCE.getBlockStateForStateId(this.entityData.get(NamedEntityData.BLOCK_DISPLAY__BLOCK_STATE, MetadataTypes.BLOCK_STATE));
    }

    public Display.BlockDisplay.BlockRenderState blockRenderState() {
      return this.blockRenderState;
    }

    @Override
    protected void updateRenderSubState(boolean interpolate, float partialTick) {
      this.blockRenderState = new Display.BlockDisplay.BlockRenderState(this.getBlockState());
    }

    public record BlockRenderState(BlockState blockState) {
    }
  }

  record ColorInterpolator(int previous, int current) implements Display.IntInterpolator {
    @Override
    public int get(float partialTick) {
      return ARGB.lerp(partialTick, this.previous, this.current);
    }
  }

  public static class ItemDisplay extends Display {
    private Display.ItemDisplay.ItemRenderState itemRenderState;

    public ItemDisplay(EntityType type, Level level) {
      super(type, level);
    }

    @Override
    public void onSyncedDataUpdated(NamedEntityData entityData) {
      super.onSyncedDataUpdated(entityData);
      if (NamedEntityData.ITEM_DISPLAY__ITEM_STACK.equals(entityData) || NamedEntityData.ITEM_DISPLAY__ITEM_DISPLAY.equals(entityData)) {
        this.updateRenderState = true;
      }
    }

    private SFItemStack getItemStack() {
      return SFItemStack.from(this.entityData.get(NamedEntityData.ITEM_DISPLAY__ITEM_STACK, MetadataTypes.ITEM));
    }

    private ItemDisplayContext getItemTransform() {
      return ItemDisplayContext.fromId(this.entityData.get(NamedEntityData.ITEM_DISPLAY__ITEM_DISPLAY, MetadataTypes.BYTE));
    }

    public Display.ItemDisplay.ItemRenderState itemRenderState() {
      return this.itemRenderState;
    }

    @Override
    protected void updateRenderSubState(boolean interpolate, float partialTick) {
      var item = this.getItemStack();
      item.setEntityRepresentation(this);
      this.itemRenderState = new Display.ItemDisplay.ItemRenderState(item, this.getItemTransform());
    }

    public record ItemRenderState(SFItemStack itemStack, ItemDisplayContext itemTransform) {
    }
  }

  record LinearFloatInterpolator(float previous, float current) implements Display.FloatInterpolator {
    @Override
    public float get(float partialTick) {
      return MathHelper.lerp(partialTick, this.previous, this.current);
    }
  }

  record LinearIntInterpolator(int previous, int current) implements Display.IntInterpolator {
    @Override
    public int get(float partialTick) {
      return MathHelper.lerpInt(partialTick, this.previous, this.current);
    }
  }

  static class PosRotInterpolationTarget {
    final double targetX;
    final double targetY;
    final double targetZ;
    final double targetYRot;
    final double targetXRot;
    int steps;

    PosRotInterpolationTarget(int steps, double x, double y, double z, double yRot, double xRot) {
      this.steps = steps;
      this.targetX = x;
      this.targetY = y;
      this.targetZ = z;
      this.targetYRot = yRot;
      this.targetXRot = xRot;
    }

    void applyTargetPosAndRot(Entity entity) {
      entity.setPos(this.targetX, this.targetY, this.targetZ);
      entity.setRot((float) this.targetYRot, (float) this.targetXRot);
    }

    void applyLerpStep(Entity entity) {
      entity.lerpPositionAndRotationStep(this.steps, this.targetX, this.targetY, this.targetZ, this.targetYRot, this.targetXRot);
    }
  }

  public record RenderState(
    Display.GenericInterpolator<Transformation> transformation,
    Display.BillboardConstraints billboardConstraints,
    int brightnessOverride,
    Display.FloatInterpolator shadowRadius,
    Display.FloatInterpolator shadowStrength,
    int glowColorOverride
  ) {
  }

  public static class TextDisplay extends Display {
    public static final byte FLAG_ALIGN_LEFT = 8;
    public static final byte FLAG_ALIGN_RIGHT = 16;
    private static final IntSet TEXT_RENDER_STATE_IDS = IntSet.of(
      NamedEntityData.TEXT_DISPLAY__TEXT.networkId(),
      NamedEntityData.TEXT_DISPLAY__LINE_WIDTH.networkId(),
      NamedEntityData.TEXT_DISPLAY__BACKGROUND_COLOR.networkId(),
      NamedEntityData.TEXT_DISPLAY__TEXT_OPACITY.networkId(),
      NamedEntityData.TEXT_DISPLAY__STYLE_FLAGS.networkId()
    );
    private Display.TextDisplay.TextRenderState textRenderState;

    public TextDisplay(EntityType type, Level level) {
      super(type, level);
    }

    public static Display.TextDisplay.Align getAlign(byte flags) {
      if ((flags & FLAG_ALIGN_LEFT) != 0) {
        return Display.TextDisplay.Align.LEFT;
      } else {
        return (flags & FLAG_ALIGN_RIGHT) != 0 ? Display.TextDisplay.Align.RIGHT : Display.TextDisplay.Align.CENTER;
      }
    }

    @Override
    public void onSyncedDataUpdated(NamedEntityData entityData) {
      super.onSyncedDataUpdated(entityData);
      if (TEXT_RENDER_STATE_IDS.contains(entityData.networkId())) {
        this.updateRenderState = true;
      }
    }

    private Component getText() {
      return this.entityData.get(NamedEntityData.TEXT_DISPLAY__TEXT, MetadataTypes.CHAT);
    }

    private int getLineWidth() {
      return this.entityData.get(NamedEntityData.TEXT_DISPLAY__LINE_WIDTH, MetadataTypes.INT);
    }

    private byte getTextOpacity() {
      return this.entityData.get(NamedEntityData.TEXT_DISPLAY__TEXT_OPACITY, MetadataTypes.BYTE);
    }

    private int getBackgroundColor() {
      return this.entityData.get(NamedEntityData.TEXT_DISPLAY__BACKGROUND_COLOR, MetadataTypes.INT);
    }

    private byte getFlags() {
      return this.entityData.get(NamedEntityData.TEXT_DISPLAY__STYLE_FLAGS, MetadataTypes.BYTE);
    }

    @Override
    protected void updateRenderSubState(boolean interpolate, float partialTick) {
      if (interpolate && this.textRenderState != null) {
        this.textRenderState = this.createInterpolatedTextRenderState(this.textRenderState, partialTick);
      } else {
        this.textRenderState = this.createFreshTextRenderState();
      }
    }

    public Display.TextDisplay.TextRenderState textRenderState() {
      return this.textRenderState;
    }

    private Display.TextDisplay.TextRenderState createFreshTextRenderState() {
      return new Display.TextDisplay.TextRenderState(
        this.getText(),
        this.getLineWidth(),
        Display.IntInterpolator.constant(this.getTextOpacity()),
        Display.IntInterpolator.constant(this.getBackgroundColor()),
        this.getFlags()
      );
    }

    private Display.TextDisplay.TextRenderState createInterpolatedTextRenderState(Display.TextDisplay.TextRenderState renderState, float partialTick) {
      var color = renderState.backgroundColor.get(partialTick);
      var opacity = renderState.textOpacity.get(partialTick);
      return new Display.TextDisplay.TextRenderState(
        this.getText(),
        this.getLineWidth(),
        new Display.LinearIntInterpolator(opacity, this.getTextOpacity()),
        new Display.ColorInterpolator(color, this.getBackgroundColor()),
        this.getFlags()
      );
    }

    public enum Align {
      CENTER,
      LEFT,
      RIGHT
    }

    public record TextRenderState(
      Component text, int lineWidth, Display.IntInterpolator textOpacity, Display.IntInterpolator backgroundColor, byte flags
    ) {
    }
  }

  record TransformationInterpolator(Transformation previous, Transformation current) implements Display.GenericInterpolator<Transformation> {
    public Transformation get(float f) {
      return (double) f >= 1.0 ? this.current : this.previous.slerp(this.current, f);
    }
  }
}
