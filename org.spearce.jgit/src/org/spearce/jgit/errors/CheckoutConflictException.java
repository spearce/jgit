package org.spearce.jgit.errors;

import java.io.IOException;

public class CheckoutConflictException extends IOException {
	private static final long serialVersionUID = 1L;

	public CheckoutConflictException(String file) {
		super("Checkout conflict with file: " + file);
	}
	
	public CheckoutConflictException(String[] files) {
		super("Checkout conflict with files: " + buildList(files));
	}

	private static String buildList(String[] files) {
		StringBuilder builder = new StringBuilder();
		for (String f : files) { 
			builder.append("\n");
			builder.append(f);
		}
		return builder.toString();
	}
}
