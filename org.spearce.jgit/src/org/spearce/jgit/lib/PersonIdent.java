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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class PersonIdent {
	private final String name;

	private final String emailAddress;

	private final Long when;

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
		when = Calendar.getInstance().getTimeInMillis();
		tzOffset = TimeZone.getDefault().getOffset(when.longValue())
				/ (60 * 1000);
	}

	public PersonIdent(final PersonIdent pi) {
		this(pi.getName(), pi.getEmailAddress());
	}

	public PersonIdent(final String aName, final String aEmailAddress) {
		this(aName, aEmailAddress, new Date(), TimeZone.getDefault());
	}

	public PersonIdent(final PersonIdent pi, final Date when, final TimeZone tz) {
		this(pi.getName(), pi.getEmailAddress(), when, tz);
	}

	public PersonIdent(final PersonIdent pi, final Date aWhen) {
		name = pi.getName();
		emailAddress = pi.getEmailAddress();
		when = new Long(aWhen.getTime());
		tzOffset = pi.tzOffset;
	}

	public PersonIdent(final String aName, final String aEmailAddress,
			final Date aWhen, final TimeZone aTZ) {
		name = aName;
		emailAddress = aEmailAddress;
		when = new Long(aWhen.getTime());
		tzOffset = aTZ.getOffset(when.longValue()) / (60 * 1000);
	}

	public PersonIdent(final String aName, final String aEmailAddress,
			final long aWhen, final int aTZ) {
		name = aName;
		emailAddress = aEmailAddress;
		when = new Long(aWhen);
		tzOffset = aTZ;
	}

	public PersonIdent(final PersonIdent pi, final long aWhen, final int aTZ) {
		name = pi.getName();
		emailAddress = pi.getEmailAddress();
		when = new Long(aWhen);
		tzOffset = aTZ;
	}

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
			when = null;
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
			when = new Long(
					Long.parseLong(in.substring(gt + 1, sp).trim()) * 1000);
			tzOffset = tzHours * 60 + tzMins;
		}

		name = in.substring(0, lt).trim();
		emailAddress = in.substring(lt + 1, gt).trim();
	}

	public String getName() {
		return name;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public Date getWhen() {
		if (when != null)
			return new Date(when.longValue());
		return null;
	}

	public int hashCode() {
		return getEmailAddress().hashCode() ^ (when.intValue());
	}

	public boolean equals(final Object o) {
		if (o instanceof PersonIdent) {
			final PersonIdent p = (PersonIdent) o;
			return getName().equals(p.getName())
					&& getEmailAddress().equals(p.getEmailAddress())
					&& (when == p.when || when != null && when.equals(p.when));
		}
		return false;
	}

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
		if (when != null) {
			r.append(when.longValue() / 1000);
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
		}
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
		if (when != null) {
			r.append(new Date(when.longValue() + minutes * 60));
		}
		r.append("]");

		return r.toString();
	}
}
