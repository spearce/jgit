package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

public abstract class SpeedTestBase extends TestCase {
	protected long nativeTime;
	protected String kernelrepo;

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
		while (inputStream.read(buf)>=0)
			; // empty
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
