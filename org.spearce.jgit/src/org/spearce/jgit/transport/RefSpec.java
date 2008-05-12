/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.transport;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;

/**
 * Describes how refs in one repository copy into another repository.
 * <p>
 * A ref specification provides matching support and limited rules to rewrite a
 * reference in one repository to another reference in another repository.
 */
public class RefSpec {
	private static final String WILDCARD_SUFFIX = "/*";

	private static boolean isWildcard(final String s) {
		return s != null && s.endsWith(WILDCARD_SUFFIX);
	}

	/** Does this specification ask for forced updated (rewind/reset)? */
	private boolean force;

	/** Is this specification actually a wildcard match? */
	private boolean wildcard;

	/** Name of the ref(s) we would copy from. */
	private String srcName;

	/** Name of the ref(s) we would copy into. */
	private String dstName;

	/**
	 * Construct an empty RefSpec.
	 * <p>
	 * A newly created empty RefSpec is not suitable for use in most
	 * applications, as at least one field must be set to match a source name.
	 */
	public RefSpec() {
		force = false;
		wildcard = false;
		srcName = Constants.HEAD;
		dstName = null;
	}

	/**
	 * Parse a ref specification for use during transport operations.
	 * <p>
	 * Specifications are typically one of the following forms:
	 * <ul>
	 * <li><code>refs/head/master</code></li>
	 * <li><code>refs/head/master:refs/remotes/origin/master</code></li>
	 * <li><code>refs/head/*:refs/remotes/origin/*</code></li>
	 * <li><code>+refs/head/master</code></li>
	 * <li><code>+refs/head/master:refs/remotes/origin/master</code></li>
	 * <li><code>+refs/head/*:refs/remotes/origin/*</code></li>
	 * <li><code>:refs/head/master</code></li>
	 * </ul>
	 * 
	 * @param spec
	 *            string describing the specification.
	 * @throws IllegalArgumentException
	 *             the specification is invalid.
	 */
	public RefSpec(final String spec) {
		String s = spec;
		if (s.startsWith("+")) {
			force = true;
			s = s.substring(1);
		}

		final int c = s.indexOf(':');
		if (c == 0) {
			s = s.substring(1);
			if (isWildcard(s))
				throw new IllegalArgumentException("Invalid wildcards " + spec);
			dstName = s;
		} else if (c > 0) {
			srcName = s.substring(0, c);
			dstName = s.substring(c + 1);
			if (isWildcard(srcName) && isWildcard(dstName))
				wildcard = true;
			else if (isWildcard(srcName) || isWildcard(dstName))
				throw new IllegalArgumentException("Invalid wildcards " + spec);
		} else {
			if (isWildcard(s))
				throw new IllegalArgumentException("Invalid wildcards " + spec);
			srcName = s;
		}
	}

	/**
	 * Expand a wildcard specification.
	 * 
	 * @param p
	 *            the wildcard specification we should base ourselves on.
	 * @param name
	 *            actual name that matched the source of <code>p</code>.
	 */
	protected RefSpec(final RefSpec p, final String name) {
		final String pdst = p.getDestination();
		if (p.getSource() == null || pdst == null)
			throw new IllegalArgumentException("Cannot expand from " + p);
		force = p.isForceUpdate();
		srcName = name;
		dstName = pdst.substring(0, pdst.length() - 1)
				+ name.substring(p.getSource().length() - 1);
	}

	private RefSpec(final RefSpec p) {
		force = p.isForceUpdate();
		wildcard = p.isWildcard();
		srcName = p.getSource();
		dstName = p.getDestination();
	}

	/**
	 * Check if this specification wants to forcefully update the destination.
	 * 
	 * @return true if this specification asks for updates without merge tests.
	 */
	public boolean isForceUpdate() {
		return force;
	}

	/**
	 * Create a new RefSpec with a different force update setting.
	 * 
	 * @param forceUpdate
	 *            new value for force update in the returned instance.
	 * @return a new RefSpec with force update as specified.
	 */
	public RefSpec setForceUpdate(final boolean forceUpdate) {
		final RefSpec r = new RefSpec(this);
		r.force = forceUpdate;
		return r;
	}

	/**
	 * Check if this specification is actually a wildcard pattern.
	 * <p>
	 * If this is a wildcard pattern then the source and destination names
	 * returned by {@link #getSource()} and {@link #getDestination()} will not
	 * be actual ref names, but instead will be patterns.
	 * 
	 * @return true if this specification could match more than one ref.
	 */
	public boolean isWildcard() {
		return wildcard;
	}

	/**
	 * Get the source ref description.
	 * <p>
	 * During a fetch this is the name of the ref on the remote repository we
	 * are fetching from. During a push this is the name of the ref on the local
	 * repository we are pushing out from.
	 * 
	 * @return name (or wildcard pattern) to match the source ref.
	 */
	public String getSource() {
		return srcName;
	}

	/**
	 * Create a new RefSpec with a different source name setting.
	 * 
	 * @param source
	 *            new value for source in the returned instance.
	 * @return a new RefSpec with source as specified.
	 * @throws IllegalStateException
	 *             There is already a destination configured, and the wildcard
	 *             status of the existing destination disagrees with the
	 *             wildcard status of the new source.
	 */
	public RefSpec setSource(final String source) {
		final RefSpec r = new RefSpec(this);
		r.srcName = source;
		if (isWildcard(r.srcName) && r.dstName == null)
			throw new IllegalStateException("Destination is not a wildcard.");
		if (isWildcard(r.srcName) != isWildcard(r.dstName))
			throw new IllegalStateException("Source/Destination must match.");
		return r;
	}

	/**
	 * Get the destination ref description.
	 * <p>
	 * During a fetch this is the local tracking branch that will be updated
	 * with the new ObjectId after feching is complete. During a push this is
	 * the remote ref that will be updated by the remote's receive-pack process.
	 * <p>
	 * If null during a fetch no tracking branch should be updated and the
	 * ObjectId should be stored transiently in order to prepare a merge.
	 * <p>
	 * If null during a push, use {@link #getSource()} instead.
	 * 
	 * @return name (or wildcard) pattern to match the destination ref.
	 */
	public String getDestination() {
		return dstName;
	}

	/**
	 * Create a new RefSpec with a different destination name setting.
	 * 
	 * @param destination
	 *            new value for destination in the returned instance.
	 * @return a new RefSpec with destination as specified.
	 * @throws IllegalStateException
	 *             There is already a source configured, and the wildcard status
	 *             of the existing source disagrees with the wildcard status of
	 *             the new destination.
	 */
	public RefSpec setDestination(final String destination) {
		final RefSpec r = new RefSpec(this);
		r.dstName = destination;
		if (isWildcard(r.dstName) && r.srcName == null)
			throw new IllegalStateException("Source is not a wildcard.");
		if (isWildcard(r.srcName) != isWildcard(r.dstName))
			throw new IllegalStateException("Source/Destination must match.");
		return r;
	}

	/**
	 * Create a new RefSpec with a different source/destination name setting.
	 * 
	 * @param source
	 *            new value for source in the returned instance.
	 * @param destination
	 *            new value for destination in the returned instance.
	 * @return a new RefSpec with destination as specified.
	 * @throws IllegalArgumentException
	 *             The wildcard status of the new source disagrees with the
	 *             wildcard status of the new destination.
	 */
	public RefSpec setSourceDestination(final String source,
			final String destination) {
		if (isWildcard(source) != isWildcard(destination))
			throw new IllegalArgumentException("Source/Destination must match.");
		final RefSpec r = new RefSpec(this);
		r.wildcard = isWildcard(source);
		r.srcName = source;
		r.dstName = destination;
		return r;
	}

	/**
	 * Does this specification's source description match the ref?
	 * 
	 * @param r
	 *            ref whose name should be tested.
	 * @return true if the names match; false otherwise.
	 */
	public boolean matchSource(final Ref r) {
		return match(r, getSource());
	}

	/**
	 * Does this specification's destination description match the ref?
	 * 
	 * @param r
	 *            ref whose name should be tested.
	 * @return true if the names match; false otherwise.
	 */
	public boolean matchDestination(final Ref r) {
		return match(r, getDestination());
	}

	/**
	 * Expand this specification to exactly match a ref.
	 * <p>
	 * Callers must first verify the passed ref matches this specification,
	 * otherwise expansion results may be unpredictable.
	 * 
	 * @param r
	 *            a ref that matched our source specification.
	 * @return a new specification that is not a wildcard.
	 */
	public RefSpec expandFromSource(final Ref r) {
		return isWildcard() ? new RefSpec(this, r.getName()) : this;
	}

	private boolean match(final Ref r, final String s) {
		if (s == null)
			return false;
		if (isWildcard())
			return r.getName().startsWith(s.substring(0, s.length() - 1));
		return r.getName().equals(s);
	}

	public int hashCode() {
		int hc = 0;
		if (getSource() != null)
			hc = hc * 31 + getSource().hashCode();
		if (getDestination() != null)
			hc = hc * 31 + getDestination().hashCode();
		return hc;
	}

	public boolean equals(final Object obj) {
		if (!(obj instanceof RefSpec))
			return false;
		final RefSpec b = (RefSpec) obj;
		if (isForceUpdate() != b.isForceUpdate())
			return false;
		if (isWildcard() != b.isWildcard())
			return false;
		if (!eq(getSource(), b.getSource()))
			return false;
		if (!eq(getDestination(), b.getDestination()))
			return false;
		return true;
	}

	private static boolean eq(final String a, final String b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;
		return a.equals(b);
	}

	public String toString() {
		final StringBuilder r = new StringBuilder();
		if (isForceUpdate())
			r.append('+');
		if (getSource() != null)
			r.append(getSource());
		if (getDestination() != null) {
			r.append(':');
			r.append(getDestination());
		}
		return r.toString();
	}
}
