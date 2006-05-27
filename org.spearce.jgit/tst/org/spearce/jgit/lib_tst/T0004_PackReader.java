package org.spearce.jgit.lib_tst;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectReader;
import org.spearce.jgit.lib.PackReader;
import org.spearce.jgit.lib.PackedObjectReader;

public class T0004_PackReader extends RepositoryTestCase {
    private static final File TEST_PACK = new File(new File("tst"),
            "pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f.pack");

    public void test001_scanPack() throws IOException {
        final PackReader pr;
        final Iterator itr;

        pr = new PackReader(r, new FileInputStream(TEST_PACK));
        itr = pr.iterator();
        while (itr.hasNext()) {
            final PackedObjectReader r = (PackedObjectReader) itr.next();
            if (r.getDeltaBaseId() != null) {
                // A deltified object can't get to its base in an unseekable
                // stream so we can't get its Id or Type.
                //
                System.out.println("DELTA base: " + r.getDeltaBaseId());
            } else {
                // Asking for the Id of a packed object will read the object
                // once, properly absorbing its data from the input stream.
                //
                System.out.println("ENTRY " + r.getType() + " " + r.getId());
            }
        }
        pr.close();
    }

    public void test002_cannotSeek() throws IOException {
        final PackReader pr;
        final ObjectId id;
        boolean pass = false;

        id = new ObjectId("902d5476fa249b7abc9d84c611577a81381f0327");
        pr = new PackReader(r, new FileInputStream(TEST_PACK));
        try {
            pr.get(id);
        } catch (IOException ioe) {
            pass = true;
        }
        assertTrue("get on unseekable stream throws exception.", pass);
        pr.close();
    }

    public void test003_lookupCompressedObject() throws IOException {
        final PackReader pr;
        final ObjectId id;
        final PackedObjectReader or;

        id = new ObjectId("902d5476fa249b7abc9d84c611577a81381f0327");
        pr = new PackReader(r, TEST_PACK);
        or = pr.get(id);
        assertNotNull(or);
        assertEquals(id, or.getId());
        assertEquals(Constants.TYPE_TREE, or.getType());
        assertEquals(35, or.getSize());
        assertEquals(7738, or.getDataOffset());
        assertNull(or.getDeltaBaseId());
        or.close();
        pr.close();
    }

    public void test004_lookupDeltifiedObject() throws IOException {
        final ObjectId id;
        final ObjectId base;
        final ObjectReader or;

        id = new ObjectId("5b6e7c66c276e7610d4a73c70ec1a1f7c1003259");
        base = new ObjectId("6ff87c4664981e4397625791c8ea3bbb5f2279a3");
        or = r.openObject(id);
        assertNotNull(or);
        assertTrue(or instanceof PackedObjectReader);
        assertEquals(base, ((PackedObjectReader) or).getDeltaBaseId());
        assertEquals(id, or.getId());
        assertEquals(Constants.TYPE_BLOB, or.getType());
        assertEquals(16512, or.getSize());
        assertEquals(537, ((PackedObjectReader) or).getDataOffset());
        or.close();
    }
}
