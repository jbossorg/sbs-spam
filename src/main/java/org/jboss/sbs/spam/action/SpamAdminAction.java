/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.sbs.spam.action;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jboss.sbs.spam.SpamManager;

import com.jivesoftware.base.User;
import com.jivesoftware.base.UserNotFoundException;
import com.jivesoftware.community.action.JiveActionSupport;

/**
 * Spam administration action
 * 
 * @author Libor Krzyzanek
 * 
 */
public class SpamAdminAction extends JiveActionSupport {

	private static final long serialVersionUID = -4261955446872689466L;

	private SpamManager spamManager;

	private String spammerUserName;

	private Set<User> unapprovedSpammers;

	@Override
	public String input() {
		unapprovedSpammers = spamManager.getUnapprovedSpammers(getUser());
		return super.input();
	}

	public String resolveContentAsSpam() {
		if (StringUtils.isBlank(spammerUserName)) {
			addActionError(getText("spam.admin.resolve.text.userRequired"));
			return INPUT;
		}

		try {
			User user = userManager.getUser(spammerUserName);

			spamManager.resolveContentAsSpam(user, getUser());

			userManager.enableUser(user);

			addActionMessage(getText("spam.admin.resolve.text.success"));
		} catch (UserNotFoundException e) {
			addActionError(getText("spam.admin.resolve.text.userNotFound"));
			return INPUT;
		}

		return SUCCESS;
	}

	public Set<User> getUnapprovedSpammers() {
		return unapprovedSpammers;
	}

	public void setSpamManager(SpamManager spamManager) {
		this.spamManager = spamManager;
	}

	public String getSpammerUserName() {
		return spammerUserName;
	}

	public void setSpammerUserName(String spammerUserName) {
		this.spammerUserName = spammerUserName;
	}

}
