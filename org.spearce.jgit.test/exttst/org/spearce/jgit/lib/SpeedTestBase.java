package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

/**
 * Base class for performance unit test.
 */
public abstract class SpeedTestBase extends TestCase {

	/**
	 * The time used by native git as this is our reference.
	 */
	protected long nativeTime;

	/**
	 * Reference to the location of the Linux kernel repo.
	 */
	protected String kernelrepo;

	/**
	 * Prepare test by running a test against the Linux kernel repo first.
	 *
	 * @param refcmd
	 *            git command to execute
	 *
	 * @throws Exception
	 */
	protected void prepare(String[] refcmd) throws Exception {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader("kernel.ref"));
			kernelrepo = bufferedReader.readLine();
			bufferedReader.close();
			timeNativeGit(kernelrepo, refcmd);
			nativeTime = timeNativeGit(kernelrepo, refcmd);
		} catch (Exception e) {
			System.out.println("Create a file named kernel.ref and put the path to the Linux kernels repository there");
			throw e;
		}
	}

	private static long timeNativeGit(String kernelrepo, String[] refcmd) throws IOException,
			InterruptedException, Exception {
		long start = System.currentTimeMillis();
		Process p = Runtime.getRuntime().exec(refcmd, null, new File(kernelrepo,".."));
		InputStream inputStream = p.getInputStream();
		InputStream errorStream = p.getErrorStream();
		byte[] buf=new byte[1024*1024];
		for (;;)
			if (inputStream.read(buf) < 0)
				break;
		if (p.waitFor()!=0) {
			int c;
			while ((c=errorStream.read())!=-1)
				System.err.print((char)c);
			throw new Exception("git log failed");
		}
		inputStream.close();
		errorStream.close();
		long stop = System.currentTimeMillis();
		return stop - start;
	}
}
