/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.lib;

import java.util.Date;
import java.util.TimeZone;

/**
 * A combination of a person identity and time in Git.
 * 
 * Git combines Name + email + time + time zone to specify who wrote or
 * committed something.
 */
public class PersonIdent {
	private final String name;

	private final String emailAddress;

	private final long when;

	private final int tzOffset;

	private static String getHostName() {
		try {
			java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
			String hostname = addr.getCanonicalHostName();
			return hostname;
		} catch (java.net.UnknownHostException e) {
			return "localhost";
		}
	}

	/**
	 * Creates new PersonIdent from config info in repository, with current time
	 * 
	 * @param repo
	 */
	public PersonIdent(final Repository repo) {
		RepositoryConfig config = repo.getConfig();
		String username = config.getString("user", null, "name");
		if (username == null)
			username = System.getProperty("user.name");

		String email = config.getString("user", null, "email");
		if (email == null)
			email = System.getProperty("user.name") + "@" + getHostName();

		name = username;
		emailAddress = email;
		when = System.currentTimeMillis();
		tzOffset = TimeZone.getDefault().getOffset(when) / (60 * 1000);
	}

	/**
	 * Copy a {@link PersonIdent}.
	 * 
	 * @param pi
	 *            Original {@link PersonIdent}
	 */
	public PersonIdent(final PersonIdent pi) {
		this(pi.getName(), pi.getEmailAddress());
	}

	/**
	 * Construct a new {@link PersonIdent} with current time.
	 * 
	 * @param aName
	 * @param aEmailAddress
	 */
	public PersonIdent(final String aName, final String aEmailAddress) {
		this(aName, aEmailAddress, new Date(), TimeZone.getDefault());
	}

	/**
	 * Copy a PersonIdent, but alter the clone's time stamp
	 * 
	 * @param pi
	 *            original {@link PersonIdent}
	 * @param when
	 *            local time
	 * @param tz
	 *            time zone
	 */
	public PersonIdent(final PersonIdent pi, final Date when, final TimeZone tz) {
		this(pi.getName(), pi.getEmailAddress(), when, tz);
	}

	/**
	 * Copy a {@link PersonIdent}, but alter the clone's time stamp
	 * 
	 * @param pi
	 *            original {@link PersonIdent}
	 * @param aWhen
	 *            local time
	 */
	public PersonIdent(final PersonIdent pi, final Date aWhen) {
		name = pi.getName();
		emailAddress = pi.getEmailAddress();
		when = aWhen.getTime();
		tzOffset = pi.tzOffset;
	}

	/**
	 * Construct a PersonIdent from simple data
	 * 
	 * @param aName
	 * @param aEmailAddress
	 * @param aWhen
	 *            local time stamp
	 * @param aTZ
	 *            time zone
	 */
	public PersonIdent(final String aName, final String aEmailAddress,
			final Date aWhen, final TimeZone aTZ) {
		name = aName;
		emailAddress = aEmailAddress;
		when = aWhen.getTime();
		tzOffset = aTZ.getOffset(when) / (60 * 1000);
	}

	/**
	 * Construct a {@link PersonIdent}
	 * 
	 * @param aName
	 * @param aEmailAddress
	 * @param aWhen
	 *            local time stamp
	 * @param aTZ
	 *            time zone
	 */
	public PersonIdent(final String aName, final String aEmailAddress,
			final long aWhen, final int aTZ) {
		name = aName;
		emailAddress = aEmailAddress;
		when = aWhen;
		tzOffset = aTZ;
	}

	/**
	 * Copy a PersonIdent, but alter the clone's time stamp
	 * 
	 * @param pi
	 *            original {@link PersonIdent}
	 * @param aWhen
	 *            local time stamp
	 * @param aTZ
	 *            time zone
	 */
	public PersonIdent(final PersonIdent pi, final long aWhen, final int aTZ) {
		name = pi.getName();
		emailAddress = pi.getEmailAddress();
		when = aWhen;
		tzOffset = aTZ;
	}

	/**
	 * Construct a PersonIdent from a string with full name, email, time time
	 * zone string. The input string must be valid.
	 * 
	 * @param in
	 *            a Git internal format author/committer string.
	 */
	public PersonIdent(final String in) {
		final int lt = in.indexOf('<');
		if (lt == -1) {
			throw new IllegalArgumentException("Malformed PersonIdent string"
					+ " (no < was found): " + in);
		}
		final int gt = in.indexOf('>', lt);
		if (gt == -1) {
			throw new IllegalArgumentException("Malformed PersonIdent string"
					+ " (no > was found): " + in);
		}
		final int sp = in.indexOf(' ', gt + 2);
		if (sp == -1) {
			when = 0;
			tzOffset = -1;
		} else {
			final String tzHoursStr = in.substring(sp + 1, sp + 4).trim();
			final int tzHours;
			if (tzHoursStr.charAt(0) == '+') {
				tzHours = Integer.parseInt(tzHoursStr.substring(1));
			} else {
				tzHours = Integer.parseInt(tzHoursStr);
			}
			final int tzMins = Integer.parseInt(in.substring(sp + 4).trim());
			when = Long.parseLong(in.substring(gt + 1, sp).trim()) * 1000;
			tzOffset = tzHours * 60 + tzMins;
		}

		name = in.substring(0, lt).trim();
		emailAddress = in.substring(lt + 1, gt).trim();
	}

	/**
	 * @return Name of person
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return email address of person
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @return timestamp
	 */
	public Date getWhen() {
		return new Date(when);
	}

	/**
	 * @return this person's preferred time zone; null if time zone is unknown.
	 */
	public TimeZone getTimeZone() {
		final String[] ids = TimeZone.getAvailableIDs(tzOffset * 60 * 1000);
		if (ids.length == 0)
			return null;
		return TimeZone.getTimeZone(ids[0]);
	}

	public int hashCode() {
		return getEmailAddress().hashCode() ^ (int) when;
	}

	public boolean equals(final Object o) {
		if (o instanceof PersonIdent) {
			final PersonIdent p = (PersonIdent) o;
			return getName().equals(p.getName())
					&& getEmailAddress().equals(p.getEmailAddress())
					&& when == p.when;
		}
		return false;
	}

	/**
	 * Format for Git storage.
	 * 
	 * @return a string in the git author format
	 */
	public String toExternalString() {
		final StringBuffer r = new StringBuffer();
		int offset = tzOffset;
		final char sign;
		final int offsetHours;
		final int offsetMins;

		if (offset < 0) {
			sign = '-';
			offset = -offset;
		} else {
			sign = '+';
		}

		offsetHours = offset / 60;
		offsetMins = offset % 60;

		r.append(getName());
		r.append(" <");
		r.append(getEmailAddress());
		r.append("> ");
		r.append(when / 1000);
		r.append(' ');
		r.append(sign);
		if (offsetHours < 10) {
			r.append('0');
		}
		r.append(offsetHours);
		if (offsetMins < 10) {
			r.append('0');
		}
		r.append(offsetMins);
		return r.toString();
	}

	public String toString() {
		final StringBuffer r = new StringBuffer();
		int minutes;

		minutes = tzOffset < 0 ? -tzOffset : tzOffset;
		minutes = (minutes / 100) * 60 + (minutes % 100);
		minutes = tzOffset < 0 ? -minutes : minutes;

		r.append("PersonIdent[");
		r.append(getName());
		r.append(", ");
		r.append(getEmailAddress());
		r.append(", ");
		r.append(new Date(when + minutes * 60));
		r.append("]");

		return r.toString();
	}
}
