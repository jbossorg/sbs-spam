SBS Plugin - SPAM
=================
Plugin extends SBS feature "Report Abuse" and provides more easily way how to report all content of particular user.

Main features are:

1. Report all user's content as a spam by SBS feature report abuse
2. Disable user
3. If user doesn't have enough points then only standard "report abuse" to particular one document is reported
4. Moderator is notified about spam content in standard way like "report abuse" does.

Saying all users's content it means:

* Document
* Forum thread/message
* Blog post
* Bookmarks to external site


Configuration
-------------
Plugin can be configured via these properties managed in SBS admin console:

* `spam.reporter.min_points` - how many points user needs to have to be able to report a spammer.
Otherwise standard report abuse is used. Default value is 0 (all registered users).
