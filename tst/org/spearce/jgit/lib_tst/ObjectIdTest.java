package org.spearce.jgit.lib_tst;

import junit.framework.TestCase;

import org.spearce.jgit.lib.ObjectId;

public class ObjectIdTest extends TestCase {
    public void test1() {
        final String x = "def4c620bc3713bb1bb26b808ec9312548e73946";
        final ObjectId oid = new ObjectId(x);
        assertEquals(x, oid.toString());
    }

    public void test2() {
        final String x = "ff00eedd003713bb1bb26b808ec9312548e73946";
        final ObjectId oid = new ObjectId(x);
        assertEquals(x, oid.toString());
    }

    public void testEquals() {
        final String x = "def4c620bc3713bb1bb26b808ec9312548e73946";
        final ObjectId a = new ObjectId(x);
        final ObjectId b = new ObjectId(x);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.equals(b));
    }
}
