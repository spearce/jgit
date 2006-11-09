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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class RefLock {
    private final File ref;

    private final File lck;

    private FileLock fLck;

    private boolean haveLck;

    private OutputStream os;

    public RefLock(final Ref r) {
	ref = r.getFile();
	lck = new File(ref.getParentFile(), ref.getName() + ".lock");
    }

    public boolean lock() throws IOException {
	lck.getParentFile().mkdirs();
	if (lck.createNewFile()) {
	    haveLck = true;
	    try {
		final FileOutputStream f = new FileOutputStream(lck);
		try {
		    fLck = f.getChannel().tryLock();
		    if (fLck != null) {
			os = new BufferedOutputStream(f,
				Constants.OBJECT_ID_LENGTH * 2 + 1);
		    } else {
			haveLck = false;
			f.close();
		    }
		} catch (OverlappingFileLockException ofle) {
		    haveLck = false;
		    f.close();
		}
	    } catch (IOException ioe) {
		unlock();
		throw ioe;
	    }
	}
	return haveLck;
    }

    public void write(final ObjectId id) throws IOException {
	try {
	    id.copyTo(os);
	    os.write('\n');
	    fLck.release();
	    os.close();
	    os = null;
	} catch (IOException ioe) {
	    unlock();
	    throw ioe;
	} catch (RuntimeException ioe) {
	    unlock();
	    throw ioe;
	}
    }

    public boolean commit() throws IOException {
	if (lck.renameTo(ref)) {
	    return true;
	} else {
	    unlock();
	    return false;
	}
    }

    public void unlock() throws IOException {
	if (os != null) {
	    try {
		os.close();
	    } catch (IOException ioe) {
	    }
	    os = null;
	}

	if (haveLck) {
	    haveLck = false;
	    lck.delete();
	}
    }
}
