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
package net.pistonmaster.soulfire.server.util;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.*;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;

// Fork of Object2ObjectCustomOpenHashMap
// The main difference is that it uses native equals and hashcode methods
// It also does not support null keys, if you provide one, the map will behave unexpectedly
public class Vec2ObjectOpenHashMap<K extends SWVec3i, V> extends AbstractObject2ObjectMap<K, V> implements Serializable, Cloneable, Hash {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final boolean ASSERTS = false;
    protected final transient int minN;
    protected final float f;
    protected transient K[] key;
    protected transient V[] value;
    protected transient int mask;
    protected transient int n;
    protected transient int maxFill;
    protected int size;
    protected transient FastEntrySet<K, V> entries;
    protected transient ObjectSet<K> keys;
    protected transient ObjectCollection<V> values;

    @SuppressWarnings("unchecked")
    public Vec2ObjectOpenHashMap(final int expected, final float f) {
        if (f <= 0 || f >= 1)
            throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
        if (expected < 0) throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        this.f = f;
        minN = n = arraySize(expected, f);
        mask = n - 1;
        maxFill = maxFill(n, f);
        key = (K[]) new SWVec3i[n + 1];
        value = (V[]) new Object[n + 1];
    }

    public Vec2ObjectOpenHashMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    private static int hashVec(SWVec3i vec) {
        return vec.hashCode();
    }

    private static boolean equalsVec(SWVec3i a, SWVec3i b) {
        if (b == null) {
            return false;
        }

        if (a == b) {
            return true;
        }

        return a.x == b.x
                && a.y == b.y
                && a.z == b.z;
    }

    private int realSize() {
        return size;
    }

    public void ensureCapacity(final int capacity) {
        final var needed = arraySize(capacity, f);
        if (needed > n) rehash(needed);
    }

    private void tryCapacity(final long capacity) {
        final var needed = (int) Math.min(1 << 30, Math.max(2, HashCommon.nextPowerOfTwo((long) Math.ceil(capacity / f))));
        if (needed > n) rehash(needed);
    }

    private V removeEntry(final int pos) {
        final var oldValue = value[pos];
        value[pos] = null;
        size--;
        shiftKeys(pos);
        if (n > minN && size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE) rehash(n / 2);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (f <= .5) ensureCapacity(m.size()); // The resulting map will be sized for m.size() elements
        else tryCapacity(size() + m.size()); // The resulting map will be tentatively sized for size() + m.size()
        // elements
        super.putAll(m);
    }

    private int find(final K k) {
        K curr;
        final var key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix(hashVec(k))) & mask]) == null)) return -(pos + 1);
        if ((equalsVec((k), (curr)))) return pos;
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return -(pos + 1);
            if ((equalsVec((k), (curr)))) return pos;
        }
    }

    private void insert(final int pos, final K k, final V v) {
        key[pos] = k;
        value[pos] = v;
        if (size++ >= maxFill) rehash(arraySize(size + 1, f));
        if (ASSERTS) checkTable();
    }

    @Override
    public V put(final K k, final V v) {
        final var pos = find(k);
        if (pos < 0) {
            insert(-pos - 1, k, v);
            return defRetValue;
        }
        final var oldValue = value[pos];
        value[pos] = v;
        return oldValue;
    }

    protected final void shiftKeys(int pos) {
        // Shift entries with the same hash.
        int last;
        int slot;
        K curr;
        final var key = this.key;
        for (; ; ) {
            pos = ((last = pos) + 1) & mask;
            for (; ; ) {
                if (((curr = key[pos]) == null)) {
                    key[last] = (null);
                    value[last] = null;
                    return;
                }
                slot = (HashCommon.mix(hashVec(curr))) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            key[last] = curr;
            value[last] = value[pos];
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(final Object k) {
        K curr;
        final var key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix(hashVec((K) k))) & mask]) == null)) return defRetValue;
        if ((equalsVec((K) (k), (curr)))) return removeEntry(pos);
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return defRetValue;
            if ((equalsVec((K) (k), (curr)))) return removeEntry(pos);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(final Object k) {
        K curr;
        final var key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix(hashVec((K) k))) & mask]) == null)) return defRetValue;
        if ((equalsVec((K) (k), (curr)))) return value[pos];
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return defRetValue;
            if ((equalsVec((K) (k), (curr)))) return value[pos];
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(final Object k) {
        K curr;
        final var key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix(hashVec((K) k))) & mask]) == null)) return false;
        if ((equalsVec((K) (k), (curr)))) return true;
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return false;
            if ((equalsVec((K) (k), (curr)))) return true;
        }
    }

    @Override
    public boolean containsValue(final Object v) {
        final var value = this.value;
        final var key = this.key;
        for (var i = n; i-- != 0; ) if (!((key[i]) == null) && java.util.Objects.equals(value[i], v)) return true;
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(final Object k, final V defaultValue) {
        K curr;
        final var key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix(hashVec((K) k))) & mask]) == null)) return defaultValue;
        if ((equalsVec((K) (k), (curr)))) return value[pos];
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return defaultValue;
            if ((equalsVec((K) (k), (curr)))) return value[pos];
        }
    }

    @Override
    public V putIfAbsent(final K k, final V v) {
        final var pos = find(k);
        if (pos >= 0) return value[pos];
        insert(-pos - 1, k, v);
        return defRetValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(final Object k, final Object v) {
        K curr;
        final var key = this.key;
        int pos;
        // The starting point.
        if (((curr = key[pos = (HashCommon.mix(hashVec((K) k))) & mask]) == null)) return false;
        if ((equalsVec((K) (k), (curr))) && java.util.Objects.equals(v, value[pos])) {
            removeEntry(pos);
            return true;
        }
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) return false;
            if ((equalsVec((K) (k), (curr))) && java.util.Objects.equals(v, value[pos])) {
                removeEntry(pos);
                return true;
            }
        }
    }

    @Override
    public boolean replace(final K k, final V oldValue, final V v) {
        final var pos = find(k);
        if (pos < 0 || !java.util.Objects.equals(oldValue, value[pos])) return false;
        value[pos] = v;
        return true;
    }

    @Override
    public V replace(final K k, final V v) {
        final var pos = find(k);
        if (pos < 0) return defRetValue;
        final var oldValue = value[pos];
        value[pos] = v;
        return oldValue;
    }

    @Override
    public V computeIfAbsent(final K key, final Object2ObjectFunction<? super K, ? extends V> mappingFunction) {
        java.util.Objects.requireNonNull(mappingFunction);
        final var pos = find(key);
        if (pos >= 0) return value[pos];
        if (!mappingFunction.containsKey(key)) return defRetValue;
        final var newValue = mappingFunction.get(key);
        insert(-pos - 1, key, newValue);
        return newValue;
    }

    @Override
    public V computeIfPresent(final K k, final java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        java.util.Objects.requireNonNull(remappingFunction);
        final var pos = find(k);
        if (pos < 0) return defRetValue;
        if (value[pos] == null) return defRetValue;
        final var newValue = remappingFunction.apply((k), (value[pos]));
        if (newValue == null) {
            removeEntry(pos);
            return defRetValue;
        }
        return value[pos] = (newValue);
    }

    @Override
    public V compute(final K k, final java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        java.util.Objects.requireNonNull(remappingFunction);
        final var pos = find(k);
        final var newValue = remappingFunction.apply((k), pos >= 0 ? (value[pos]) : null);
        if (newValue == null) {
            if (pos >= 0) {
                removeEntry(pos);
            }
            return defRetValue;
        }
        if (pos < 0) {
            insert(-pos - 1, k, (newValue));
            return (newValue);
        }
        return value[pos] = (newValue);
    }

    @Override
    public V merge(final K k, final V v, final java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        if (size == 0) return;
        size = 0;
        Arrays.fill(key, (null));
        Arrays.fill(value, null);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public FastEntrySet<K, V> object2ObjectEntrySet() {
        if (entries == null) entries = new MapEntrySet();
        return entries;
    }

    @Override
    public @NotNull ObjectSet<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ObjectCollection<V> values() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    protected void rehash(final int newN) {
        final var key = this.key;
        final var value = this.value;
        final var mask = newN - 1; // Note that this is used by the hashing macro
        final var newKey = (K[]) new SWVec3i[newN + 1];
        final var newValue = (V[]) new Object[newN + 1];
        var i = n;
        int pos;
        for (var j = realSize(); j-- != 0; ) {
            while (((key[--i]) == null)) ;
            if (!((newKey[pos = (HashCommon.mix(hashVec(key[i]))) & mask]) == null))
                while (!((newKey[pos = (pos + 1) & mask]) == null)) ;
            newKey[pos] = key[i];
            newValue[pos] = value[i];
        }
        newValue[newN] = value[n];
        n = newN;
        this.mask = mask;
        maxFill = maxFill(n, f);
        this.key = newKey;
        this.value = newValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Vec2ObjectOpenHashMap<K, V> clone() {
        Vec2ObjectOpenHashMap<K, V> c;
        try {
            c = (Vec2ObjectOpenHashMap<K, V>) super.clone();
        } catch (CloneNotSupportedException cantHappen) {
            throw new InternalError();
        }
        c.keys = null;
        c.values = null;
        c.entries = null;
        c.key = key.clone();
        c.value = value.clone();
        return c;
    }

    @Override
    public int hashCode() {
        var h = 0;
        for (int j = realSize(), i = 0, t; j-- != 0; ) {
            while (((key[i]) == null)) i++;
            t = (hashVec(key[i]));
            if (this != value[i]) t ^= ((value[i]) == null ? 0 : (value[i]).hashCode());
            h += t;
            i++;
        }

        return h;
    }

    private void checkTable() {
    }

    final class MapEntry implements Entry<K, V>, Map.Entry<K, V>, it.unimi.dsi.fastutil.Pair<K, V> {
        // The table index this entry refers to, or -1 if this entry has been deleted.
        int index;

        MapEntry(final int index) {
            this.index = index;
        }

        MapEntry() {
        }

        @Override
        public K getKey() {
            return key[index];
        }

        @Override
        public K left() {
            return key[index];
        }

        @Override
        public V getValue() {
            return value[index];
        }

        @Override
        public V right() {
            return value[index];
        }

        @Override
        public V setValue(final V v) {
            final var oldValue = value[index];
            value[index] = v;
            return oldValue;
        }

        @Override
        public it.unimi.dsi.fastutil.Pair<K, V> right(final V v) {
            value[index] = v;
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) return false;
            var e = (Map.Entry<K, V>) o;
            return (equalsVec((key[index]), ((e.getKey())))) && java.util.Objects.equals(value[index], (e.getValue()));
        }

        @Override
        public int hashCode() {
            return (hashVec(key[index])) ^ ((value[index]) == null ? 0 : (value[index]).hashCode());
        }

        @Override
        public String toString() {
            return key[index] + "=>" + value[index];
        }
    }

    private abstract class MapIterator<ConsumerType> {
        int pos = n;
        int last = -1;
        int c = size;
        ObjectArrayList<K> wrapped;

        @SuppressWarnings("unused")
        abstract void acceptOnIndex(final ConsumerType action, final int index);

        public boolean hasNext() {
            return c != 0;
        }

        public int nextEntry() {
            if (!hasNext()) throw new NoSuchElementException();
            c--;
            final var key = Vec2ObjectOpenHashMap.this.key;
            for (; ; ) {
                if (--pos < 0) {
                    // We are just enumerating elements from the wrapped list.
                    last = Integer.MIN_VALUE;
                    final var k = wrapped.get(-pos - 1);
                    var p = (HashCommon.mix(hashVec(k))) & mask;
                    while (!(equalsVec((k), (key[p])))) p = (p + 1) & mask;
                    return p;
                }
                if (!((key[pos]) == null)) return last = pos;
            }
        }

        public void forEachRemaining(final ConsumerType action) {
            final var key = Vec2ObjectOpenHashMap.this.key;
            while (c != 0) {
                if (--pos < 0) {
                    // We are just enumerating elements from the wrapped list.
                    last = Integer.MIN_VALUE;
                    final var k = wrapped.get(-pos - 1);
                    var p = (HashCommon.mix(hashVec(k))) & mask;
                    while (!(equalsVec((k), (key[p])))) p = (p + 1) & mask;
                    acceptOnIndex(action, p);
                    c--;
                } else if (!((key[pos]) == null)) {
                    acceptOnIndex(action, last = pos);
                    c--;
                }
            }
        }

        private void shiftKeys(int pos) {
            // Shift entries with the same hash.
            int last;
            int slot;
            K curr;
            final var key = Vec2ObjectOpenHashMap.this.key;
            for (; ; ) {
                pos = ((last = pos) + 1) & mask;
                for (; ; ) {
                    if (((curr = key[pos]) == null)) {
                        key[last] = (null);
                        value[last] = null;
                        return;
                    }
                    slot = (HashCommon.mix(hashVec(curr))) & mask;
                    if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                    pos = (pos + 1) & mask;
                }
                if (pos < last) { // Wrapped entry.
                    if (wrapped == null) wrapped = new ObjectArrayList<>(2);
                    wrapped.add(key[pos]);
                }
                key[last] = curr;
                value[last] = value[pos];
            }
        }

        public void remove() {
            if (last == -1) throw new IllegalStateException();
            if (last == n) {
                key[n] = null;
                value[n] = null;
            } else if (pos >= 0) shiftKeys(last);
            else {
                // We're removing wrapped entries.
                Vec2ObjectOpenHashMap.this.remove(wrapped.set(-pos - 1, null));
                last = -1; // Note that we must not decrement size
                return;
            }
            size--;
            last = -1; // You can no longer remove this entry.
            if (ASSERTS) checkTable();
        }

        public int skip(final int n) {
            var i = n;
            while (i-- != 0 && hasNext()) nextEntry();
            return n - i - 1;
        }
    }

    private final class EntryIterator extends MapIterator<Consumer<? super Entry<K, V>>> implements ObjectIterator<Entry<K, V>> {
        private MapEntry entry;

        @Override
        public MapEntry next() {
            return entry = new MapEntry(nextEntry());
        }

        // forEachRemaining inherited from MapIterator superclass.
        @Override
        void acceptOnIndex(final Consumer<? super Entry<K, V>> action, final int index) {
            action.accept(entry = new MapEntry(index));
        }

        @Override
        public void remove() {
            super.remove();
            entry.index = -1; // You cannot use a deleted entry.
        }
    }

    private final class FastEntryIterator extends MapIterator<Consumer<? super Entry<K, V>>> implements ObjectIterator<Entry<K, V>> {
        private final MapEntry entry = new MapEntry();

        @Override
        public MapEntry next() {
            entry.index = nextEntry();
            return entry;
        }

        // forEachRemaining inherited from MapIterator superclass.
        @Override
        void acceptOnIndex(final Consumer<? super Entry<K, V>> action, final int index) {
            entry.index = index;
            action.accept(entry);
        }
    }

    private abstract class MapSpliterator<ConsumerType, SplitType extends MapSpliterator<ConsumerType, SplitType>> {
        int pos = 0;
        int max = n;
        int c = 0;
        boolean hasSplit = false;

        MapSpliterator() {
        }

        MapSpliterator(int pos, int max, boolean hasSplit) {
            this.pos = pos;
            this.max = max;
            this.hasSplit = hasSplit;
        }

        abstract void acceptOnIndex(final ConsumerType action, final int index);

        abstract SplitType makeForSplit(int pos, int max);

        public boolean tryAdvance(final ConsumerType action) {
            final var key = Vec2ObjectOpenHashMap.this.key;
            while (pos < max) {
                if (!((key[pos]) == null)) {
                    ++c;
                    acceptOnIndex(action, pos++);
                    return true;
                }
                ++pos;
            }
            return false;
        }

        public void forEachRemaining(final ConsumerType action) {
            final var key = Vec2ObjectOpenHashMap.this.key;
            while (pos < max) {
                if (!((key[pos]) == null)) {
                    acceptOnIndex(action, pos);
                    ++c;
                }
                ++pos;
            }
        }

        public long estimateSize() {
            if (!hasSplit) {
                // Root spliterator; we know how many are remaining.
                return size - c;
            } else {
                // After we split, we can no longer know exactly how many we have (or at least not efficiently).
                // (size / n) * (max - pos) aka currentTableDensity * numberOfBucketsLeft seems like a good
                // estimate.
                return Math.min(size - c, (long) (((double) realSize() / n) * (max - pos)));
            }
        }

        public SplitType trySplit() {
            if (pos >= max - 1) return null;
            var retLen = (max - pos) >> 1;
            if (retLen <= 1) return null;
            var myNewPos = pos + retLen;
            var retPos = pos;
            // Since null is returned first, and the convention is that the returned split is the prefix of
            // elements,
            // the split will take care of returning null (if needed), and we won't return it anymore.
            var split = makeForSplit(retPos, myNewPos);
            this.pos = myNewPos;
            this.hasSplit = true;
            return split;
        }

        public long skip(long n) {
            if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
            if (n == 0) return 0;
            long skipped = 0;
            final var key = Vec2ObjectOpenHashMap.this.key;
            while (pos < max && n > 0) {
                if (!((key[pos++]) == null)) {
                    ++skipped;
                    --n;
                }
            }
            return skipped;
        }
    }

    private final class EntrySpliterator extends MapSpliterator<Consumer<? super Entry<K, V>>, EntrySpliterator> implements ObjectSpliterator<Entry<K, V>> {
        private static final int POST_SPLIT_CHARACTERISTICS = ObjectSpliterators.SET_SPLITERATOR_CHARACTERISTICS & ~java.util.Spliterator.SIZED;

        EntrySpliterator() {
        }

        EntrySpliterator(int pos, int max) {
            super(pos, max, true);
        }

        @Override
        public int characteristics() {
            return hasSplit ? POST_SPLIT_CHARACTERISTICS : ObjectSpliterators.SET_SPLITERATOR_CHARACTERISTICS;
        }

        @Override
        void acceptOnIndex(final Consumer<? super Entry<K, V>> action, final int index) {
            action.accept(new MapEntry(index));
        }

        @Override
        EntrySpliterator makeForSplit(int pos, int max) {
            return new EntrySpliterator(pos, max);
        }
    }

    private final class MapEntrySet extends AbstractObjectSet<Entry<K, V>> implements FastEntrySet<K, V> {
        @Override
        public @NotNull ObjectIterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public ObjectIterator<Entry<K, V>> fastIterator() {
            return new FastEntryIterator();
        }

        @Override
        public ObjectSpliterator<Entry<K, V>> spliterator() {
            return new EntrySpliterator();
        }

        //
        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            final var k = ((K) e.getKey());
            final var v = ((V) e.getValue());
            K curr;
            final var key = Vec2ObjectOpenHashMap.this.key;
            int pos;
            // The starting point.
            if (((curr = key[pos = (HashCommon.mix(hashVec(k))) & mask]) == null)) return false;
            if ((equalsVec((k), (curr)))) return java.util.Objects.equals(value[pos], v);
            // There's always an unused entry.
            while (true) {
                if (((curr = key[pos = (pos + 1) & mask]) == null)) return false;
                if ((equalsVec((k), (curr)))) return java.util.Objects.equals(value[pos], v);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(final Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) return false;
            final var k = ((K) e.getKey());
            final var v = ((V) e.getValue());
            K curr;
            final var key = Vec2ObjectOpenHashMap.this.key;
            int pos;
            // The starting point.
            if (((curr = key[pos = (HashCommon.mix(hashVec(k))) & mask]) == null)) return false;
            if ((equalsVec((curr), (k)))) {
                if (java.util.Objects.equals(value[pos], v)) {
                    removeEntry(pos);
                    return true;
                }
                return false;
            }
            while (true) {
                if (((curr = key[pos = (pos + 1) & mask]) == null)) return false;
                if ((equalsVec((curr), (k)))) {
                    if (java.util.Objects.equals(value[pos], v)) {
                        removeEntry(pos);
                        return true;
                    }
                }
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Vec2ObjectOpenHashMap.this.clear();
        }

        @Override
        public void forEach(final Consumer<? super Entry<K, V>> consumer) {
            for (var pos = n; pos-- != 0; ) if (!((key[pos]) == null)) consumer.accept(new MapEntry(pos));
        }

        @Override
        public void fastForEach(final Consumer<? super Entry<K, V>> consumer) {
            final var entry = new MapEntry();
            for (var pos = n; pos-- != 0; )
                if (!((key[pos]) == null)) {
                    entry.index = pos;
                    consumer.accept(entry);
                }
        }
    }
}

