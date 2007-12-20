/*
 *  Copyright (C) 2007  Robin Rosenberg
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

import java.io.IOException;

public class T0008_testparserev extends RepositoryTestCase {

	ObjectId resolve(String in) {
		try {
			return db.resolve(in);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public void testObjectId_existing() {
		assertEquals("49322bb17d3acc9146f98c97d078513228bbf3c0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0").toString());
	}

	public void testObjectId_nonexisting() {
		assertEquals("49322bb17d3acc9146f98c97d078513228bbf3c1",resolve("49322bb17d3acc9146f98c97d078513228bbf3c1").toString());
	}

	public void testObjectId_objectid_implicit_firstparent() {
		assertEquals("6e1475206e57110fcef4b92320436c1e9872a322",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^").toString());
		assertEquals("1203b03dc816ccbb67773f28b3c19318654b0bc8",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^^").toString());
		assertEquals("bab66b48f836ed950c99134ef666436fb07a09a0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^^^").toString());
	}

	public void testObjectId_objectid_self() {
		assertEquals("49322bb17d3acc9146f98c97d078513228bbf3c0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^0").toString());
		assertEquals("49322bb17d3acc9146f98c97d078513228bbf3c0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^0^0").toString());
		assertEquals("49322bb17d3acc9146f98c97d078513228bbf3c0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^0^0^0").toString());
	}

	public void testObjectId_objectid_explicit_firstparent() {
		assertEquals("6e1475206e57110fcef4b92320436c1e9872a322",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^1").toString());
		assertEquals("1203b03dc816ccbb67773f28b3c19318654b0bc8",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^1^1").toString());
		assertEquals("bab66b48f836ed950c99134ef666436fb07a09a0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^1^1^1").toString());
	}

	public void testObjectId_objectid_explicit_otherparents() {
		assertEquals("6e1475206e57110fcef4b92320436c1e9872a322",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^1").toString());
		assertEquals("f73b95671f326616d66b2afb3bdfcdbbce110b44",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^2").toString());
		assertEquals("d0114ab8ac326bab30e3a657a0397578c5a1af88",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^3").toString());
		assertEquals("d0114ab8ac326bab30e3a657a0397578c5a1af88",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^03").toString());
	}

	public void testRef_refname() {
		assertEquals("49322bb17d3acc9146f98c97d078513228bbf3c0",resolve("master^0").toString());
		assertEquals("6e1475206e57110fcef4b92320436c1e9872a322",resolve("master^").toString());
		assertEquals("6e1475206e57110fcef4b92320436c1e9872a322",resolve("refs/heads/master^1").toString());
	}

	public void testDistance() {
		assertEquals("6e1475206e57110fcef4b92320436c1e9872a322",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0~0").toString());
		assertEquals("1203b03dc816ccbb67773f28b3c19318654b0bc8",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0~1").toString());
		assertEquals("bab66b48f836ed950c99134ef666436fb07a09a0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0~2").toString());
		assertEquals("bab66b48f836ed950c99134ef666436fb07a09a0",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0~02").toString());
	}

	public void testTree() {
		assertEquals("6020a3b8d5d636e549ccbd0c53e2764684bb3125",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^{tree}").toString());
		assertEquals("02ba32d3649e510002c21651936b7077aa75ffa9",resolve("49322bb17d3acc9146f98c97d078513228bbf3c0^^{tree}").toString());
	}

	public void testHEAD() {
		assertEquals("6020a3b8d5d636e549ccbd0c53e2764684bb3125",resolve("HEAD^{tree}").toString());
	}
}
