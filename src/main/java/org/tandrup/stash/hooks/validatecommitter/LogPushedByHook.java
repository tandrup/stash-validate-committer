package org.tandrup.stash.hooks.validatecommitter;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.setting.*;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogPushedByHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final StashAuthenticationContext stashAuthenticationContext;
    private final ActiveObjects ao;
    private final RefChangeHelper refChangeHelper;

    public LogPushedByHook(StashAuthenticationContext stashAuthenticationContext, ActiveObjects ao, RefChangeHelper refChangeHelper) {
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.ao = ao;
        this.refChangeHelper = refChangeHelper;
    }
    
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges)
    {
        refChangeHelper.debug("Post-receive", refChanges);

        for (RefChange refChange : refChanges) {
            System.out.println(refChange.getRefId() + "\t" + refChange.getFromHash() + "\t" + refChange.getToHash() + "\t" + refChange.getType());
        }
        
        final StashUser currentUser = stashAuthenticationContext.getCurrentUser();
        final Collection<Changeset> newChangesets = refChangeHelper.getNewChangesets(context.getRepository(), refChanges);
        for (Changeset changeset : newChangesets) {
            recordPushedBy(changeset, currentUser);
        }
    }
    
    private void recordPushedBy(Changeset changeset, StashUser currentUser) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put(PushedBy.CHANGESET_ID, changeset.getId());
        params.put(PushedBy.NAME, currentUser.getName());
        params.put(PushedBy.DISPLAY_NAME, currentUser.getDisplayName());
        params.put(PushedBy.EMAIL_ADDRESS, currentUser.getEmailAddress());
        params.put(PushedBy.DATE, new Date());
        PushedBy pushedBy = ao.create(PushedBy.class, params);
    }
    
    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository)
    {
        if (settings.getString("url", "").isEmpty())
        {
            //errors.addFieldError("url", "Url field is blank, please supply one");
        }
    }
}