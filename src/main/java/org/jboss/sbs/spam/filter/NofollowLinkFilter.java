/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.sbs.spam.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.jivesoftware.base.wiki.JiveHtmlElement;
import com.jivesoftware.base.wiki.JiveLinkBuilder;
import com.jivesoftware.community.annotations.PropertyNames;
import com.jivesoftware.community.renderer.AbstractRenderFilter;
import com.jivesoftware.community.renderer.PostProcessingRenderFilter;
import com.jivesoftware.community.renderer.RenderContext;
import com.jivesoftware.community.renderer.annotations.PersistSettings;
import com.jivesoftware.community.renderer.impl.v2.JAXPUtils;

/**
 * @author Libor Krzyzanek
 * 
 */
@PropertyNames({ "enabled", "domainWhiteList" })
@PersistSettings(true)
public class NofollowLinkFilter extends AbstractRenderFilter implements PostProcessingRenderFilter {

	private static Map<String, String[]> documentation = new HashMap<String, String[]>();
	private static String[] DEFAULT_HELP = new String[] { "", "", "" };

	protected static final Logger log = LogManager.getLogger(NofollowLinkFilter.class);

	private Set<String> domainsWhiteList = new HashSet<String>();

	private String domainWhiteList;

	@Override
	public String getName() {
		return "NofollowLinkFilter";
	}

	private JiveLinkBuilder builder;

	@Override
	public void execute(Document document, RenderContext renderContext) {
		if (!isEnabled()) {
			log.debug("Filter is not enabled");
			return;
		}

		List<Element> elementList = JAXPUtils.selectAllNodes(document, JiveHtmlElement.Anchor.getTag());

		for (Element element : elementList) {
			String addr = element.getAttribute("href");
			if (log.isDebugEnabled()) {
				log.debug("Setting rel to this anchor with address: " + addr);
			}

			if (isOnWhitelist(addr)) {
				log.debug("Address is on white list. Skipping no follow attribute.");
				continue;
			}

			if (shouldProcess(element, renderContext)) {
				element.setAttribute("rel", "nofollow");
				log.debug("done.");
			}
		}
	}

	public boolean isOnWhitelist(String addr) {
		for (String a : domainsWhiteList) {
			if (addr.startsWith(a)) {
				return true;
			}
		}
		return false;
	}

	public JiveLinkBuilder getBuilder() {
		return builder;
	}

	public void setBuilder(JiveLinkBuilder builder) {
		this.builder = builder;
	}

	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}

	@Override
	protected Map<String, String[]> getDocumentationMap() {
		return documentation;
	}

	@Override
	protected String[] getDefaultHelp() {
		return DEFAULT_HELP;
	}

	@Override
	public int getOrder() {
		return 100;
	}

	public Set<String> getDomainsWhiteList() {
		return domainsWhiteList;
	}

	public void setDomainsWhiteList(Set<String> domainsWhiteList) {
		this.domainsWhiteList = domainsWhiteList;
	}

	public String getDomainWhiteList() {
		return domainWhiteList;
	}

	public void setDomainWhiteList(String domainWhiteList) {
		this.domainWhiteList = domainWhiteList;
		domainsWhiteList = new HashSet<String>();

		if (domainWhiteList != null) {
			StringTokenizer tokenizer = new StringTokenizer(domainWhiteList, " ");
			while (tokenizer.hasMoreTokens()) {
				String d = tokenizer.nextToken();
				domainsWhiteList.add("http://" + d);
				domainsWhiteList.add("https://" + d);
			}
		}
		log.debug("Domain white list initialized based on this setting: " + domainWhiteList);
		log.debug("Result is: " + domainsWhiteList);
	}

}
