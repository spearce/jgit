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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.MissingObjectException;

public class CheckoutTree extends TreeVisitorWithCurrentDirectory
{
    private final byte[] copyBuffer;

    public CheckoutTree(final File root)
    {
        super(root);
        copyBuffer = new byte[8192];
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        final File destFile = new File(getCurrentDirectory(), f.getName());
        final ObjectReader or = f.openReader();

        if (or == null)
        {
            throw new MissingObjectException(f.getId(), Constants.TYPE_BLOB);
        }

        try
        {
            final InputStream is = or.getInputStream();
            try
            {
                final FileOutputStream fos = new FileOutputStream(destFile);
                try
                {
                    int r;
                    while ((r = is.read(copyBuffer)) > 0)
                    {
                        fos.write(copyBuffer, 0, r);
                    }
                }
                finally
                {
                    fos.close();
                }
            }
            finally
            {
                or.close();
            }
        }
        catch (IOException ioe)
        {
            destFile.delete();
            throw ioe;
        }
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        super.startVisitTree(t);
        getCurrentDirectory().mkdirs();
    }
}
