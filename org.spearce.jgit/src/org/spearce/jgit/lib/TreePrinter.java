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
