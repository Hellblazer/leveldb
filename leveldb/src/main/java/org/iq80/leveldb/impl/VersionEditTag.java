/**
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.iq80.leveldb.impl;

import com.google.common.base.Charsets;
import org.iq80.leveldb.util.SliceInput;
import org.iq80.leveldb.util.VariableLengthQuantity;
import org.iq80.leveldb.util.SliceOutput;

import java.util.Map.Entry;

import static org.iq80.leveldb.util.Slices.readLengthPrefixedBytes;
import static org.iq80.leveldb.util.Slices.writeLengthPrefixedBytes;

public enum VersionEditTag
{
    COMPARATOR(1)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    byte[] bytes = new byte[VariableLengthQuantity.unpackInt(sliceInput)];
                    sliceInput.readBytes(bytes);
                    versionEdit.setComparatorName(new String(bytes, Charsets.UTF_8));
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    String comparatorName = versionEdit.getComparatorName();
                    if (comparatorName != null) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);
                        byte[] bytes = comparatorName.getBytes(Charsets.UTF_8);
                        VariableLengthQuantity.packInt(bytes.length, sliceOutput);
                        sliceOutput.writeBytes(bytes);
                    }
                }
            },
    LOG_NUMBER(2)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    versionEdit.setLogNumber(VariableLengthQuantity.unpackLong(sliceInput));
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    Long logNumber = versionEdit.getLogNumber();
                    if (logNumber != null) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);
                        VariableLengthQuantity.packLong(logNumber, sliceOutput);
                    }
                }
            },

    NEXT_FILE_NUMBER(3)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    versionEdit.setNextFileNumber(VariableLengthQuantity.unpackLong(sliceInput));
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    Long nextFileNumber = versionEdit.getNextFileNumber();
                    if (nextFileNumber != null) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);
                        VariableLengthQuantity.packLong(nextFileNumber, sliceOutput);
                    }
                }
            },

    LAST_SEQUENCE(4)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    versionEdit.setLastSequenceNumber(VariableLengthQuantity.unpackLong(sliceInput));
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    Long lastSequenceNumber = versionEdit.getLastSequenceNumber();
                    if (lastSequenceNumber != null) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);
                        VariableLengthQuantity.packLong(lastSequenceNumber, sliceOutput);
                    }
                }
            },

    COMPACT_POINTER(5)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    // level
                    int level = VariableLengthQuantity.unpackInt(sliceInput);

                    // internal key
                    InternalKey internalKey = new InternalKey(readLengthPrefixedBytes(sliceInput));

                    versionEdit.setCompactPointer(level, internalKey);
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    for (Entry<Integer, InternalKey> entry : versionEdit.getCompactPointers().entrySet()) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);

                        // level
                        VariableLengthQuantity.packInt(entry.getKey(), sliceOutput);

                        // internal key
                        writeLengthPrefixedBytes(sliceOutput, entry.getValue().encode());
                    }
                }
            },

    DELETED_FILE(6)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    // level
                    int level = VariableLengthQuantity.unpackInt(sliceInput);

                    // file number
                    long fileNumber = VariableLengthQuantity.unpackLong(sliceInput);

                    versionEdit.deleteFile(level, fileNumber);
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    for (Entry<Integer, Long> entry : versionEdit.getDeletedFiles().entries()) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);

                        // level
                        VariableLengthQuantity.packInt(entry.getKey(), sliceOutput);

                        // file number
                        VariableLengthQuantity.packLong(entry.getValue(), sliceOutput);
                    }
                }
            },

    NEW_FILE(7)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    // level
                    int level = VariableLengthQuantity.unpackInt(sliceInput);

                    // file number
                    long fileNumber = VariableLengthQuantity.unpackLong(sliceInput);

                    // file size
                    long fileSize = VariableLengthQuantity.unpackLong(sliceInput);

                    // smallest key
                    InternalKey smallestKey = new InternalKey(readLengthPrefixedBytes(sliceInput));

                    // largest key
                    InternalKey largestKey = new InternalKey(readLengthPrefixedBytes(sliceInput));

                    versionEdit.addFile(level, fileNumber, fileSize, smallestKey, largestKey);
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    for (Entry<Integer, FileMetaData> entry : versionEdit.getNewFiles().entries()) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);

                        // level
                        VariableLengthQuantity.packInt(entry.getKey(), sliceOutput);


                        // file number
                        FileMetaData fileMetaData = entry.getValue();
                        VariableLengthQuantity.packLong(fileMetaData.getNumber(), sliceOutput);

                        // file size
                        VariableLengthQuantity.packLong(fileMetaData.getFileSize(), sliceOutput);

                        // smallest key
                        writeLengthPrefixedBytes(sliceOutput, fileMetaData.getSmallest().encode());

                        // smallest key
                        writeLengthPrefixedBytes(sliceOutput, fileMetaData.getLargest().encode());
                    }
                }
            },

    // 8 was used for large value refs

    PREVIOUS_LOG_NUMBER(9)
            {
                @Override
                public void readValue(SliceInput sliceInput, VersionEdit versionEdit)
                {
                    long previousLogNumber = VariableLengthQuantity.unpackLong(sliceInput);
                    versionEdit.setPreviousLogNumber(previousLogNumber);
                }

                @Override
                public void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit)
                {
                    Long previousLogNumber = versionEdit.getPreviousLogNumber();
                    if (previousLogNumber != null) {
                        VariableLengthQuantity.packInt(getPersistentId(), sliceOutput);
                        VariableLengthQuantity.packLong(previousLogNumber, sliceOutput);
                    }
                }
            },;

    public static VersionEditTag getValueTypeByPersistentId(int persistentId)
    {
        for (VersionEditTag compressionType : VersionEditTag.values()) {
            if (compressionType.persistentId == persistentId) {
                return compressionType;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown %s persistentId %d", VersionEditTag.class.getSimpleName(), persistentId));
    }

    private final int persistentId;

    VersionEditTag(int persistentId)
    {
        this.persistentId = persistentId;
    }

    public int getPersistentId()
    {
        return persistentId;
    }

    public abstract void readValue(SliceInput sliceInput, VersionEdit versionEdit);

    public abstract void writeValue(SliceOutput sliceOutput, VersionEdit versionEdit);
}
