package org.jboss.sbs.spam.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.sbs.spam.DbSpamManager;
import org.jboss.sbs.spam.SpamManager;

import com.jivesoftware.base.User;
import com.jivesoftware.community.JiveContentObject;
import com.jivesoftware.community.JiveInterceptor;
import com.jivesoftware.community.JiveObject;
import com.jivesoftware.community.RejectedException;
import com.jivesoftware.community.annotations.PropertyNames;
import com.jivesoftware.community.lifecycle.JiveApplication;
import com.jivesoftware.community.lifecycle.spring.SpringJiveContextImpl;
import com.jivesoftware.community.moderation.ModeratableType;
import com.jivesoftware.community.moderation.ModerationHelper;
import com.jivesoftware.community.objecttype.JiveObjectType;
import com.jivesoftware.community.objecttype.ObjectTypeManager;

/**
 * Interceptor triggers moderation on a new (or edited) object if the author has
 * no (other) content posted yet
 * 
 * @author Michal Stefanik
 *
 */
@PropertyNames({})
public class ModerateOnFirstPostInterceptor implements JiveInterceptor {

	private static final Logger log = Logger.getLogger(ModerateOnFirstPostInterceptor.class);

	private SpamManager spamManager;

	public ModerateOnFirstPostInterceptor() {
		SpringJiveContextImpl var = (SpringJiveContextImpl) JiveApplication.getContext();
		spamManager = var.getBean(DbSpamManager.class);
	}

	public ModerateOnFirstPostInterceptor(int objectType, long objectID) {
		this();
	}

	@Override
	public List<Type> getTypes() {
		List<Type> out = new ArrayList<Type>();
		out.add(Type.TYPE_POST);
		out.add(Type.TYPE_EDIT);
		return out;
	}

	@Override
	public void invokeInterceptor(JiveObject object, Type type)
			throws RejectedException {
		log.info("INTERCEPTOR type triggered: " + type.name());

		if (object instanceof JiveContentObject) {
			JiveContentObject contentObject = (JiveContentObject) object;

			if (log.isDebugEnabled()) {
				log.debug("triggered for object " + contentObject.getSubject()
						+ "type: " + object.getClass().getName() + " status: "
						+ contentObject.getStatus());
			}
			User author = contentObject.getUser();

			if (!spamManager.hasSomeContent(author, contentObject)) {

				if (isModeratableType(object)) {
					// non-moderatable types pass without moderation

					if (object instanceof JiveContentObject) {

						ModerationHelper moderationHelper = JiveApplication.getContext().getModerationHelper();
						moderationHelper.forceContentObjectModeration(contentObject);
						
						//verify that moderation was triggered
						boolean moderated = moderationHelper.isInModeration(contentObject);

						if (moderated) {
							log.info("invokeInterceptor(): moderation successfully triggered on "
									+ contentObject.getPlainSubject());
						} else {
							log.warn("invokeInterceptor(): moderation failed on "
									+ contentObject.getPlainSubject());
						}
					} else {
						log.debug(((JiveContentObject) object)
								.getPlainSubject()
								+ " is not JiveContentObject");
					}
				} else {
					log.debug(((JiveContentObject) object).getPlainSubject()
							+ " is not moderatable");
				}
			}
		}
	}

	@Override
	public boolean isSystemLevel() {
		return false;
	}

	/**
	 * Decides whether the new object's type is moderatable.
	 * 
	 * @param jiveObject relevant object
	 */
	private boolean isModeratableType(JiveObject jiveObject) {
		ObjectTypeManager objectTypeMananager = JiveApplication.getContext()
				.getObjectTypeManager();
		JiveObjectType jot = objectTypeMananager.getObjectType(jiveObject
				.getObjectType());
		return (jot instanceof ModeratableType);
	}

	public void setSpamManager(SpamManager spamManager) {
		this.spamManager = spamManager;
	}
}
