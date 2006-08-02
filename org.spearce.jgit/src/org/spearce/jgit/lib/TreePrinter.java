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

import java.io.IOException;
import java.io.PrintStream;

public class TreePrinter implements TreeVisitor
{
    private final PrintStream dest;

    public TreePrinter(final PrintStream x)
    {
        dest = x;
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        dest.println(f);
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
        dest.println(s);
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        dest.println(t);
    }

    public void endVisitTree(final Tree t) throws IOException
    {
    }
}
