package com.blockkillers.core;

import java.util.function.Supplier;

/**
 * Fixed-capacity object pool + dense active list. obtain() reuses a preallocated object,
 * removeAt() swaps with the last active element. No allocation during gameplay.
 */
public final class EntityList<T> {
    private final Object[] items;
    public int size;

    public EntityList(int capacity, Supplier<T> factory) {
        items = new Object[capacity];
        for (int i = 0; i < capacity; i++) items[i] = factory.get();
    }
    @SuppressWarnings("unchecked")
    public T get(int i) { return (T) items[i]; }
    public int capacity() { return items.length; }
    public boolean full() { return size >= items.length; }
    /** Returns a recycled object or null if the pool is exhausted. */
    @SuppressWarnings("unchecked")
    public T obtain() { return size >= items.length ? null : (T) items[size++]; }
    public void removeAt(int i) {
        Object o = items[i];
        items[i] = items[size - 1];
        items[size - 1] = o;
        size--;
    }
    public void clear() { size = 0; }
}
