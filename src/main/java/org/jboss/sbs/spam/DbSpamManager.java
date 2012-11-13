/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.sbs.spam;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;
import com.jivesoftware.base.User;
import com.jivesoftware.base.UserManager;
import com.jivesoftware.community.AbuseManager;
import com.jivesoftware.community.AbuseReport;
import com.jivesoftware.community.AbuseType;
import com.jivesoftware.community.ApprovalManager;
import com.jivesoftware.community.Blog;
import com.jivesoftware.community.BlogManager;
import com.jivesoftware.community.BlogPost;
import com.jivesoftware.community.BlogPostResultFilter;
import com.jivesoftware.community.Document;
import com.jivesoftware.community.DocumentManager;
import com.jivesoftware.community.DocumentState;
import com.jivesoftware.community.ForumManager;
import com.jivesoftware.community.ForumMessage;
import com.jivesoftware.community.ForumThread;
import com.jivesoftware.community.JiveGlobals;
import com.jivesoftware.community.JiveIterator;
import com.jivesoftware.community.JiveObject;
import com.jivesoftware.community.JiveObjectLoader;
import com.jivesoftware.community.ModerationFilter;
import com.jivesoftware.community.NotFoundException;
import com.jivesoftware.community.StatusLevelManager;
import com.jivesoftware.community.UserAuthoredObject;
import com.jivesoftware.community.audit.aop.Audit;
import com.jivesoftware.community.favorites.Favorite;
import com.jivesoftware.community.favorites.FavoriteManager;
import com.jivesoftware.community.favorites.type.ExternalUrlObjectType;
import com.jivesoftware.community.favorites.type.FavoritableType;
import com.jivesoftware.community.impl.dao.ApprovalWorkflowBean;
import com.jivesoftware.community.moderation.JiveObjectModerator;
import com.jivesoftware.community.moderation.ModerationItemException;

/**
 * DB Implementation of {@link SpamManager}
 * 
 * @author Libor Krzyzanek
 * 
 */
public class DbSpamManager implements SpamManager {

	private static final Logger log = Logger.getLogger(DbSpamManager.class);

	private AbuseManager abuseManager;

	private UserManager userManager;

	private DocumentManager documentManager;

	private ForumManager forumManager;

	private BlogManager blogManager;

	private FavoriteManager favoriteManager;

	private StatusLevelManager statusLevelManager;

	private JiveObjectModerator jiveObjectModerator;

	private ApprovalManager approvalManager;

	private JiveObjectLoader jiveObjectLoader;

	private FavoritableType externalUrlObjectType;

	private final DocumentState[] documentStates = { DocumentState.PUBLISHED };

	private final DocumentState[] documentStatesToResolve = { DocumentState.PENDING_APPROVAL };

	public int getReporterMinPoints() {
		return JiveGlobals.getJiveIntProperty("spam.reporter.min_points", 0);
	}

	@Override
	public boolean canReportSpammer(User user) {
		long points = statusLevelManager.getPointLevel(user);
		if (log.isTraceEnabled()) {
			log.trace("User " + user.getUsername() + " has points: " + points);
		}

		if (points > getReporterMinPoints()) {
			return true;
		}
		return false;
	}

	@Override
	public void disableSpammer(User spammer) {
		if (log.isInfoEnabled()) {
			log.info("Disabling spammer: " + spammer.getUsername());
		}
		userManager.disableUser(spammer);

	}

	@Audit
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void reportSpammersContent(User spammer, User reporter, String comment) {
		if (log.isInfoEnabled()) {
			log.info("Reporting SPAM Abuse on all content of of this spammer: " + spammer.getUsername()
					+ ". Reporter is: " + reporter.getUsername());
		}

		final Date reportDate = new Date();

		JiveIterator<Document> docs = documentManager.getUserDocuments(spammer, documentStates);
		for (Document document : docs) {
			if (log.isTraceEnabled()) {
				log.trace("Report spam of document: " + document.getDocumentID());
			}
			reportSpam(document, reporter, comment, reportDate);
		}

		JiveIterator<ForumMessage> messages = forumManager.getUserMessages(spammer);
		for (ForumMessage message : messages) {
			if (log.isTraceEnabled()) {
				log.trace("Report spam of message: " + message.getID() + ", threadId: " + message.getForumThreadID());
			}
			// TODO: Check how works root messages (threads)
			reportSpam(message, reporter, comment, reportDate);
		}

		JiveIterator<Blog> blogs = blogManager.getBlogs(spammer);
		for (Blog blog : blogs) {
			JiveIterator<BlogPost> blogPosts = blog.getBlogPosts(BlogPostResultFilter.createDefaultFilter());
			for (BlogPost blogPost : blogPosts) {
				if (log.isTraceEnabled()) {
					log.trace("Report spam Blog post: " + blogPost.getID());
				}
				reportSpam(blogPost, reporter, comment, reportDate);
			}
		}
		JiveIterator<Favorite> favorites = favoriteManager.getUserFavorites(spammer,
				Sets.newHashSet(externalUrlObjectType));
		for (Favorite favorite : favorites) {
			JiveObject favoritedObject = favorite.getObjectFavorite().getFavoritedObject();
			if (log.isTraceEnabled()) {
				log.trace("Report spam Favorite (Bookmark) to external URL: " + favorite.getID());
				log.trace("Favorited object: " + favoritedObject);
			}
			reportSpam(favoritedObject, reporter, comment, reportDate);
		}

	}

	@Audit
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void resolveContentAsSpam(User spammer, User moderator) {
		if (log.isInfoEnabled()) {
			log.info("Resolve SPAM reports on all content of of this user: " + spammer);
		}

		JiveIterator<Document> docs = documentManager.getUserDocuments(spammer, documentStatesToResolve);
		for (Document document : docs) {
			if (log.isTraceEnabled()) {
				log.trace("Resolve Report spam of document: " + document.getDocumentID());
			}
			resolveSpamReport(document, moderator);
		}

		JiveIterator<ForumMessage> messages = forumManager.getUserMessages(spammer,
				ModerationFilter.createAbuseOnlyFilter());
		List<Long> resolvedThreads = new ArrayList<Long>();
		for (ForumMessage message : messages) {
			if (log.isTraceEnabled()) {
				log.trace("Resolve Report spam of message: " + message.getID() + ", threadId: "
						+ message.getForumThreadID());
			}
			ForumThread thread = message.getForumThread();
			if (!resolvedThreads.contains(thread.getID())) {
				resolvedThreads.add(thread.getID());
				if (thread.getUser().getUsername().equals(spammer.getUsername())) {
					if (log.isTraceEnabled()) {
						log.trace("Resolve Report spam of thread: " + thread.getID());
					}
					resolveSpamReport(thread, moderator);
				}
			}
			resolveSpamReport(message, moderator);
		}

		JiveIterator<Blog> blogs = blogManager.getBlogs(spammer);
		BlogPostResultFilter blogFilter = BlogPostResultFilter.createDefaultFilter();
		blogFilter.setOnlyWaitingMod(false);
		blogFilter.setOnlyDraft(false);
		blogFilter.setOnlyPublished(false);
		blogFilter.setUserID(spammer.getID());
		for (Blog blog : blogs) {
			JiveIterator<BlogPost> blogPosts = blog.getBlogPosts(blogFilter);
			for (BlogPost blogPost : blogPosts) {
				if (log.isTraceEnabled()) {
					log.trace("Resolve Report spam Blog post: " + blogPost.getID());
				}
				resolveSpamReport(blogPost, moderator);
			}
		}

		JiveIterator<Favorite> favorites = favoriteManager.getUserFavorites(spammer,
				Sets.newHashSet(externalUrlObjectType));
		for (Favorite favorite : favorites) {
			JiveObject favoritedObject = favorite.getObjectFavorite().getFavoritedObject();
			if (log.isTraceEnabled()) {
				log.trace("Resolve Report spam Favorite (Bookmark) to external URL: " + favorite.getID());
				log.trace("Favorited object, id: " + favoritedObject.getID() + ", type: "
						+ favoritedObject.getObjectType());
			}
			resolveSpamReport(favoritedObject, moderator);
		}

	}

	@Override
	public User getSpammer(JiveObject jiveObject) {
		if (log.isTraceEnabled()) {
			log.trace("Get Spammer from object: " + jiveObject);
		}
		// Problematic is ExternalURL which doesn't contain owner of such external URL which is usually abused for SPAM
		if (jiveObject instanceof UserAuthoredObject) {
			return ((UserAuthoredObject) jiveObject).getUser();
		}
		return null;
	}

	public void reportSpam(JiveObject jiveObject, User reporter, String comment, Date reportDate) {
		AbuseReport abuseReport = new AbuseReport();
		abuseReport.setAbuseType(AbuseType.SPAM);
		abuseReport.setObjectID(jiveObject.getID());
		abuseReport.setObjectType(jiveObject.getObjectType());
		abuseReport.setUser(reporter);
		abuseReport.setReportDate(reportDate);
		abuseReport.setComment(comment);

		abuseManager.reportAbuse(abuseReport);
	}

	public void resolveSpamReport(JiveObject jiveObject, User moderator) {
		abuseManager.resolveAbuseReports(jiveObject);
		List<ApprovalWorkflowBean> workflows = approvalManager.getWorkflowBeans(jiveObject,
				JiveObjectModerator.Type.ABUSE);
		for (ApprovalWorkflowBean workflow : workflows) {
			try {
				jiveObjectModerator.approve(workflow.getWorkflowID(), jiveObject, moderator,
						"Spam report: Content is not spam");
			} catch (ModerationItemException e) {
				log.error("Cannot approve workflow, id: " + workflow.getWorkflowID() + ", message: " + e.getMessage());
			}
		}
	}

	@Override
	public Set<User> getUnapprovedSpammers(User moderator) {
		List<ApprovalWorkflowBean> workflows = approvalManager.getUnApprovedWorkflowBeans(moderator.getID(),
				JiveObjectModerator.Type.ABUSE);
		Set<User> users = new HashSet<User>();
		for (ApprovalWorkflowBean approvalWorkflowBean : workflows) {
			try {
				JiveObject jiveObject = jiveObjectLoader.getJiveObject((int) approvalWorkflowBean.getTypeID(),
						approvalWorkflowBean.getObjID());
				if (jiveObject instanceof UserAuthoredObject) {
					if (log.isTraceEnabled()) {
						log.trace("Adding " + ((UserAuthoredObject) jiveObject).getUser().getUsername()
								+ "user from object " + jiveObject);
					}
					users.add(((UserAuthoredObject) jiveObject).getUser());
				}
			} catch (NotFoundException e) {
				log.error("Cannot find object for approval", e);
			}
		}
		return users;
	}

	public void setAbuseManager(AbuseManager abuseManager) {
		this.abuseManager = abuseManager;
	}

	public void setBlogManager(BlogManager blogManager) {
		this.blogManager = blogManager;
	}

	public void setDocumentManager(DocumentManager documentManager) {
		this.documentManager = documentManager;
	}

	public void setForumManager(ForumManager forumManager) {
		this.forumManager = forumManager;
	}

	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	public void setFavoriteManager(FavoriteManager favoriteManager) {
		this.favoriteManager = favoriteManager;
	}

	public void setStatusLevelManager(StatusLevelManager statusLevelManager) {
		this.statusLevelManager = statusLevelManager;
	}

	public void setJiveObjectModerator(JiveObjectModerator jiveObjectModerator) {
		this.jiveObjectModerator = jiveObjectModerator;
	}

	public void setApprovalManager(ApprovalManager approvalManager) {
		this.approvalManager = approvalManager;
	}

	public void setJiveObjectLoader(JiveObjectLoader jiveObjectLoader) {
		this.jiveObjectLoader = jiveObjectLoader;
	}

	public void setExternalUrlObjectType(ExternalUrlObjectType externalUrlObjectType) {
		this.externalUrlObjectType = externalUrlObjectType;
	}

}
