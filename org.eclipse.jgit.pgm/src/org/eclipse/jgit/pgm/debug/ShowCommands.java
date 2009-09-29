package org.eclipse.jgit.pgm.debug;

import java.net.URL;

import org.kohsuke.args4j.Option;
import org.eclipse.jgit.pgm.Command;
import org.eclipse.jgit.pgm.CommandCatalog;
import org.eclipse.jgit.pgm.CommandRef;
import org.eclipse.jgit.pgm.TextBuiltin;

@Command(usage = "Display a list of all registered jgit commands")
class ShowCommands extends TextBuiltin {
	@Option(name = "--pretty", usage = "alter the detail shown")
	private Format pretty = Format.USAGE;

	@Override
	protected void run() throws Exception {
		final CommandRef[] list = CommandCatalog.all();

		int width = 0;
		for (final CommandRef c : list)
			width = Math.max(width, c.getName().length());
		width += 2;

		for (final CommandRef c : list) {
			System.err.print(c.isCommon() ? '*' : ' ');
			System.err.print(' ');

			System.err.print(c.getName());
			for (int i = c.getName().length(); i < width; i++)
				System.err.print(' ');

			pretty.print(c);
			System.err.println();
		}
		System.err.println();
	}

	static enum Format {
		/** */
		USAGE {
			void print(final CommandRef c) {
				System.err.print(c.getUsage());
			}
		},

		/** */
		CLASSES {
			void print(final CommandRef c) {
				System.err.print(c.getImplementationClassName());
			}
		},

		/** */
		URLS {
			void print(final CommandRef c) {
				final ClassLoader ldr = c.getImplementationClassLoader();

				String cn = c.getImplementationClassName();
				cn = cn.replace('.', '/') + ".class";

				final URL url = ldr.getResource(cn);
				if (url == null) {
					System.err.print("!! NOT FOUND !!");
					return;
				}

				String rn = url.toExternalForm();
				if (rn.endsWith(cn))
					rn = rn.substring(0, rn.length() - cn.length());

				System.err.print(rn);
			}
		};

		abstract void print(CommandRef c);
	}
}
