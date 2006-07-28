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

import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.spearce.jgit.lib.PersonIdent;

public class T0001_PersonIdent extends TestCase
{
    public void test001_NewIdent()
    {
        final PersonIdent p = new PersonIdent(
            "A U Thor",
            "author@example.com",
            new Date(1142878501000L),
            TimeZone.getTimeZone("EST"));
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
        assertEquals("A U Thor <author@example.com> 1142878501 -0500", p
            .toExternalString());
    }

    public void test002_ParseIdent()
    {
        final String i = "A U Thor <author@example.com> 1142878501 -0500";
        final PersonIdent p = new PersonIdent(i);
        assertEquals(i, p.toExternalString());
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
    }

    public void test003_ParseIdent()
    {
        final String i = "A U Thor <author@example.com> 1142878501 +0230";
        final PersonIdent p = new PersonIdent(i);
        assertEquals(i, p.toExternalString());
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
    }

    public void test004_ParseIdent()
    {
        final String i = "A U Thor<author@example.com> 1142878501 +0230";
        final PersonIdent p = new PersonIdent(i);
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
    }

    public void test005_ParseIdent()
    {
        final String i = "A U Thor<author@example.com>1142878501 +0230";
        final PersonIdent p = new PersonIdent(i);
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
    }

    public void test006_ParseIdent()
    {
        final String i = "A U Thor   <author@example.com>1142878501 +0230";
        final PersonIdent p = new PersonIdent(i);
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
    }

    public void test007_ParseIdent()
    {
        final String i = "A U Thor<author@example.com>1142878501 +0230 ";
        final PersonIdent p = new PersonIdent(i);
        assertEquals("A U Thor", p.getName());
        assertEquals("author@example.com", p.getEmailAddress());
        assertEquals(1142878501000L, p.getWhen().getTime());
    }
}
