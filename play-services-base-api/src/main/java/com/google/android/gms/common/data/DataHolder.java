/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.common.data;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.CursorWindow;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for accessing collections of data, organized into columns. This provides the backing
 * support for DataBuffer. Much like a cursor, the holder supports the notion of a current
 * position, and has methods for extracting various types of data from named columns.
 */
@PublicApi(until = "1")
public class DataHolder extends AutoSafeParcelable implements Closeable {
    @SafeParceled(1)
    private final String[] columns;

    @SafeParceled(2)
    private final CursorWindow[] windows;

    @SafeParceled(3)
    private final int statusCode;

    @SafeParceled(4)
    private final Bundle metadata;

    private boolean closed = false;
    private Map<String, Integer> columnIndices;

    protected static final int FIELD_TYPE_NULL = 0;
    protected static final int FIELD_TYPE_INTEGER = 1;
    protected static final int FIELD_TYPE_FLOAT = 2;
    protected static final int FIELD_TYPE_STRING = 3;
    protected static final int FIELD_TYPE_BLOB = 4;

    /**
     * Creates a data holder with the specified data.
     *
     * @param columns    The column names corresponding to the data in the given windows.
     * @param windows    The {@link CursorWindow} instances holding the data.
     * @param statusCode The status code of this {@link DataHolder}.
     * @param metadata   The metadata associated with this {@link DataHolder} (may be null).
     */
    public DataHolder(String[] columns, CursorWindow[] windows, int statusCode, Bundle metadata) {
        this.columns = columns;
        this.windows = windows;
        this.statusCode = statusCode;
        this.metadata = metadata;
        validateContents();
    }

    /**
     * Creates a data holder wrapping the provided cursor, with provided status code and metadata.
     *
     * @param cursor     The cursor containing the data.
     * @param statusCode The status code of this {@link DataHolder}.
     * @param metadata   The metadata associated with this {@link DataHolder} (may be null).
     */
    public DataHolder(Cursor cursor, int statusCode, Bundle metadata) {
        this(cursor.getColumnNames(), createCursorWindows(cursor), statusCode, metadata);
    }

    /**
     * Get a {@link DataHolder.Builder} to create a new {@link DataHolder} manually.
     *
     * @param columns      The array of column names that the object supports.
     * @param uniqueColumn The non-null column name that must contain unique values. New rows added to the builder with the same value in this column will replace any older rows.
     * @return {@link DataHolder.Builder} object to work with.
     */
    public static Builder builder(String[] columns, String uniqueColumn) {
        return new Builder(columns, uniqueColumn);
    }

    /**
     * Get a {@link DataHolder.Builder} to create a new {@link DataHolder} manually.
     *
     * @param columns The array of column names that the object supports.
     * @return {@link DataHolder.Builder} object to work with.
     */
    public static Builder builder(String[] columns) {
        return builder(columns, null);
    }

    @SuppressLint({"NewApi", "ObsoleteSdkInt"})
    static int getCursorType(Cursor cursor, int i) {
        return cursor.getType(i);
    }

    /**
     * Closes the data holder, releasing all of its resources and making it completely invalid.
     */
    @Override
    public void close() {
        synchronized (this) {
            if (!closed) {
                closed = true;
                for (CursorWindow window : windows) {
                    window.close();
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static CursorWindow[] createCursorWindows(Builder builder) {
        if (builder.columns.length == 0) return new CursorWindow[0];
        List<CursorWindow> windows = new ArrayList<>();
        try {
            CursorWindow current = null;
            for (int rowIndex = 0; rowIndex < builder.rows.size(); rowIndex++) {
                Map<String, Object> row = builder.rows.get(rowIndex);
                if (current == null || !current.allocRow()) {
                    current = new CursorWindow(false);
                    current.setStartPosition(rowIndex);
                    current.setNumColumns(builder.columns.length);
                    windows.add(current);
                    if (!current.allocRow()) {
                        windows.remove(current);
                        return windows.toArray(new CursorWindow[0]);
                    }
                }
                for (int columnIndex = 0; columnIndex < builder.columns.length; columnIndex++) {
                    Object val = row.get(builder.columns[columnIndex]);
                    if (val == null) {
                        current.putNull(rowIndex, columnIndex);
                    } else if (val instanceof String) {
                        current.putString((String) val, rowIndex, columnIndex);
                    } else if (val instanceof Long) {
                        current.putLong((Long) val, rowIndex, columnIndex);
                    } else if (val instanceof Integer) {
                        current.putLong((Integer) val, rowIndex, columnIndex);
                    } else if (val instanceof Boolean) {
                        if ((Boolean) val)
                            current.putLong(1, rowIndex, columnIndex);
                    } else if (val instanceof byte[]) {
                        current.putBlob((byte[]) val, rowIndex, columnIndex);
                    } else if (val instanceof Double) {
                        current.putDouble((Double) val, rowIndex, columnIndex);
                    } else if (val instanceof Float) {
                        current.putDouble((Float) val, rowIndex, columnIndex);
                    } else {
                        throw new IllegalArgumentException("Unsupported object for column " + columnIndex + ": " + val);
                    }
                }
            }
        } catch (RuntimeException e) {
            for (CursorWindow window : windows) {
                window.close();
            }
            throw e;
        }
        return windows.toArray(new CursorWindow[0]);
    }

    private static CursorWindow[] createCursorWindows(Cursor cursor) {
        if (cursor.getColumnCount() == 0) return new CursorWindow[0];
        List<CursorWindow> windows = new ArrayList<>();
        CursorWindow current = null;
        int rowIndex = 0;
        while (cursor.moveToNext()) {
            if (current == null || !current.allocRow()) {
                current = new CursorWindow(false);
                current.setStartPosition(rowIndex);
                current.setNumColumns(cursor.getColumnCount());
                windows.add(current);
                if (!current.allocRow()) {
                    windows.remove(current);
                    return windows.toArray(new CursorWindow[0]);
                }
            }
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                switch (getCursorType(cursor, i)) {
                    case FIELD_TYPE_NULL:
                        current.putNull(rowIndex, i);
                        break;
                    case FIELD_TYPE_BLOB:
                        current.putBlob(cursor.getBlob(i), rowIndex, i);
                        break;
                    case FIELD_TYPE_FLOAT:
                        current.putDouble(cursor.getDouble(i), rowIndex, i);
                        break;
                    case FIELD_TYPE_INTEGER:
                        current.putLong(cursor.getLong(i), rowIndex, i);
                        break;
                    case FIELD_TYPE_STRING:
                        current.putString(cursor.getString(i), rowIndex, i);
                        break;
                }
            }
            rowIndex++;
        }
        cursor.close();
        return windows.toArray(new CursorWindow[0]);
    }

    /**
     * Gets the number of rows in the data holder.
     *
     * @return the number of rows in the data holder.
     */
    public int getCount() {
        int c = 0;
        if (windows != null) {
            for (CursorWindow window : windows) {
                c += window.getNumRows();
            }
        }
        return c;
    }

    /**
     * Retrieves the string value for a given column at the provided position.
     * This will throw an {@link IllegalArgumentException} if the column does not exist, the
     * position is invalid, or the data holder has been closed.
     *
     * @param column      The column to retrieve.
     * @param row         The row to retrieve the data from.
     * @param windowIndex Index of the cursor window to extract the data from.
     * @return The string value in that column.
     */
    public String getString(String column, int row, int windowIndex) {
        return windows[windowIndex].getString(row, columnIndices.get(column));
    }

    @NonNull
    @Override
    public String toString() {
        return "DataHolder{" +
                "columns=" + Arrays.toString(columns) +
                ", windows=" + Arrays.toString(windows) +
                ", statusCode=" + statusCode +
                ", metadata=" + metadata +
                '}';
    }

    public void validateContents() {
        columnIndices = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnIndices.put(columns[i], i);
        }
    }

    /**
     * Helper class to build {@link DataHolder} instances containing arbitrary data.
     * <p/>
     * Note that the constructor is private; use DataHolder.builder() to create instances of this class.
     */
    public static class Builder {
        private final String[] columns;
        private final ArrayList<Map<String, Object>> rows;
        private final Map<Object, Integer> uniqueIndizes;

        private Builder(String[] columns, String uniqueColumn) {
            this.columns = columns;
            this.rows = new ArrayList<>();
            this.uniqueIndizes = new HashMap<>();
        }

        /**
         * Instantiate an {@link DataHolder} from this {@link DataHolder.Builder} with the given status code and metadata.
         *
         * @param statusCode The status code of this {@link DataHolder}.
         * @param metadata   The metadata associated with this {@link DataHolder} (may be null).
         * @return {@link DataHolder} representation of this object.
         */
        public DataHolder build(int statusCode, Bundle metadata) {
            return new DataHolder(columns, createCursorWindows(this), statusCode, metadata);
        }

        /**
         * Instantiate an {@link DataHolder} from this {@link DataHolder.Builder} with the given status code and null metadata.
         *
         * @param statusCode The status code of this {@link DataHolder}.
         * @return {@link DataHolder} representation of this object.
         */
        public DataHolder build(int statusCode) {
            return build(statusCode, null);
        }

        /**
         * @return The number of rows that the resulting DataHolder will contain.
         */
        public int getCount() {
            return rows.size();
        }

    }

    public static final Creator<DataHolder> CREATOR = new AutoCreator<DataHolder>(DataHolder.class) {
        @Override
        public DataHolder createFromParcel(Parcel parcel) {
            DataHolder res = super.createFromParcel(parcel);
            res.validateContents();
            return res;
        }
    };
}
