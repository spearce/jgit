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
package org.spearce.jgit.lib_tst;

import junit.framework.TestCase;

import org.spearce.jgit.lib.ObjectId;

public class T0001_ObjectId extends TestCase
{
    public void test001_toString()
    {
        final String x = "def4c620bc3713bb1bb26b808ec9312548e73946";
        final ObjectId oid = new ObjectId(x);
        assertNotNull("has bytes", oid.getBytes());
        assertEquals(20, oid.getBytes().length);
        assertEquals(x, oid.toString());
    }

    public void test002_toString()
    {
        final String x = "ff00eedd003713bb1bb26b808ec9312548e73946";
        final ObjectId oid = new ObjectId(x);
        assertEquals(x, oid.toString());
    }

    public void test003_equals()
    {
        final String x = "def4c620bc3713bb1bb26b808ec9312548e73946";
        final ObjectId a = new ObjectId(x);
        final ObjectId b = new ObjectId(x);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue("a and b are same", a.equals(b));
    }

    public void test004_isId()
    {
        assertTrue("valid id", ObjectId
            .isId("def4c620bc3713bb1bb26b808ec9312548e73946"));
    }

    public void test005_notIsId()
    {
        assertFalse("bob is not an id", ObjectId.isId("bob"));
    }

    public void test006_notIsId()
    {
        assertFalse("39 digits is not an id", ObjectId
            .isId("def4c620bc3713bb1bb26b808ec9312548e7394"));
    }

    public void test007_notIsId()
    {
        assertFalse("uppercase is not accepted", ObjectId
            .isId("Def4c620bc3713bb1bb26b808ec9312548e73946"));
    }

    public void test008_notIsId()
    {
        assertFalse("g is not a valid hex digit", ObjectId
            .isId("gef4c620bc3713bb1bb26b808ec9312548e73946"));
    }

    public void test009_toString()
    {
        final String x = "ff00eedd003713bb1bb26b808ec9312548e73946";
        final ObjectId oid = new ObjectId(x);
        assertEquals(x, ObjectId.toString(oid));
    }

    public void test010_toString()
    {
        final String x = "0000000000000000000000000000000000000000";
        assertEquals(x, ObjectId.toString(null));
    }
}
