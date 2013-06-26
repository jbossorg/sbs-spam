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

package org.jboss.sbs.spam;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jboss.sbs.spam.filter.NofollowLinkFilter;

import com.jivesoftware.base.plugin.Plugin;
import com.jivesoftware.base.wiki.JiveLinkBuilder;
import com.jivesoftware.community.JiveGlobals;
import com.jivesoftware.community.renderer.impl.JiveGlobalRenderManager;

/**
 * @author Libor Krzyzanek
 * 
 */
public class SpamPlugin implements Plugin {

	private JiveGlobalRenderManager globalRenderManager;

	private JiveLinkBuilder builder;

	private Boolean nofollowLinkFilterRegistered = false;

	protected static final Logger log = LogManager.getLogger(SpamPlugin.class);

	public SpamPlugin() {
	}

	@Override
	public void initPlugin() {
		// known bug CS-10237 - init method is called three times. That's the reason of this synchronized stuff
		log.info("initPlugin started.");
		synchronized (nofollowLinkFilterRegistered) {
			// filter cannot be initialized by spring because globelRendererManager return it in getRenderPlugin
			// although it's
			// not in list of renderers
			NofollowLinkFilter nofollowLinkFilter = new NofollowLinkFilter();
			nofollowLinkFilter.setBuilder(builder);

			final String filterName = nofollowLinkFilter.getName();

			// It's needed to set filter properties because SBS doesn't set it after restart even value is stored in
			// jive properties.
			boolean enabled = JiveGlobals.getJiveBooleanProperty("globalRenderManager." + filterName + ".enabled",
					nofollowLinkFilter.isEnabled());
			nofollowLinkFilter.setEnabled(enabled);

			String domainWhiteList = JiveGlobals.getJiveProperty("globalRenderManager." + filterName
					+ ".domainWhiteList");
			nofollowLinkFilter.setDomainWhiteList(domainWhiteList);

			log.debug("registered plugin: " + globalRenderManager.getRenderPlugin(filterName));

			if (!nofollowLinkFilterRegistered && globalRenderManager.getRenderPlugin(filterName) == null) {
				globalRenderManager.addRenderPlugin(nofollowLinkFilter);
				log.info(filterName + " filter successfully registered.");
			} else {
				log.info(filterName + " filter already registered.");
			}
			nofollowLinkFilterRegistered = true;
		}
	}

	@Override
	public void destroy() {
	}

	public void setGlobalRenderManager(JiveGlobalRenderManager globalRenderManager) {
		this.globalRenderManager = globalRenderManager;
	}

	public void setBuilder(JiveLinkBuilder builder) {
		this.builder = builder;
	}

}
