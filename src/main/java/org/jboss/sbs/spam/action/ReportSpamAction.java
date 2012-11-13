/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.sbs.spam.action;

import org.apache.log4j.Logger;
import org.jboss.sbs.spam.SpamManager;

import com.jivesoftware.base.User;
import com.jivesoftware.community.AbuseType;
import com.jivesoftware.community.JiveObject;
import com.jivesoftware.community.NotFoundException;
import com.jivesoftware.community.action.MessageAbuseAction;
import com.jivesoftware.community.action.util.AlwaysDisallowAnonymous;
import com.jivesoftware.community.favorites.external.ExternalURL;

/**
 * Action to report SPAM. It extends functionality of {@link MessageAbuseAction}
 * 
 * @author Libor Krzyzanek
 * 
 */
@AlwaysDisallowAnonymous
public class ReportSpamAction extends MessageAbuseAction {

	private static final long serialVersionUID = 1150437943822774437L;

	private static final Logger log = Logger.getLogger(ReportSpamAction.class);

	private SpamManager spamManager;

	@Override
	public int getAbuseType() {
		return AbuseType.SPAM.getKey();
	}

	@Override
	public String execute() {
		log.debug("Report content as usual Report Abuse action");
		String reportedObjectResult = super.execute();
		if (!reportedObjectResult.equals(SUCCESS)) {
			return reportedObjectResult;
		}

		if (!spamManager.canReportSpammer(getUser())) {
			log.debug("User " + getUser().getUsername() + " cannot report user and his all content as spam. "
					+ "Only one particular content was reported as a spam.");
			return SUCCESS;
		}

		JiveObject jiveObject = null;
		try {
			jiveObject = getJiveObjectLoader().getJiveObject(getObjectType(), getObjectID());
		} catch (NotFoundException e) {
			// should not occur because it's already catched in super.execute();
			throw new RuntimeException(e);
		}

		User spammer = spamManager.getSpammer(jiveObject);
		if (spammer == null) {
			if (jiveObject instanceof ExternalURL) {
				log.debug("object is external URL and has now owner.");
				return SUCCESS;
			}
			log.error("Spammer not found for particular object. Objectc: " + jiveObject);
			return ERROR;
		}

		if (log.isDebugEnabled()) {
			log.debug("Spammer (author) of reported content is: " + spammer.getUsername());
		}

		spamManager.reportSpammersContent(spammer, getUser(), getComment());

		spamManager.disableSpammer(spammer);

		return SUCCESS;
	}

	public SpamManager getSpamManager() {
		return spamManager;
	}

	public void setSpamManager(SpamManager spamManager) {
		this.spamManager = spamManager;
	}

}
