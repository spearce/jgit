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
