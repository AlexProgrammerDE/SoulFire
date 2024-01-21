/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.server.util;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.*;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
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
    /**
     * We never resize below this threshold, which is the construction-time {#n}.
     */
    protected final transient int minN;
    /**
     * The acceptable load factor.
     */
    protected final float f;
    /**
     * The array of keys.
     */
    protected transient K[] key;
    /**
     * The array of values.
     */
    protected transient V[] value;
    /**
     * The mask for wrapping a position counter.
     */
    protected transient int mask;
    /**
     * The current table size.
     */
    protected transient int n;
    /**
     * Threshold after which we rehash. It must be the table size times {@link #f}.
     */
    protected transient int maxFill;
    /**
     * Number of entries in the set (including the key zero, if present).
     */
    protected int size;
    /**
     * Cached set of entries.
     */
    protected transient FastEntrySet<K, V> entries;
    /**
     * Cached set of keys.
     */
    protected transient ObjectSet<K> keys;
    /**
     * Cached collection of values.
     */
    protected transient ObjectCollection<V> values;

    /**
     * Creates a new hash map.
     *
     * <p>
     * The actual table size will be the least power of two greater than {@code expected}/{@code f}.
     *
     * @param expected the expected number of elements in the hash map.
     * @param f        the load factor.
     */
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

    /**
     * Creates a new hash map with initial expected {@link Hash#DEFAULT_INITIAL_SIZE} entries and
     * {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
     */
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

    /**
     * Ensures that this map can hold a certain number of keys without rehashing.
     *
     * @param capacity a number of keys; there will be no rehashing unless the map {@linkplain #size()
     *                 size} exceeds this number.
     */
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

    /**
     * Shifts left entries with the specified hash code, starting at the specified position, and empties
     * the resulting free entry.
     *
     * @param pos a starting position.
     */
    protected final void shiftKeys(int pos) {
        // Shift entries with the same hash.
        int last, slot;
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public V putIfAbsent(final K k, final V v) {
        final var pos = find(k);
        if (pos >= 0) return value[pos];
        insert(-pos - 1, k, v);
        return defRetValue;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(final K k, final V oldValue, final V v) {
        final var pos = find(k);
        if (pos < 0 || !java.util.Objects.equals(oldValue, value[pos])) return false;
        value[pos] = v;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V replace(final K k, final V v) {
        final var pos = find(k);
        if (pos < 0) return defRetValue;
        final var oldValue = value[pos];
        value[pos] = v;
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public V merge(final K k, final V v, final java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        java.util.Objects.requireNonNull(remappingFunction);
        java.util.Objects.requireNonNull(v);
        final var pos = find(k);
        if (pos < 0 || value[pos] == null) {
            if (pos < 0) insert(-pos - 1, k, v);
            else value[pos] = v;
            return v;
        }
        final var newValue = remappingFunction.apply((value[pos]), (v));
        if (newValue == null) {
            removeEntry(pos);
            return defRetValue;
        }
        return value[pos] = (newValue);
    }

    /* Removes all elements from this map.
     *
     * <p>To increase object reuse, this method does not change the table size.
     * If you want to reduce the table size, you must use {@link #trim()}.
     *
     */
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
        if (keys == null) keys = new KeySet();
        return keys;
    }

    @Override
    public @NotNull ObjectCollection<V> values() {
        if (values == null) values = new AbstractObjectCollection<>() {
            @Override
            public @NotNull ObjectIterator<V> iterator() {
                return new ValueIterator();
            }

            @Override
            public ObjectSpliterator<V> spliterator() {
                return new ValueSpliterator();
            }

            /** {@inheritDoc} */
            @Override
            public void forEach(final Consumer<? super V> consumer) {
                for (var pos = n; pos-- != 0; ) if (!((key[pos]) == null)) consumer.accept(value[pos]);
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean contains(Object v) {
                return containsValue(v);
            }

            @Override
            public void clear() {
                Vec2ObjectOpenHashMap.this.clear();
            }
        };
        return values;
    }

    /**
     * Rehashes the map, making the table as small as possible.
     *
     * <p>
     * This method rehashes the table to the smallest size satisfying the load factor. It can be used
     * when the set will not be changed anymore, so to optimize access speed and size.
     *
     * <p>
     * If the table size is already the minimum possible, this method does nothing.
     *
     * @return true if there was enough memory to trim the map.
     * @see #trim(int)
     */
    public boolean trim() {
        return trim(size);
    }

    /**
     * Rehashes this map if the table is too large.
     *
     * <p>
     * Let <var>N</var> be the smallest table size that can hold <code>max(n,{@link #size()})</code>
     * entries, still satisfying the load factor. If the current table size is smaller than or equal to
     * <var>N</var>, this method does nothing. Otherwise, it rehashes this map in a table of size
     * <var>N</var>.
     *
     * <p>
     * This method is useful when reusing maps. {@linkplain #clear() Clearing a map} leaves the table
     * size untouched. If you are reusing a map many times, you can call this method with a typical size
     * to avoid keeping around a very large table just because of a few large transient maps.
     *
     * @param n the threshold for the trimming.
     * @return true if there was enough memory to trim the map.
     * @see #trim()
     */
    public boolean trim(final int n) {
        final var l = HashCommon.nextPowerOfTwo((int) Math.ceil(n / f));
        if (l >= this.n || size > maxFill(l, f)) return true;
        try {
            rehash(l);
        } catch (OutOfMemoryError cantDoIt) {
            return false;
        }
        return true;
    }

    /**
     * Rehashes the map.
     *
     * <p>
     * This method implements the basic rehashing strategy, and may be overridden by subclasses
     * implementing different rehashing strategies (e.g., disk-based rehashing). However, you should not
     * override this method unless you understand the internal workings of this class.
     *
     * @param newN the new size
     */
    @SuppressWarnings("unchecked")
    protected void rehash(final int newN) {
        final var key = this.key;
        final var value = this.value;
        final var mask = newN - 1; // Note that this is used by the hashing macro
        final var newKey = (K[]) new SWVec3i[newN + 1];
        final var newValue = (V[]) new Object[newN + 1];
        int i = n, pos;
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

    /**
     * Returns a deep copy of this map.
     *
     * <p>
     * This method performs a deep copy of this hash map; the data stored in the map, however, is not
     * cloned. Note that this makes a difference only for object keys.
     *
     * @return a deep copy of this map.
     */
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

    /**
     * Returns a hash code for this map.
     * <p>
     * This method overrides the generic method provided by the superclass. Since {@code equals()} is
     * not overriden, it is important that the value returned by this method is the same value as the
     * one returned by the overriden method.
     *
     * @return a hash code for this map.
     */
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

    /**
     * The entry class for a hash map does not record key and value, but rather the position in the hash
     * table of the corresponding entry. This is necessary so that calls to
     * {@link java.util.Map.Entry#setValue(Object)} are reflected in the map
     */
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

    /**
     * An iterator over a hash map.
     */
    private abstract class MapIterator<ConsumerType> {
        /**
         * The index of the last entry returned, if positive or zero; initially, {@link #n}. If negative,
         * the last entry returned was that of the key of index {@code - pos - 1} from the {@link #wrapped}
         * list.
         */
        int pos = n;
        /**
         * The index of the last entry that has been returned (more precisely, the value of {@link #pos} if
         * {@link #pos} is positive, or {@link Integer#MIN_VALUE} if {@link #pos} is negative). It is -1 if
         * either we did not return an entry yet, or the last returned entry has been removed.
         */
        int last = -1;
        /**
         * A downward counter measuring how many entries must still be returned.
         */
        int c = size;
        /**
         * A lazily allocated list containing keys of entries that have wrapped around the table because of
         * removals.
         */
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

        /**
         * Shifts left entries with the specified hash code, starting at the specified position, and empties
         * the resulting free entry.
         *
         * @param pos a starting position.
         */
        private void shiftKeys(int pos) {
            // Shift entries with the same hash.
            int last, slot;
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
        /**
         * The index (which bucket) of the next item to give to the action.
         * counts up instead of down.
         */
        int pos = 0;
        /**
         * The maximum bucket (exclusive) to iterate to
         */
        int max = n;
        /**
         * An upwards counter counting how many we have given
         */
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

        EntrySpliterator(int pos, int max, boolean hasSplit) {
            super(pos, max, hasSplit);
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
            return new EntrySpliterator(pos, max, true);
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void forEach(final Consumer<? super Entry<K, V>> consumer) {
            for (var pos = n; pos-- != 0; ) if (!((key[pos]) == null)) consumer.accept(new MapEntry(pos));
        }

        /**
         * {@inheritDoc}
         */
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

    /**
     * An iterator on keys.
     *
     * <p>
     * We simply override the
     * {@link java.util.ListIterator#next()}/{@link java.util.ListIterator#previous()} methods (and
     * possibly their type-specific counterparts) so that they return keys instead of entries.
     */
    private final class KeyIterator extends MapIterator<Consumer<? super K>> implements ObjectIterator<K> {
        public KeyIterator() {
            super();
        }

        // forEachRemaining inherited from MapIterator superclass.
        // Despite the superclass declared with generics, the way Java inherits and generates bridge methods
        // avoids the boxing/unboxing
        @Override
        void acceptOnIndex(final Consumer<? super K> action, final int index) {
            action.accept(key[index]);
        }

        @Override
        public K next() {
            return key[nextEntry()];
        }
    }

    private final class KeySpliterator extends MapSpliterator<Consumer<? super K>, KeySpliterator> implements ObjectSpliterator<K> {
        private static final int POST_SPLIT_CHARACTERISTICS = ObjectSpliterators.SET_SPLITERATOR_CHARACTERISTICS & ~java.util.Spliterator.SIZED;

        KeySpliterator() {
        }

        KeySpliterator(int pos, int max, boolean hasSplit) {
            super(pos, max, hasSplit);
        }

        @Override
        public int characteristics() {
            return hasSplit ? POST_SPLIT_CHARACTERISTICS : ObjectSpliterators.SET_SPLITERATOR_CHARACTERISTICS;
        }

        @Override
        void acceptOnIndex(final Consumer<? super K> action, final int index) {
            action.accept(key[index]);
        }

        @Override
        KeySpliterator makeForSplit(int pos, int max) {
            return new KeySpliterator(pos, max, true);
        }
    }

    private final class KeySet extends AbstractObjectSet<K> {
        @Override
        public @NotNull ObjectIterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public ObjectSpliterator<K> spliterator() {
            return new KeySpliterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void forEach(final Consumer<? super K> consumer) {
            for (var pos = n; pos-- != 0; ) {
                final var k = key[pos];
                if (!((k) == null)) consumer.accept(k);
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean contains(Object k) {
            return containsKey(k);
        }

        @Override
        public boolean remove(Object k) {
            final var oldSize = size;
            Vec2ObjectOpenHashMap.this.remove(k);
            return size != oldSize;
        }

        @Override
        public void clear() {
            Vec2ObjectOpenHashMap.this.clear();
        }
    }

    /**
     * An iterator on values.
     *
     * <p>
     * We simply override the
     * {@link java.util.ListIterator#next()}/{@link java.util.ListIterator#previous()} methods (and
     * possibly their type-specific counterparts) so that they return values instead of entries.
     */
    private final class ValueIterator extends MapIterator<Consumer<? super V>> implements ObjectIterator<V> {
        public ValueIterator() {
            super();
        }

        // forEachRemaining inherited from MapIterator superclass.
        // Despite the superclass declared with generics, the way Java inherits and generates bridge methods
        // avoids the boxing/unboxing
        @Override
        void acceptOnIndex(final Consumer<? super V> action, final int index) {
            action.accept(value[index]);
        }

        @Override
        public V next() {
            return value[nextEntry()];
        }
    }

    private final class ValueSpliterator extends MapSpliterator<Consumer<? super V>, ValueSpliterator> implements ObjectSpliterator<V> {
        private static final int POST_SPLIT_CHARACTERISTICS = ObjectSpliterators.COLLECTION_SPLITERATOR_CHARACTERISTICS & ~java.util.Spliterator.SIZED;

        ValueSpliterator() {
        }

        ValueSpliterator(int pos, int max, boolean hasSplit) {
            super(pos, max, hasSplit);
        }

        @Override
        public int characteristics() {
            return hasSplit ? POST_SPLIT_CHARACTERISTICS : ObjectSpliterators.COLLECTION_SPLITERATOR_CHARACTERISTICS;
        }

        @Override
        void acceptOnIndex(final Consumer<? super V> action, final int index) {
            action.accept(value[index]);
        }

        @Override
        ValueSpliterator makeForSplit(int pos, int max) {
            return new ValueSpliterator(pos, max, true);
        }
    }
}

