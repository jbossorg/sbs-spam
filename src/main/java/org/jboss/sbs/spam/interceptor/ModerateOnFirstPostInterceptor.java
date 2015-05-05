package org.jboss.sbs.spam.interceptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jboss.sbs.spam.DbSpamManager;
import org.jboss.sbs.spam.SpamManager;
import org.openrdf.sail.rdbms.evaluation.QueryBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jivesoftware.base.User;
import com.jivesoftware.community.Community;
import com.jivesoftware.community.DocumentState;
import com.jivesoftware.community.ForumThread;
import com.jivesoftware.community.JiveConstants;
import com.jivesoftware.community.JiveContainer;
import com.jivesoftware.community.JiveContentObject;
import com.jivesoftware.community.JiveInterceptor;
import com.jivesoftware.community.JiveObject;
import com.jivesoftware.community.RejectedException;
import com.jivesoftware.community.JiveInterceptor.Type;
import com.jivesoftware.community.browse.sort.ModificationDateSort;
import com.jivesoftware.community.content.threads.ThreadBrowseQueryBuilder;
import com.jivesoftware.community.impl.DbDocumentManager;
import com.jivesoftware.community.impl.DbForumManager;
import com.jivesoftware.community.impl.DbForumMessage;
import com.jivesoftware.community.impl.ForumThreadContainableTypeManager;
import com.jivesoftware.community.lifecycle.JiveApplication;
import com.jivesoftware.community.lifecycle.spring.SpringJiveContextImpl;
import com.jivesoftware.community.mail.EmailMessage;
import com.jivesoftware.community.moderation.ModeratableType;
import com.jivesoftware.community.moderation.ModerationHelper;
import com.jivesoftware.community.objecttype.JiveObjectType;
import com.jivesoftware.community.objecttype.ObjectTypeManager;


public class ModerateOnFirstPostInterceptor implements JiveInterceptor {
	private static final Logger log = Logger.getLogger(ModerateOnFirstPostInterceptor.class);
	
	private SpamManager spamManager;
	private static final String REJECTIONMSG = "As this is your first activity here,"
			+ " your post will be first moderated by your administrator."
			+ " If everything is fine, your post will appear here shortly.";
	
	public ModerateOnFirstPostInterceptor() {
		SpringJiveContextImpl var = (SpringJiveContextImpl) JiveApplication.getContext();
		spamManager = var.getBean(DbSpamManager.class);
    }

    public ModerateOnFirstPostInterceptor(int objectType, long objectID) {
    	this();
    }
	
	/**
     * Returns the interception types the interceptor supports.
     *
     * @return a list of the interceptors interception types.
     */
	@Override
	public List<Type> getTypes() {
		List<Type> out = new ArrayList<Type>();
		out.add(Type.TYPE_POST);
		out.add(Type.TYPE_EDIT);
		//TODO overit, ci TYPE_EDIT nerobi kraviny
		return out;
	}
	
	/**
     * Invokes the interceptor on the specified jive object. The interceptor can either leave the 
     * jive object as it is, modify the object's content or throw a RejectedException to block it 
     * from being created/posted. Only a <tt>{@link Type#TYPE_PRE}</tt> interceptor can throw an 
     * exception.
     *
     * @param jiveObject the jive object to take action on.
     * @param type the type of interceptor to run
     * @throws RejectedException if the object should be prevented from being created/posted.
     */
	@Override
	public void invokeInterceptor(JiveObject object, Type type)
			throws RejectedException {
		log.info("INTERCEPTOR:");
		
		if (object instanceof JiveContentObject) {
			JiveContentObject contentObject = (JiveContentObject) object;
			
			User author = contentObject.getUser();
			
			log.info("triggered for "+((JiveContentObject) object).getPlainSubject());
			
			//TODO: review this comment
			//because interceptor is TYPE_POST (called AFTER creating a content), 
			//a number of created contents on time of creating a first content is always one.
			if(!spamManager.hasSomeContent(author, contentObject)){
				
				if(isModeratableType(object)){
					//non-moderatable types pass without moderation
					
					if (object instanceof JiveContentObject) {
						
						ModerationHelper moderationHelper = JiveApplication.getContext().getModerationHelper();
						moderationHelper.forceContentObjectModeration(contentObject);
			            
			            boolean moderated = moderationHelper.isInModeration(contentObject);
			            
			            if(moderated){
			            	log.info("invokeInterceptor(): moderation was successfully triggered on "+contentObject.getPlainSubject());
			            } else {
			            	log.warn("invokeInterceptor(): moderation failed on "+contentObject.getPlainSubject());
			            }
			        } else {
			        	log.info(((JiveContentObject) object).getPlainSubject()+" is not JiveContentObject");
			        }
				} else {
					log.info(((JiveContentObject) object).getPlainSubject()+" is not moderatable");
				}
			}
		}
	}
	
	/**
     * Returns whether this is a system level interceptor
     *
     * @return true if it is a system level interceptor.
     */
	@Override
	public boolean isSystemLevel() {
		return false;
	}
	
	public void setSpamManager(SpamManager spamManager){
		this.spamManager = spamManager;
	}
	
	//methods copied from KeywordInterceptor used to moderate a content:
	//either using system moderation
	//or just by sending emails
	
	/**
     * Figure out if this object is actually moderatable. No point in sending it through moderation if
     * it can be moderated.
     */
    private boolean isModeratableType(JiveObject jiveObject) {
        ObjectTypeManager objectTypeMananager = JiveApplication.getContext().getObjectTypeManager();
        JiveObjectType jot = objectTypeMananager.getObjectType(jiveObject.getObjectType());
        return (jot instanceof ModeratableType);
    }
    
    /*
    
    // Moderation - moderatable types that contain keywords are now handled when the SystemModerationInterceptor
    // runs and the moderation strategies are invoked, but we still need to handle non-moderatable types
    // like outcomes (which should get blocked).
    if (!isModeratableType(object) && moderationQueryString != null && keywordAnalyzer.hasMatch(moderationQueryString, text)) {
        // non-moderatable types should just be blocked
        throw new RejectedException(getBlockErrorMessage(), object);
    }

    // Email notifications.
    if (emailQueryString != null && emailList != null) {
        JiveContentObject content = resolveContentObject(object);
        // If we found a match, send email notifications.
        // Special case for Threads (to stop duplicate notifications - thread + message)
        if (content != null && keywordAnalyzer.hasMatch(emailQueryString, text) && !(content instanceof ForumThread)) {
            JiveContainer container =
                    JiveApplication.getContext().getJiveContainerManager().getJiveContainerFor(content);
            Locale locale = JiveApplication.getContext().getLocaleManager().getGlobaleLocale();

            // If we have a community, use that locale for mail notification
            if (container instanceof Community) {
                locale = ((Community) container).getFinalLocale();
            }

            // Loop through all the addresses in the notify list and send them an email.
            for (String toEmail : emailList) {
                EmailMessage em = new EmailMessage();
                em.setLocale(locale);
                em.addRecipient(null, toEmail);
                em.setTextBodyProperty(EMAIL_TEXT_BODY);
                em.setHtmlBodyProperty(EMAIL_HTML_BODY);
                em.setSubjectProperty(EMAIL_SUBJECT);
                populateContext(em.getContext(), content);
                populateObjectContext(em.getContext(), object);
                JiveApplication.getContext().getEmailManager().send(em);
            }
        }
    }
    
    */
}
