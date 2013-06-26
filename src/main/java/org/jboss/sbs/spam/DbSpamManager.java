/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.sbs.spam;

import com.google.common.collect.Sets;
import com.jivesoftware.base.User;
import com.jivesoftware.base.UserManager;
import com.jivesoftware.community.*;
import com.jivesoftware.community.audit.aop.Audit;
import com.jivesoftware.community.browse.sort.BlogPostPublishDateSort;
import com.jivesoftware.community.browse.util.BrowseQueryBuilderFactory;
import com.jivesoftware.community.content.blogs.BlogPostBrowseQueryBuilder;
import com.jivesoftware.community.favorites.Favorite;
import com.jivesoftware.community.favorites.FavoriteManager;
import com.jivesoftware.community.favorites.type.ExternalUrlObjectType;
import com.jivesoftware.community.favorites.type.FavoritableType;
import com.jivesoftware.community.impl.dao.ApprovalWorkflowBean;
import com.jivesoftware.community.moderation.JiveObjectModerator;
import com.jivesoftware.community.moderation.ModerationItemException;
import com.jivesoftware.community.statuslevel.StatusLevelManager;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * DB Implementation of {@link SpamManager}
 *
 * @author Libor Krzyzanek
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

	protected BrowseQueryBuilderFactory browseQueryBuilderFactory;

	private final DocumentState[] documentStates = {DocumentState.PUBLISHED};

	private final DocumentState[] documentStatesToResolve = {DocumentState.PENDING_APPROVAL};

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

		Iterable<Document> docs = documentManager.getUserDocuments(spammer, documentStates);
		for (Document document : docs) {
			if (log.isTraceEnabled()) {
				log.trace("Report spam of document: " + document.getDocumentID());
			}
			reportSpam(document, reporter, comment, reportDate);
		}

		Iterable<ForumMessage> messages = forumManager.getUserMessages(spammer);
		for (ForumMessage message : messages) {
			if (log.isTraceEnabled()) {
				log.trace("Report spam of message: " + message.getID() + ", threadId: " + message.getForumThreadID());
			}
			// TODO: Check how works root messages (threads)
			reportSpam(message, reporter, comment, reportDate);
		}

		List<Blog> blogs = blogManager.getExplicitlyEntitledBlogs(spammer);
		for (Blog blog : blogs) {
			if (blog.isUserBlog()) {
				Iterator<BlogPost> blogPosts = blogManager.getBlogPosts(blog);
				while (blogPosts.hasNext()) {
					BlogPost blogPost = blogPosts.next();
					if (log.isTraceEnabled()) {
						log.trace("Report spam for Blog post, id: " + blogPost.getID());
					}
					reportSpam(blogPost, reporter, comment, reportDate);
				}
			}
		}
		Iterator<Favorite> favorites = favoriteManager.getUserFavorites(spammer,
				Sets.newHashSet(externalUrlObjectType));
		while (favorites.hasNext()) {
			Favorite favorite = favorites.next();
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

		log.debug("Resolve Documents");
		Iterable<Document> docs = documentManager.getUserDocuments(spammer, documentStatesToResolve);
		for (Document document : docs) {
			if (log.isTraceEnabled()) {
				log.trace("Resolve Report spam of document: " + document.getDocumentID());
			}
			resolveSpamReport(document, moderator);
		}

		log.debug("Resolve Threads");
		ThreadResultFilter moderationFilter = ThreadResultFilter.createDefaultUserMessagesFilter();
		moderationFilter.setStatus(JiveContentObject.Status.ABUSE_HIDDEN);

		Iterable<ForumMessage> messages = forumManager.getUserMessages(spammer, moderationFilter);
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

		log.debug("Resolve Blogs");
		Iterable<Blog> blogs = blogManager.getExplicitlyEntitledBlogs(spammer);
		for (Blog blog : blogs) {
			if (log.isTraceEnabled()) {
				log.trace("Processing blog: " + blog.getName());
			}
			if (blog.isUserBlog()) {
				Iterator<BlogPost> blogPosts = getBlogPosts(blog, JiveContentObject.Status.ABUSE_HIDDEN);
				while (blogPosts.hasNext()) {
					BlogPost blogPost = blogPosts.next();
					if (log.isTraceEnabled()) {
						log.trace("Resolve Report spam Blog post: " + blogPost.getID());
					}
					resolveSpamReport(blogPost, moderator);
				}
			}
		}

		log.trace("Resolve Bookmarks");
		Iterator<Favorite> favorites = favoriteManager.getUserFavorites(spammer,
				Sets.newHashSet(externalUrlObjectType));
		while (favorites.hasNext()) {
			Favorite favorite = favorites.next();
			JiveObject favoritedObject = favorite.getObjectFavorite().getFavoritedObject();
			if (log.isTraceEnabled()) {
				log.trace("Resolve Report spam Favorite (Bookmark) to external URL: " + favorite.getID());
				log.trace("Favorited object, id: " + favoritedObject.getID() + ", type: "
						+ favoritedObject.getObjectType());
			}
			resolveSpamReport(favoritedObject, moderator);
		}

	}

	protected Iterator<BlogPost> getBlogPosts(JiveContainer container, JiveContentObject.Status... statuses) {
		BlogPostBrowseQueryBuilder blogPostBrowseQueryBuilder = browseQueryBuilderFactory.getBlogPostBrowseQueryBuilder();
		blogPostBrowseQueryBuilder.setContainer(container).setStatuses(statuses);
		return blogPostBrowseQueryBuilder.getContentIterator(0, Integer.MAX_VALUE, new BlogPostPublishDateSort());
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
		// Similar to MessageAbuseAction#execute()
		AbuseReport abuseReport = new AbuseReport();
		abuseReport.setAbuseType(AbuseType.spam);
		abuseReport.setObjectID(jiveObject.getID());
		abuseReport.setObjectType(jiveObject.getObjectType());
		abuseReport.setUser(reporter);
		abuseReport.setReportDate(reportDate);
		abuseReport.setComment(comment);
		abuseReport.setJiveObject(jiveObject);

		abuseManager.reportAbuse(abuseReport);
	}

	public void resolveSpamReport(JiveObject jiveObject, User moderator) {
		abuseManager.resolveAbuseReports(jiveObject);
		List<ApprovalWorkflowBean> workflows = approvalManager.getWorkflowBeans(jiveObject,
				JiveObjectModerator.Type.ABUSE);
		for (ApprovalWorkflowBean workflow : workflows) {
			try {
				jiveObjectModerator.approve(workflow.getWorkflowID(), jiveObject, moderator, "Spam report: Content is not spam");
			} catch (ModerationItemException e) {
				log.error("Cannot approve workflow, id: " + workflow.getWorkflowID() + ", message: " + e.getMessage());
				if (log.isTraceEnabled()) {
					log.error("Moderation exception", e);
				}
			}
		}
	}

	@Override
	public Set<User> getUnapprovedSpammers(User moderator) {
		List<ApprovalWorkflowBean> workflows = approvalManager.getUnApprovedWorkflowBeans(moderator.getID(), -1,
				JiveObjectModerator.Type.ABUSE);
		Set<User> users = new HashSet<User>();
		for (ApprovalWorkflowBean approvalWorkflowBean : workflows) {
			try {
				JiveObject jiveObject = jiveObjectLoader.getJiveObject((int) approvalWorkflowBean.getTypeID(),
						approvalWorkflowBean.getObjID());
				if (jiveObject instanceof UserAuthoredObject) {
					if (log.isTraceEnabled()) {
						log.trace("Adding " + ((UserAuthoredObject) jiveObject).getUser().getUsername()
								+ " user from object " + jiveObject);
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

	public void setBrowseQueryBuilderFactory(BrowseQueryBuilderFactory browseQueryBuilderFactory) {
		this.browseQueryBuilderFactory = browseQueryBuilderFactory;
	}
}
