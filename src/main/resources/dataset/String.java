package com.example.util;

/**
 * A simplified String utility class for testing purposes.
 * This class demonstrates immutability and the Builder pattern.
 *
 * Key characteristics:
 * - Immutable design
 * - Builder pattern for construction
 * - Value object semantics
 */
public final class String {

    private final char[] value;
    private final int length;

    /**
     * Creates a String from a character array.
     * Design pattern: Immutable Value Object
     *
     * @param chars the character array
     */
    public String(char[] chars) {
        this.value = chars.clone(); // Defensive copy for immutability
        this.length = chars.length;
    }

    /**
     * Creates a String from another String.
     *
     * @param original the original string
     */
    public String(String original) {
        this.value = original.value.clone();
        this.length = original.length;
    }

    /**
     * Returns the length of this string.
     *
     * @return the string length
     */
    public int length() {
        return length;
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index the character index
     * @return the character at the specified position
     */
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return value[index];
    }

    /**
     * Concatenates this string with another.
     * Returns a new String (immutability).
     *
     * @param other the string to concatenate
     * @return a new concatenated string
     */
    public String concat(String other) {
        char[] newValue = new char[this.length + other.length];
        System.arraycopy(this.value, 0, newValue, 0, this.length);
        System.arraycopy(other.value, 0, newValue, this.length, other.length);
        return new String(newValue);
    }

    /**
     * Builder for constructing String instances.
     * Design pattern: Builder pattern
     */
    public static class Builder {
        private char[] buffer;
        private int position;

        public Builder() {
            this.buffer = new char[16];
            this.position = 0;
        }

        public Builder append(char c) {
            ensureCapacity(position + 1);
            buffer[position++] = c;
            return this;
        }

        private void ensureCapacity(int capacity) {
            if (capacity > buffer.length) {
                char[] newBuffer = new char[capacity * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, position);
                buffer = newBuffer;
            }
        }

        public String build() {
            char[] result = new char[position];
            System.arraycopy(buffer, 0, result, 0, position);
            return new String(result);
        }
    }
}

