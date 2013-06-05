package org.jboss.sbs.spam.interceptor;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;


/**
 * Test for  GovernorStatusLevelInterceptor
 */
public class GovernorStatusLevelInterceptorTest {
	@Test
	public void testSetEmailDomainWhitelist() throws Exception {
		GovernorStatusLevelInterceptor interceptor = new GovernorStatusLevelInterceptor();

		Assert.assertArrayEquals(null, interceptor.emailDomainAllowed);

		interceptor.setEmailDomainWhitelist(null);
		Assert.assertArrayEquals(null, interceptor.emailDomainAllowed);

		interceptor.setEmailDomainWhitelist("");
		Assert.assertArrayEquals(null, interceptor.emailDomainAllowed);

		interceptor.setEmailDomainWhitelist("redhat.com");

		Assert.assertArrayEquals(new String[]{"redhat.com"}, interceptor.emailDomainAllowed);

		interceptor.setEmailDomainWhitelist("   redhat.com   ");
		Assert.assertArrayEquals(new String[]{"redhat.com"}, interceptor.emailDomainAllowed);

		interceptor.setEmailDomainWhitelist("redhat.com jboss.org");
		Assert.assertArrayEquals(new String[]{"redhat.com", "jboss.org"}, interceptor.emailDomainAllowed);

		interceptor.setEmailDomainWhitelist("  redhat.com    jboss.org  ");
		Assert.assertArrayEquals(new String[]{"redhat.com", "jboss.org"}, interceptor.emailDomainAllowed);
	}
}
