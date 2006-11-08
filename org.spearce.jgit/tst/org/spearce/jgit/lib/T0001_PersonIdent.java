/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

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
