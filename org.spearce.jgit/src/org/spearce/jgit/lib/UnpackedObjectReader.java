/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.spearce.jgit.errors.CorruptObjectException;

public class UnpackedObjectReader extends ObjectReader
{
    private static final int MAX_TYPE_LEN = 16;

    private final String objectType;

    private final long objectSize;

    private InflaterInputStream inflater;

    public UnpackedObjectReader(final ObjectId id, final XInputStream src)
        throws IOException
    {
        long tempSize = 0;
        int byte1, byte2, word1;

        setId(id);

        // Try to determine if this is a legacy format loose object or
        // a new style loose object. The legacy format was completely
        // compressed with zlib so the first byte must be 0x78 (15-bit
        // window size, deflated) and the first 16 bit word must be
        // evenly divisible by 31. Otherwise its a new style loose object.
        //
        src.mark(2);
        byte1 = src.readUInt8();
        byte2 = src.readUInt8();
        word1 = (byte1 << 8) | byte2;
        if (byte1 == 0x78 && (word1 % 31) == 0)
        {
            final StringBuffer typeBuf = new StringBuffer(MAX_TYPE_LEN);
            final String typeStr;

            src.reset();
            inflater = new InflaterInputStream(src);

            for (;;)
            {
                final int c = inflater.read();
                if (' ' == c)
                {
                    break;
                }
                else if (c < 'a' || c > 'z' || typeBuf.length() >= MAX_TYPE_LEN)
                {
                    throw new CorruptObjectException(id, "bad type in header");
                }
                typeBuf.append((char) c);
            }

            typeStr = typeBuf.toString();
            if (Constants.TYPE_BLOB.equals(typeStr))
            {
                objectType = Constants.TYPE_BLOB;
            }
            else if (Constants.TYPE_TREE.equals(typeStr))
            {
                objectType = Constants.TYPE_TREE;
            }
            else if (Constants.TYPE_COMMIT.equals(typeStr))
            {
                objectType = Constants.TYPE_COMMIT;
            }
            else if (Constants.TYPE_TAG.equals(typeStr))
            {
                objectType = Constants.TYPE_TAG;
            }
            else
            {
                throw new CorruptObjectException(id, "invalid type: " + typeStr);
            }

            for (;;)
            {
                final int c = inflater.read();
                if (0 == c)
                {
                    break;
                }
                else if (c < '0' || c > '9')
                {
                    throw new CorruptObjectException(id, "bad length in header");
                }
                tempSize *= 10;
                tempSize += c - '0';
            }
        }
        else
        {
            int typeCode, c, shift;
            long size;

            src.reset();
            c = src.readUInt8();
            typeCode = (c >> 4) & 7;
            size = c & 15;
            shift = 4;
            while ((c & 0x80) != 0)
            {
                c = src.readUInt8();
                size += (c & 0x7f) << shift;
                shift += 7;
            }

            switch (typeCode)
            {
            case Constants.OBJ_EXT:
                throw new CorruptObjectException(
                    id,
                    "Extended object types not supported.");
            case Constants.OBJ_COMMIT:
                objectType = Constants.TYPE_COMMIT;
                break;
            case Constants.OBJ_TREE:
                objectType = Constants.TYPE_TREE;
                break;
            case Constants.OBJ_BLOB:
                objectType = Constants.TYPE_BLOB;
                break;
            case Constants.OBJ_TAG:
                objectType = Constants.TYPE_TAG;
                break;
            case Constants.OBJ_TYPE_5:
                throw new CorruptObjectException(
                    id,
                    "Object type 5 not supported.");
            case Constants.OBJ_TYPE_6:
                throw new CorruptObjectException(
                    id,
                    "Object type 6 not supported.");
            case Constants.OBJ_DELTA:
                throw new CorruptObjectException(
                    id,
                    "Delta in loose object not supported.");
            default:
                throw new CorruptObjectException(id, "Unknown object type "
                    + typeCode
                    + ".");
            }

            inflater = new InflaterInputStream(src);
        }

        objectSize = tempSize;
    }

    public String getType()
    {
        return objectType;
    }

    public long getSize()
    {
        return objectSize;
    }

    public InputStream getInputStream()
    {
        if (inflater == null)
        {
            throw new IllegalStateException("Already closed.");
        }
        return inflater;
    }

    public void close() throws IOException
    {
        if (inflater != null)
        {
            inflater.close();
            inflater = null;
        }
    }
}
