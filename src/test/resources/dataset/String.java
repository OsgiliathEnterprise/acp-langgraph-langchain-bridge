package java.lang;

/**
 * The String class represents character strings. All string literals in Java
 * programs, such as "abc", are implemented as instances of this class.
 *
 * Strings are constant; their values cannot be changed after they are created.
 * Because String objects are immutable they can be shared.
 */
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence
{
    /** The value is used for character storage. */
    private final char value[];

    /** The offset is the first index of the storage that is used. */
    private final int offset;

    /** The count is the number of characters in the String. */
    private final int count;

    /** Cache the hash code for the string */
    private int hash; // Default to 0

    /**
     * Initializes a newly created String object so that it represents
     * an empty character sequence.
     */
    public String() {
        this.offset = 0;
        this.count = 0;
        this.value = new char[0];
    }

    /**
     * Initializes a newly created String object so that it represents the
     * same sequence of characters as the argument; in other words, the newly
     * created string is a copy of the argument string.
     */
    public String(String original) {
        int size = original.count;
        char[] originalValue = original.value;
        char[] v;
        if (originalValue.length > size) {
            v = Arrays.copyOf(originalValue, size);
        } else {
            v = originalValue;
        }
        this.offset = 0;
        this.count = size;
        this.value = v;
    }

    /**
     * Allocates a new String so that it represents the sequence of
     * characters currently contained in the character array argument.
     */
    public String(char value[]) {
        this.offset = 0;
        this.count = value.length;
        this.value = Arrays.copyOf(value, value.length);
    }

    /**
     * Allocates a new String that contains characters from a subarray
     * of the character array argument.
     */
    public String(char value[], int offset, int count) {
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count <= 0) {
            if (count == 0) {
                this.offset = 0;
                this.count = 0;
                this.value = new char[0];
                return;
            }
            throw new StringIndexOutOfBoundsException(count);
        }
        // Note: offset or count might be near -1>>>1.
        if (offset > value.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }
        this.offset = 0;
        this.value = Arrays.copyOfRange(value, offset, offset+count);
        this.count = count;
    }

    /**
     * Allocates a new String constructed from a subarray
     * of the byte array argument.
     */
    public String(byte bytes[], int offset, int length, String charsetName)
        throws java.io.UnsupportedEncodingException
    {
        if (charsetName == null) throw new NullPointerException("charsetName");
        checkBounds(bytes, offset, length);
        char[] v = StringCoding.decode(charsetName, bytes, offset, length);
        this.offset = 0;
        this.value = v;
        this.count = v.length;
    }

    /**
     * Returns a new String composed of copies of the CharSequence elements
     * joined together with a copy of the specified delimiter.
     */
    public static String join(CharSequence delimiter, CharSequence... elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        StringJoiner joiner = new StringJoiner(delimiter);
        for (CharSequence cs: elements) {
            joiner.add(cs);
        }
        return joiner.toString();
    }

    /**
     * Returns a new String composed of copies of the CharSequence elements
     * joined together with a copy of the specified delimiter.
     */
    public static String join(CharSequence delimiter,
                            Iterable<? extends CharSequence> elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        StringJoiner joiner = new StringJoiner(delimiter);
        for (CharSequence cs: elements) {
            joiner.add(cs);
        }
        return joiner.toString();
    }

    /**
     * Returns the length of this string.
     */
    public int length() {
        return count;
    }

    /**
     * Returns the character at the specified index.
     */
    public char charAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index + offset];
    }

    /**
     * Returns a substring of this string.
     */
    public String substring(int beginIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (beginIndex > count) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (beginIndex == 0) {
            return this;
        }
        return new String(offset + beginIndex, count - beginIndex, value);
    }

    /**
     * Returns a substring of this string.
     */
    public String substring(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > count) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (beginIndex > endIndex) {
            throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
        }
        if ((beginIndex == 0) && (endIndex == count)) {
            return this;
        }
        return new String(offset + beginIndex, endIndex - beginIndex, value);
    }

    /**
     * Concatenates the specified string to the end of this string.
     */
    public String concat(String str) {
        int otherLen = str.length();
        if (otherLen == 0) {
            return this;
        }
        char[] v1 = value;
        int offset1 = offset;
        char[] v2 = str.value;
        int offset2 = str.offset;
        int count1 = count;
        int count2 = str.count;
        char[] v = new char[count1 + count2];
        System.arraycopy(v1, offset1, v, 0, count1);
        System.arraycopy(v2, offset2, v, count1, count2);
        return new String(0, count1 + count2, v);
    }

    /**
     * Returns true if and only if this string contains the specified
     * sequence of char values.
     */
    public boolean contains(CharSequence s) {
        return indexOf(s.toString()) > -1;
    }

    /**
     * Returns the index within this string of the first occurrence of
     * the specified character.
     */
    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of
     * the specified character, starting the search at the specified index.
     */
    public native int indexOf(int ch, int fromIndex);

    /**
     * Returns the index within this string of the first occurrence of
     * the specified substring.
     */
    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     */
    public native int indexOf(String str, int fromIndex);

    /**
     * Returns a hash code for this string.
     */
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            int off = offset;
            char[] val = value;
            int len = count;

            for (int i = 0; i < len; i++) {
                h = 31*h + val[off++];
            }
            hash = h;
        }
        return h;
    }

    /**
     * Compares this string to the specified object.
     */
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = count;
            if (n == anotherString.count) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = offset;
                int j = anotherString.offset;
                while (n-- != 0) {
                    if (v1[i++] != v2[j++])
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Compares this String to another String, ignoring case considerations.
     */
    public boolean equalsIgnoreCase(String anotherString) {
        return (this == anotherString) ? true :
               (anotherString != null) &&
               (anotherString.count == count) &&
               regionMatches(true, 0, anotherString, 0, count);
    }

    /**
     * Compares two strings lexicographically.
     */
    public int compareTo(String anotherString) {
        int len1 = count;
        int len2 = anotherString.count;
        int n = Math.min(len1, len2);
        char v1[] = value;
        char v2[] = anotherString.value;
        int i = offset;
        int j = anotherString.offset;

        if (i == j) {
            while (n-- != 0) {
                char c1 = v1[i++];
                char c2 = v2[j++];
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
        } else {
            while (n-- != 0) {
                char c1 = v1[i++];
                char c2 = v2[j++];
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
        }
        return len1 - len2;
    }

    /**
     * Returns a string resulting from replacing all occurrences of
     * oldChar in this string with newChar.
     */
    public String replace(char oldChar, char newChar) {
        if (oldChar != newChar) {
            int len = count;
            int i = -1;
            char[] val = value;
            int off = offset;

            while (++i < len) {
                if (val[off + i] == oldChar) {
                    break;
                }
            }
            if (i < len) {
                char c[] = new char[len];
                System.arraycopy(val, off, c, 0, i);
                while (i < len) {
                    c[i] = (val[off + i] == oldChar) ? newChar : val[off + i];
                    i++;
                }
                return new String(0, len, c);
            }
        }
        return this;
    }

    /**
     * Returns a new string resulting from replacing all occurrences of
     * target in this string with replacement.
     */
    public String replace(CharSequence target, CharSequence replacement) {
        return Pattern.compile(Pattern.quote(target.toString()), Pattern.LITERAL).
            matcher(this).replaceAll(Matcher.quoteReplacement(replacement.toString()));
    }

    /**
     * Returns a string resulting from replacing all occurrences of
     * target in this string with replacement.
     */
    public String replaceAll(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceAll(replacement);
    }

    /**
     * Returns a string resulting from replacing the first occurrence of
     * target in this string with replacement.
     */
    public String replaceFirst(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceFirst(replacement);
    }

    /**
     * Splits this string around matches of the given regular expression.
     */
    public String[] split(String regex, int limit) {
        return Pattern.compile(regex).split(this, limit);
    }

    /**
     * Splits this string around matches of the given regular expression.
     */
    public String[] split(String regex) {
        return split(regex, 0);
    }
}

