/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.sbs.spam.filter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * @author Libor Krzyzanek
 * 
 */
public class NofollowLinkFilterTest {

	@Test
	public void testIsOnWhitelist() {
		NofollowLinkFilter filter = new NofollowLinkFilter();
		filter.setDomainWhiteList("jboss.org www.jboss.org in.relation.to aerogear.org");

		assertTrue(filter.isOnWhitelist("http://jboss.org"));

		assertTrue(filter.isOnWhitelist("http://in.relation.to"));

		assertTrue(filter.isOnWhitelist("http://aerogear.org"));

		assertTrue(filter.isOnWhitelist("http://www.jboss.org"));
		assertTrue(filter.isOnWhitelist("https://www.jboss.org"));

		assertFalse(filter.isOnWhitelist("http://www.google.com"));
	}

}
