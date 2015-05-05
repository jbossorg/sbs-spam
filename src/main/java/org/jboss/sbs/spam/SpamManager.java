/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.sbs.spam;

import java.util.Set;

import com.jivesoftware.base.User;
import com.jivesoftware.community.JiveContentObject;
import com.jivesoftware.community.JiveObject;

/**
 * SPAM Manager
 * 
 * @author Libor Krzyzanek
 * 
 */
public interface SpamManager {

	/**
	 * Identify if reporter can report spammer
	 * 
	 * @param user
	 * @return
	 */
	public boolean canReportSpammer(User user);

	/**
	 * Disable user
	 * 
	 * @param spammer
	 */
	public void disableSpammer(User spammer);

	/**
	 * All content is reported as SPAM
	 * 
	 * @param spammer
	 * @param reporter
	 * @param comment
	 */
	public void reportSpammersContent(User spammer, User reporter, String comment);

	/**
	 * Resolve spam reports for particular users. This is 'rollback' action to
	 * {@link #reportSpammersContent(User, User, String)}
	 * 
	 * @param spammer
	 *            user who was reporeted as spammer but isn't
	 * @param moderator
	 *            user who resolving spammer
	 */
	public void resolveContentAsSpam(User spammer, User moderator);

	/**
	 * Get spammer (author) of particular content
	 * 
	 * @param jiveObject
	 * @return
	 */
	public User getSpammer(JiveObject jiveObject);

	/**
	 * Get set of spammers for particular moderator
	 * 
	 * @param moderator
	 * @return
	 */
	public Set<User> getUnapprovedSpammers(User moderator);
	
	
	public boolean hasSomeContent(User author, JiveContentObject contentObject);

}
