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
import com.jivesoftware.community.lifecycle.JiveApplication;
import com.jivesoftware.community.lifecycle.spring.SpringJiveContextImpl;
import com.jivesoftware.community.moderation.ModeratableType;
import com.jivesoftware.community.moderation.ModerationHelper;
import com.jivesoftware.community.objecttype.JiveObjectType;
import com.jivesoftware.community.objecttype.ObjectTypeManager;


public class ModerateOnFirstPostInterceptor implements JiveInterceptor {
	//this interceptor triggers moderation on a new (or edited) object if the author has no (other) content yet posted
	
	private static final Logger log = Logger.getLogger(ModerateOnFirstPostInterceptor.class);
	
	private SpamManager spamManager;
	
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
		log.info("INTERCEPTOR type: "+type.name());
		
		if (object instanceof JiveContentObject) {
			JiveContentObject contentObject = (JiveContentObject) object;
			
			log.debug("triggered for object "+contentObject.getSubject()+" status: "+contentObject.getStatus());
			
			User author = contentObject.getUser();
			
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
			        	log.debug(((JiveContentObject) object).getPlainSubject()+" is not JiveContentObject");
			        }
				} else {
					log.debug(((JiveContentObject) object).getPlainSubject()+" is not moderatable");
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
	
	/**
     * Figure out if this object is actually moderatable. No point in sending content to moderation if
     * it can not be moderated.
     */
    private boolean isModeratableType(JiveObject jiveObject) {
        ObjectTypeManager objectTypeMananager = JiveApplication.getContext().getObjectTypeManager();
        JiveObjectType jot = objectTypeMananager.getObjectType(jiveObject.getObjectType());
        return (jot instanceof ModeratableType);
    }

}
