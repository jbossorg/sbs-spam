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
import java.util.List;
import java.util.Map;

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
@PropertyNames({ "enabled" })
@PersistSettings(true)
public class NofollowLinkFilter extends AbstractRenderFilter implements PostProcessingRenderFilter {

    private static Map<String, String[]> documentation = new HashMap<String, String[]>();
    private static String[] DEFAULT_HELP = new String[] { "", "", "" };

    protected static final Logger log = LogManager.getLogger(NofollowLinkFilter.class);

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
            if (log.isDebugEnabled()) {
                log.debug("Setting rel to this anchor: " + element);
            }
            if (shouldProcess(element, renderContext)) {
                element.setAttribute("rel", "nofollow");
                log.debug("done.");
            }
        }

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

}
