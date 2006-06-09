package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class PackReader
{
    private static final byte[] SIGNATURE = {'P', 'A', 'C', 'K'};

    private static final int IDX_HDR_LEN = 256 * 4;

    private static final int OBJ_EXT = 0;

    private static final int OBJ_COMMIT = 1;

    private static final int OBJ_TREE = 2;

    private static final int OBJ_BLOB = 3;

    private static final int OBJ_TAG = 4;

    private static final int OBJ_TYPE_5 = 5;

    private static final int OBJ_TYPE_6 = 6;

    private static final int OBJ_DELTA = 7;

    private final Repository repo;

    private final XInputStream packStream;

    private final XInputStream idxStream;

    private final long[] idxHeader;

    private long objectCnt;

    private int lastRead;

    public PackReader(final Repository parentRepo, final InputStream ps)
        throws IOException
    {
        repo = parentRepo;
        idxStream = null;
        idxHeader = null;

        packStream = new XInputStream(ps);
        try
        {
            readPackHeader();
        }
        catch (IOException ioe)
        {
            packStream.close();
            throw ioe;
        }
    }

    public PackReader(final Repository parentRepo, final File packFile)
        throws IOException
    {
        final String name = packFile.getName();
        final int dot = name.lastIndexOf('.');

        repo = parentRepo;
        packStream = new XInputStream(new FileInputStream(packFile));
        try
        {
            readPackHeader();
        }
        catch (IOException err)
        {
            packStream.close();
            throw err;
        }

        try
        {
            final File idxFile = new File(packFile.getParentFile(), name
                .substring(0, dot)
                + ".idx");
            if (idxFile.length() != (IDX_HDR_LEN + (24 * objectCnt) + (2 * Constants.OBJECT_ID_LENGTH)))
            {
                throw new CorruptObjectException("Pack index "
                    + idxFile.getName()
                    + " has incorrect file size.");
            }

            idxStream = new XInputStream(new FileInputStream(idxFile));
            idxHeader = new long[256];
            for (int k = 0; k < idxHeader.length; k++)
            {
                idxHeader[k] = idxStream.xuint32();
            }
        }
        catch (IOException ioe)
        {
            packStream.close();
            throw ioe;
        }
    }

    public Iterator iterator()
    {
        try
        {
            packStream.position(SIGNATURE.length + 2 * 4);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("Can't iterate entries.", ioe);
        }

        return new Iterator()
        {
            private long current = 0;

            private PackedObjectReader last;

            public boolean hasNext()
            {
                return current < objectCnt;
            }

            public Object next()
            {
                if (!hasNext())
                {
                    throw new IllegalStateException("No more items.");
                }

                try
                {
                    // If the caller did not absorb the object data then we
                    // must do so now otherwise we will try to parse zipped
                    // data as though it were an object header.
                    //
                    if (last != null
                        && last.getDataOffset() == packStream.position())
                    {
                        final Inflater inf = new Inflater();
                        try
                        {
                            final byte[] input = new byte[1024];
                            final byte[] output = new byte[1024];
                            while (!inf.finished())
                            {
                                if (inf.needsInput())
                                {
                                    packStream.mark(input.length);
                                    lastRead = packStream.read(input);
                                    inf.setInput(input, 0, lastRead);
                                }
                                inf.inflate(output);
                            }
                            unread(inf.getRemaining());
                        }
                        catch (DataFormatException dfe)
                        {
                            throw new RuntimeException("Cannot absorb packed"
                                + " object data as the pack is corrupt.", dfe);
                        }
                        finally
                        {
                            inf.end();
                        }
                    }

                    current++;
                    last = reader();
                    return last;
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Can't read next pack entry.", e);
                }
            }

            public void remove()
            {
                throw new UnsupportedOperationException("Remove pack entry.");
            }
        };
    }

    public synchronized ObjectReader resolveBase(final ObjectId id)
        throws IOException
    {
        return repo.openObject(id);
    }

    public synchronized PackedObjectReader get(final ObjectId id)
        throws IOException
    {
        final long offset;
        final PackedObjectReader objReader;

        offset = findOffset(id);
        if (offset == -1)
        {
            return null;
        }

        packStream.position(offset);
        objReader = reader();
        objReader.setId(id);
        return objReader;
    }

    public synchronized void close() throws IOException
    {
        packStream.close();
        if (idxStream != null)
        {
            idxStream.close();
        }
    }

    int read(final long offset, final byte[] b, final int off, final int len)
        throws IOException
    {
        packStream.position(offset);
        packStream.mark(len);
        lastRead = packStream.read(b, off, len);
        return lastRead;
    }

    void unread(final int len) throws IOException
    {
        packStream.reset();
        packStream.skip(lastRead - len);
    }

    private void readPackHeader() throws IOException
    {
        final byte[] sig;
        final long vers;

        sig = packStream.xread(SIGNATURE.length);
        if (ObjectId.compare(sig, SIGNATURE) != 0)
        {
            throw new IOException("Not a PACK file.");
        }

        vers = packStream.xuint32();
        if (vers != 2 && vers != 3)
        {
            throw new IOException("Unsupported pack version " + vers + ".");
        }

        objectCnt = packStream.xuint32();
    }

    private PackedObjectReader reader() throws IOException
    {
        final int typeCode;
        final String typeStr;
        ObjectId deltaBase = null;
        final long offset;
        int c;
        long size;
        int shift;

        c = packStream.xuint8();
        typeCode = (c >> 4) & 7;
        size = c & 15;
        shift = 4;
        while ((c & 0x80) != 0)
        {
            c = packStream.xuint8();
            size += (c & 0x7f) << shift;
            shift += 7;
        }

        switch (typeCode)
        {
        case OBJ_EXT:
            throw new IOException("Extended object types not supported.");
        case OBJ_COMMIT:
            typeStr = Constants.TYPE_COMMIT;
            break;
        case OBJ_TREE:
            typeStr = Constants.TYPE_TREE;
            break;
        case OBJ_BLOB:
            typeStr = Constants.TYPE_BLOB;
            break;
        case OBJ_TAG:
            typeStr = Constants.TYPE_TAG;
            break;
        case OBJ_TYPE_5:
            throw new IOException("Object type 5 not supported.");
        case OBJ_TYPE_6:
            throw new IOException("Object type 6 not supported.");
        case OBJ_DELTA:
            typeStr = null;
            break;
        default:
            throw new IOException("Unknown object type " + typeCode + ".");
        }

        if (typeCode == OBJ_DELTA)
        {
            deltaBase = new ObjectId(packStream
                .xread(Constants.OBJECT_ID_LENGTH));
        }
        offset = packStream.position();
        return new PackedObjectReader(this, typeStr, size, offset, deltaBase);
    }

    private long findOffset(final ObjectId objId) throws IOException
    {
        int fi;
        long hi;
        long lo;

        if (idxHeader == null)
        {
            throw new IOException("Stream is not seekable.");
        }

        fi = objId.getBytes()[0];
        fi = ((fi >> 4) & 0xf) << 4 | (fi & 0xf);
        hi = idxHeader[fi];
        lo = fi == 0 ? 0 : idxHeader[fi - 1];
        do
        {
            final long mi = (lo + hi) / 2;
            final long offset;
            final int cmp;

            idxStream.position(IDX_HDR_LEN
                + ((4 + Constants.OBJECT_ID_LENGTH) * mi));
            offset = idxStream.xuint32();
            cmp = objId.compareTo(idxStream.xread(Constants.OBJECT_ID_LENGTH));
            if (cmp < 0)
            {
                lo = mi + 1;
            }
            else if (cmp == 0)
            {
                return offset;
            }
            else
            {
                hi = mi;
            }
        }
        while (lo < hi);
        return -1;
    }
}
