/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tandrup.stash.hooks.validatecommitter;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.user.Person;
import com.atlassian.stash.user.StashAuthenticationContext;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 *
 * @author mtandrup
 */
public class ChangesetPanel implements WebPanel {
    private final StashAuthenticationContext stashAuthenticationContext;
    private final ActiveObjects ao;

    public ChangesetPanel(StashAuthenticationContext stashAuthenticationContext, ActiveObjects ao) {
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.ao = ao;
    }
    
    @Override
    public String getHtml(Map<String, Object> map) {
        Changeset changeset = (Changeset) map.get("changeset");
        PushedBy pushedBy = ao.get(PushedBy.class, changeset.getId());
        if (pushedBy != null && pushedBy.getEmailAddress() != null) {
            Person author = changeset.getAuthor();
            if (!pushedBy.getEmailAddress().equalsIgnoreCase(author.getEmailAddress())) {
                return "<dl>"
                        + "<span class=\"aui-icon aui-icon-warning\">Warning</span>"
                        + "Pushed by " + pushedBy.getEmailAddress() + ".<br/>But committer is " + author.getEmailAddress() + "."
                        + "</dl>";
            }
        }
        return "";
    }

    @Override
    public void writeHtml(Writer writer, Map<String, Object> map) throws IOException {
        writer.append(getHtml(map));
    }
}
