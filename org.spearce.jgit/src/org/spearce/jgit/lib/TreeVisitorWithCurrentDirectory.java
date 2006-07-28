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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public abstract class TreeVisitorWithCurrentDirectory implements TreeVisitor
{
    private final ArrayList stack;

    private File currentDirectory;

    protected TreeVisitorWithCurrentDirectory(final File rootDirectory)
    {
        stack = new ArrayList(16);
        currentDirectory = rootDirectory;
    }

    protected File getCurrentDirectory()
    {
        return currentDirectory;
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        stack.add(currentDirectory);
        if (!t.isRoot())
        {
            currentDirectory = new File(currentDirectory, t.getName());
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        currentDirectory = (File) stack.remove(stack.size() - 1);
    }
}
