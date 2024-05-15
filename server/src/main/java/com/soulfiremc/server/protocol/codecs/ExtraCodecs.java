package com.soulfiremc.server.protocol.codecs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.BaseMapCodec;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class ExtraCodecs {
  public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
  public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
  public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
    .flatComapMap(
      UnsignedBytes::toInt,
      integer -> integer > 255 ? DataResult.error(() -> "Unsigned byte was too large: " + integer + " > 255") : DataResult.success(integer.byteValue())
    );
  public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, integer -> "Value must be non-negative: " + integer);
  public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, integer -> "Value must be positive: " + integer);
  public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, float_ -> "Value must be positive: " + float_);
  public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(string -> {
    try {
      return DataResult.success(Pattern.compile(string));
    } catch (PatternSyntaxException var2) {
      return DataResult.error(() -> "Invalid regex pattern '" + string + "': " + var2.getMessage());
    }
  }, Pattern::pattern);
  public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
  public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(string -> {
    try {
      return DataResult.success(Base64.getDecoder().decode(string));
    } catch (IllegalArgumentException var2) {
      return DataResult.error(() -> "Malformed base64 string");
    }
  }, bs -> Base64.getEncoder().encodeToString(bs));
  public static final Function<Optional<Long>, OptionalLong> toOptionalLong = optional -> optional.map(OptionalLong::of).orElseGet(OptionalLong::empty);
  public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = optionalLong -> optionalLong.isPresent()
    ? Optional.of(optionalLong.getAsLong())
    : Optional.empty();
  public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
    .xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> Arrays.stream(bitSet.toLongArray()));
  public static final Codec<String> NON_EMPTY_STRING = Codec.STRING
    .validate(string -> string.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(string));
  public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(string -> {
    var is = string.codePoints().toArray();
    return is.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + string) : DataResult.success(is[0]);
  }, Character::toString);

  public static <T> Codec<T> converter(DynamicOps<T> ops) {
    return Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(ops).getValue(), object -> new Dynamic<>(ops, object));
  }

  public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> function, Function<E, Lifecycle> function2) {
    return codec.mapResult(new ResultFunction<>() {
      public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<E, T>> dataResult) {
        return dataResult.result().map(pair -> dataResult.setLifecycle(function.apply(pair.getFirst()))).orElse(dataResult);
      }

      public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, E object, DataResult<T> dataResult) {
        return dataResult.setLifecycle(function2.apply(object));
      }

      @Override
      public String toString() {
        return "WithLifecycle[" + function + " " + function2 + "]";
      }
    });
  }

  public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> lifecycleGetter) {
    return overrideLifecycle(codec, lifecycleGetter, lifecycleGetter);
  }

  public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> key, Codec<V> value) {
    return new ExtraCodecs.StrictUnboundedMapCodec<>(key, value);
  }

  private static Codec<Integer> intRangeWithMessage(int min, int max, Function<Integer, String> errorMessage) {
    return Codec.INT
      .validate(
        integer -> integer.compareTo(min) >= 0 && integer.compareTo(max) <= 0
          ? DataResult.success(integer)
          : DataResult.error(() -> errorMessage.apply(integer))
      );
  }

  public static Codec<Integer> intRange(int min, int max) {
    return intRangeWithMessage(min, max, integer -> "Value must be within range [" + min + ";" + max + "]: " + integer);
  }

  private static Codec<Float> floatRangeMinExclusiveWithMessage(float min, float max, Function<Float, String> errorMessage) {
    return Codec.FLOAT
      .validate(
        float_ -> float_.compareTo(min) > 0 && float_.compareTo(max) <= 0 ? DataResult.success(float_) : DataResult.error(() -> errorMessage.apply(float_))
      );
  }

  public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> codec) {
    return codec.validate(list -> list.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(list));
  }

  public static <E> MapCodec<E> retrieveContext(Function<DynamicOps<?>, DataResult<E>> retriever) {
    class ContextRetrievalCodec extends MapCodec<E> {
      public <T> RecordBuilder<T> encode(E object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
        return recordBuilder;
      }

      public <T> DataResult<E> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
        return retriever.apply(dynamicOps);
      }

      public String toString() {
        return "ContextRetrievalCodec[" + retriever + "]";
      }

      public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
        return Stream.empty();
      }
    }

    return new ContextRetrievalCodec();
  }

  public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> typeGetter) {
    return collection -> {
      var iterator = collection.iterator();
      if (iterator.hasNext()) {
        var object = typeGetter.apply(iterator.next());

        while (iterator.hasNext()) {
          var object2 = iterator.next();
          var object3 = typeGetter.apply(object2);
          if (object3 != object) {
            return DataResult.error(() -> "Mixed type list: element " + object2 + " had type " + object3 + ", but list is of type " + object);
          }
        }
      }

      return DataResult.success(collection, Lifecycle.stable());
    };
  }

  public static <A> Codec<A> catchDecoderException(Codec<A> codec) {
    return Codec.of(codec, new Decoder<>() {
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> dynamicOps, T object) {
        try {
          return codec.decode(dynamicOps, object);
        } catch (Exception var4) {
          return DataResult.error(() -> "Caught exception decoding " + object + ": " + var4.getMessage());
        }
      }
    });
  }

  public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter dateTimeFormatter) {
    return Codec.STRING.comapFlatMap(string -> {
      try {
        return DataResult.success(dateTimeFormatter.parse(string));
      } catch (Exception var3) {
        return DataResult.error(var3::getMessage);
      }
    }, dateTimeFormatter::format);
  }

  public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> codec) {
    return codec.xmap(toOptionalLong, fromOptionalLong);
  }

  public static <K, V> Codec<Map<K, V>> sizeLimitedMap(Codec<Map<K, V>> mapCodec, int maxSize) {
    return mapCodec.validate(
      map -> map.size() > maxSize
        ? DataResult.error(() -> "Map is too long: " + map.size() + ", expected range [0-" + maxSize + "]")
        : DataResult.success(map)
    );
  }

  public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> codec) {
    return Codec.unboundedMap(codec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
  }

  public static <A> Codec<Optional<A>> optionalEmptyMap(Codec<A> codec) {
    return new Codec<>() {
      public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        return isEmptyMap(dynamicOps, object)
          ? DataResult.success(Pair.of(Optional.empty(), object))
          : codec.decode(dynamicOps, object).map(pair -> pair.mapFirst(Optional::of));
      }

      private static <T> boolean isEmptyMap(DynamicOps<T> ops, T value) {
        var optional = ops.getMap(value).result();
        return optional.isPresent() && optional.get().entries().findAny().isEmpty();
      }

      public <T> DataResult<T> encode(Optional<A> input, DynamicOps<T> ops, T value) {
        return input.isEmpty() ? DataResult.success(ops.emptyMap()) : codec.encode(input.get(), ops, value);
      }
    };
  }

  public record StrictUnboundedMapCodec<K, V>(Codec<K> a, Codec<V> b) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
    public <T> DataResult<Map<K, V>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
      Builder<K, V> builder = ImmutableMap.builder();

      for (var pair : mapLike.entries().toList()) {
        var dataResult = this.keyCodec().parse(dynamicOps, pair.getFirst());
        var dataResult2 = this.elementCodec().parse(dynamicOps, pair.getSecond());
        var dataResult3 = dataResult.apply2stable(Pair::of, dataResult2);
        var optional = dataResult3.error();
        if (optional.isPresent()) {
          var string = optional.get().message();
          return DataResult.error(() -> dataResult.result().isPresent() ? "Map entry '" + dataResult.result().get() + "' : " + string : string);
        }

        if (dataResult3.result().isEmpty()) {
          return DataResult.error(() -> "Empty or invalid map contents are not allowed");
        }

        var pair2 = (Pair<K, V>) dataResult3.result().get();
        builder.put(pair2.getFirst(), pair2.getSecond());
      }

      Map<K, V> map = builder.build();
      return DataResult.success(map);
    }

    public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> dynamicOps, T object) {
      return dynamicOps.getMap(object)
        .setLifecycle(Lifecycle.stable())
        .flatMap(mapLike -> this.decode(dynamicOps, mapLike))
        .map(map -> Pair.of(map, object));
    }

    public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T value) {
      return this.encode(input, ops, ops.mapBuilder()).build(value);
    }

    public String toString() {
      return "StrictUnboundedMapCodec[" + this.a + " -> " + this.b + "]";
    }

    public Codec<K> keyCodec() {
      return this.a;
    }

    public Codec<V> elementCodec() {
      return this.b;
    }
  }
}
