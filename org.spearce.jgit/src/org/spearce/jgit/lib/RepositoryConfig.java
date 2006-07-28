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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RepositoryConfig
{
    private final Repository repo;

    private final File configFile;

    private List entries;

    private Map byName;

    private Map lastInEntry;

    private Map lastInGroup;

    protected RepositoryConfig(final Repository r)
    {
        repo = r;
        configFile = new File(repo.getDirectory(), "config");
        clear();
    }

    public String getString(final String group, final String name)
    {
        final Object o;
        o = byName.get(group.toLowerCase() + "." + name.toLowerCase());
        if (o instanceof List)
        {
            return ((Entry) ((List) o).get(0)).value;
        }
        else if (o instanceof Entry)
        {
            return ((Entry) o).value;
        }
        else
        {
            return null;
        }
    }

    public void create()
    {
        Entry e;

        clear();

        e = new Entry();
        e.base = "core";
        add(e);

        e = new Entry();
        e.base = "core";
        e.name = "repositoryformatversion";
        e.value = "0";
        add(e);

        e = new Entry();
        e.base = "core";
        e.name = "filemode";
        e.value = "true";
        add(e);
    }

    public void save() throws IOException
    {
        final File tmp = new File(configFile.getParentFile(), configFile
            .getName()
            + ".lock");
        final PrintWriter r = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(tmp),
                Constants.CHARACTER_ENCODING)));
        boolean ok = false;
        try
        {
            final Iterator i = entries.iterator();
            while (i.hasNext())
            {
                final Entry e = (Entry) i.next();
                if (e.prefix != null)
                {
                    r.print(e.prefix);
                }
                if (e.base != null && e.name == null)
                {
                    r.print('[');
                    r.print(e.base);
                    if (e.extendedBase != null)
                    {
                        r.print(' ');
                        r.print('"');
                        r.print(escapeValue(e.extendedBase));
                        r.print('"');
                    }
                    r.print(']');
                }
                else if (e.base != null && e.name != null)
                {
                    if (e.prefix == null || "".equals(e.prefix))
                    {
                        r.print('\t');
                    }
                    r.print(e.name);
                    if (e.value != null)
                    {
                        r.print(" = ");
                        r.print(escapeValue(e.value));
                    }
                    if (e.suffix != null)
                    {
                        r.print(' ');
                    }
                }
                if (e.suffix != null)
                {
                    r.print(e.suffix);
                }
                r.println();
            }
            ok = true;
        }
        finally
        {
            r.close();
            if (!ok || !tmp.renameTo(configFile))
            {
                tmp.delete();
            }
        }
    }

    public void load() throws IOException
    {
        clear();
        final BufferedReader r = new BufferedReader(new InputStreamReader(
            new FileInputStream(configFile),
            Constants.CHARACTER_ENCODING));
        try
        {
            Entry last = null;
            Entry e = new Entry();
            for (;;)
            {
                r.mark(1);
                int input = r.read();
                final char in = (char) input;
                if (-1 == input)
                {
                    break;
                }
                else if ('\n' == in)
                {
                    // End of this entry.
                    add(e);
                    if (e.base != null)
                    {
                        last = e;
                    }
                    e = new Entry();
                }
                else if (e.suffix != null)
                {
                    // Everything up until the end-of-line is in the suffix.
                    e.suffix += in;
                }
                else if (';' == in || '#' == in)
                {
                    // The rest of this line is a comment; put into suffix.
                    e.suffix = String.valueOf(in);
                }
                else if (e.base == null && Character.isWhitespace(in))
                {
                    // Save the leading whitespace (if any).
                    if (e.prefix == null)
                    {
                        e.prefix = "";
                    }
                    e.prefix += in;
                }
                else if ('[' == in)
                {
                    // This is a group header line.
                    e.base = readBase(r);
                    input = r.read();
                    if ('"' == input)
                    {
                        e.extendedBase = readValue(r, true, '"');
                        input = r.read();
                    }
                    if (']' == input)
                    {
                        e.extendedBase = null;
                    }
                    else
                    {
                        throw new IOException("Bad group header.");
                    }
                    e.suffix = "";
                }
                else if (last != null)
                {
                    // Read a value.
                    e.base = last.base;
                    e.extendedBase = last.extendedBase;
                    r.reset();
                    e.name = readName(r);
                    e.value = readValue(r, false, -1);
                }
                else
                {
                    throw new IOException("Invalid line in config file.");
                }
            }
        }
        finally
        {
            r.close();
        }
    }

    private void clear()
    {
        entries = new ArrayList();
        byName = new HashMap();
        lastInEntry = new HashMap();
        lastInGroup = new HashMap();
    }

    private void add(final Entry e)
    {
        entries.add(e);
        if (e.base != null)
        {
            final String b = e.base.toLowerCase();
            final String group;
            if (e.extendedBase != null)
            {
                group = b + "." + e.extendedBase;
            }
            else
            {
                group = b;
            }
            if (e.name != null)
            {
                final String n = e.name.toLowerCase();
                final String key = group + "." + n;
                final Object o = byName.get(key);
                if (o == null)
                {
                    byName.put(key, e);
                }
                else if (o instanceof Entry)
                {
                    final ArrayList l = new ArrayList();
                    l.add(o);
                    l.add(e);
                    byName.put(key, l);
                }
                else if (o instanceof List)
                {
                    ((List) o).add(e);
                }
                lastInEntry.put(key, e);
            }
            lastInGroup.put(group, e);
        }
    }

    private static String escapeValue(final String x)
    {
        boolean inquote = false;
        int lineStart = 0;
        final StringBuffer r = new StringBuffer(x.length());
        for (int k = 0; k < x.length(); k++)
        {
            final char c = x.charAt(k);
            switch (c)
            {
            case '\n':
                if (inquote)
                {
                    r.append('"');
                    inquote = false;
                }
                r.append("\\n\\\n");
                lineStart = r.length();
                break;

            case '\t':
                r.append("\\t");
                break;

            case '\b':
                r.append("\\b");
                break;

            case '\\':
                r.append("\\\\");
                break;

            case '"':
                r.append("\\\"");
                break;

            case ';':
            case '#':
                if (!inquote)
                {
                    r.insert(lineStart, '"');
                    inquote = true;
                }
                r.append(c);
                break;

            case ' ':
                if (!inquote
                    && r.length() > 0
                    && r.charAt(r.length() - 1) == ' ')
                {
                    r.insert(lineStart, '"');
                    inquote = true;
                }
                r.append(' ');
                break;

            default:
                r.append(c);
                break;
            }
        }
        if (inquote)
        {
            r.append('"');
        }
        return r.toString();
    }

    private static String readBase(final BufferedReader r) throws IOException
    {
        final StringBuffer base = new StringBuffer();
        for (;;)
        {
            r.mark(1);
            int c = r.read();
            if (c < 0)
            {
                throw new IOException("Unexpected end of config file.");
            }
            else if (']' == c)
            {
                r.reset();
                break;
            }
            else if (' ' == c || '\t' == c)
            {
                for (;;)
                {
                    r.mark(1);
                    c = r.read();
                    if (c < 0)
                    {
                        throw new IOException("Unexpected end of config file.");
                    }
                    else if ('"' == c)
                    {
                        r.reset();
                        break;
                    }
                    else if (' ' == c || '\t' == c)
                    {
                        // Skipped...
                    }
                    else
                    {
                        throw new IOException("Bad base entry.");
                    }
                }
                break;
            }
            else if (Character.isLetterOrDigit((char) c) || '.' == c)
            {
                base.append((char) c);
            }
            else
            {
                throw new IOException("Bad base entry.");
            }
        }
        return base.toString();
    }

    private static String readName(final BufferedReader r) throws IOException
    {
        final StringBuffer name = new StringBuffer();
        for (;;)
        {
            int c = r.read();
            if (c < 0)
            {
                throw new IOException("Unexpected end of config file.");
            }
            else if ('=' == c)
            {
                break;
            }
            else if (' ' == c || '\t' == c)
            {
                for (;;)
                {
                    r.mark(1);
                    c = r.read();
                    if (c < 0)
                    {
                        throw new IOException("Unexpected end of config file.");
                    }
                    else if ('=' == c)
                    {
                        break;
                    }
                    else if (';' == c || '#' == c || '\n' == c)
                    {
                        r.reset();
                        break;
                    }
                    else if (' ' == c || '\t' == c)
                    {
                        // Skipped...
                    }
                    else
                    {
                        throw new IOException("Bad entry delimiter.");
                    }
                }
                break;
            }
            else if (Character.isLetterOrDigit((char) c))
            {
                name.append((char) c);
            }
            else
            {
                throw new IOException("Bad config entry name.");
            }
        }
        return name.toString();
    }

    private static String readValue(
        final BufferedReader r,
        boolean quote,
        final int eol) throws IOException
    {
        final StringBuffer value = new StringBuffer();
        boolean space = false;
        for (;;)
        {
            r.mark(1);
            int c = r.read();
            if (c < 0)
            {
                throw new IOException("Unexpected end of config file.");
            }
            if ('\n' == c)
            {
                if (quote)
                {
                    throw new IOException("Newline in quotes not allowed.");
                }
                r.reset();
                break;
            }
            if (eol == c)
            {
                break;
            }
            if (!quote)
            {
                if (Character.isWhitespace((char) c))
                {
                    space = true;
                    continue;
                }
                if (';' == c || '#' == c)
                {
                    r.reset();
                    break;
                }
            }
            if (space)
            {
                if (value.length() > 0)
                {
                    value.append(' ');
                }
                space = false;
            }
            if ('\\' == c)
            {
                c = r.read();
                switch (c)
                {
                case -1:
                    throw new IOException("End of file in escape.");
                case '\n':
                    continue;
                case 't':
                    value.append('\t');
                    continue;
                case 'b':
                    value.append('\b');
                    continue;
                case 'n':
                    value.append('\n');
                    continue;
                case '\\':
                    value.append('\\');
                    continue;
                case '"':
                    value.append('"');
                    continue;
                default:
                    throw new IOException("Bad escape: " + ((char) c));
                }
            }
            if ('"' == c)
            {
                quote = !quote;
                continue;
            }
            value.append((char) c);
        }
        return value.length() > 0 ? value.toString() : null;
    }

    public String toString()
    {
        return "RepositoryConfig[" + configFile.getPath() + "]";
    }

    private static class Entry
    {
        String prefix;

        String base;

        String extendedBase;

        String name;

        String value;

        String suffix;
    }
}
