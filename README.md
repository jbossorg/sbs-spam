Jive SBS Plugin - SPAM
======================

Report Spam link
----------------
Plugin extends SBS feature "Report Abuse" and provides more easily way how to report all content of particular user.

Main features are:

1. Report all user's content as a spam by SBS feature report abuse
2. Disable user
3. If user doesn't have enough points then only standard "report abuse" to particular one document is reported
4. Moderator is notified about spam content in standard way like "report abuse" does.

Saying all user's content it means:

* Document
* Forum thread/message
* Blog post
* Bookmarks to external site


### Configuration ###
Plugin can be configured via these properties managed in SBS admin console:

* `spam.reporter.min_points` - how many points user needs to have to be able to report a spammer.
Otherwise standard report abuse is used. Default value is 0 (all registered users).


Content Governor
----------------
Content governor extends standard [Message Governor](http://docs.jivesoftware.com/jive_sbs/4.5/index.jsp?topic=/com.jivesoftware.help.sbs.online_4.5/admin/ConfiguringInterceptors.html)
feature and allows to not affect experienced users (based on status level)

### Installation and configuration ###
Governor is implemented as standard SBS interceptor. You need to install (after plugin installation).

Go to SBS admin console and then navigate to Spaces > Settings > Interceptor
Put into field "Class Name" this value `org.jboss.sbs.spam.interceptor.GovernorStatusLevelInterceptor` and hit Add Interceptor.

After this installation you can add this interceptor to the list of active interceptors in standard way.

These properties can be configured in interceptor:
* Post Interval
* Points Level
* Rejection Message
* E-mail white-list
* Security group white-list

No follow links filter
----------------------
Post processing filter which adds `re="nofollow"` attribute to all links except those listed in white list.
It's enabled by default.
If you want to disable it just go to `Admin console > Spaces > Settings > Filter and Macros` and turn off this filter


First Post Moderation
---------------------
First Post Moderation interceptor allows to automatically send the first content of a user to moderation. Once the new user attempts to post a Document, Blog post, Forum message or new thread for the first time, it sends the content to be moderated by administrator.

The interceptor excludes the content that is already in moderation, so as long as no user's content has been approved by administrator, all the new content produced by the user will be sent to moderation.

### Installation ###
First Post Moderation is implemented as standard SBS interceptor. You will need to install it similarly to previous:
Go to SBS admin console and then navigate to Spaces > Settings > Interceptor
Into the field `Class Name` put this value: `org.jboss.sbs.spam.interceptor.ModerateOnFirstPostInterceptor` and hit Add Interceptor.

After that you can add this interceptor to the list of active interceptors in standard way.

This interceptor does not contain any configurable properties.




