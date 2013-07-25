package org.tandrup.stash.hooks.validatecommitter;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.history.HistoryService;
import com.atlassian.stash.hook.*;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.scm.git.GitScm;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Person;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateCommitterHook implements PreReceiveRepositoryHook
{
    public static final String VALIDATE_COMMITTER_NAME = "validateCommitterName";
    public static final String VALIDATE_COMMITTER_EMAIL = "validateComitterEmail";
    public static final String EXEMPTED_USERS = "exemptedUsers";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final StashAuthenticationContext stashAuthenticationContext;
    private final HistoryService historyService;
    private final GitScm gitScm;
    private final ActiveObjects ao;
    private final RefChangeHelper refChangeHelper;
    
    public ValidateCommitterHook(StashAuthenticationContext stashAuthenticationContext, HistoryService historyService, GitScm gitScm, ActiveObjects ao, RefChangeHelper refChangeHelper) {
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.historyService = historyService;
        this.gitScm = gitScm;
        this.ao = ao;
        this.refChangeHelper = refChangeHelper;
    }
    
    @Override
    public boolean onReceive(RepositoryHookContext context, Collection<RefChange> refChanges, HookResponse response) {
        refChangeHelper.debug("Pre-receive", refChanges);
        
        final Map<String, Person> rejectedRefs = new HashMap<String, Person>();
        final StashUser currentUser = stashAuthenticationContext.getCurrentUser();
        final Collection<Changeset> newChangesets = refChangeHelper.getNewChangesets(context.getRepository(), refChanges);
        for (Changeset changeset : newChangesets) {
            final Person author = changeset.getAuthor();
            if (!validateCommitter(context.getSettings(), author, currentUser)) {
                rejectedRefs.put(changeset.getDisplayId(), author);
            }
        }
        
        if (!rejectedRefs.isEmpty()) {
            response.err().println("========= WARNING: =========");
            response.err().println("The following commits has a committer that does not match your profile.");
            for (Map.Entry<String, Person> entry : rejectedRefs.entrySet()) {
                response.err().format("%s %s <%s>\n", entry.getKey(), entry.getValue().getName(), entry.getValue().getEmailAddress());
            }
            response.err().println("Check your git settings.");
            response.err().println("============================");
        }
        
        return true;
    }
    
    private boolean validateCommitter(Settings settings, Person author, StashUser currentUser) {
        for (String userName : getExemptedUsers(settings)) {
            if (userName.equalsIgnoreCase(currentUser.getName())) {
                return true;
            }
        }
        
        if (!currentUser.getEmailAddress().equalsIgnoreCase(author.getEmailAddress())) {
            return false;
        }
        
        if (!currentUser.getDisplayName().equalsIgnoreCase(author.getName())) {
            return false;
        }
        
        return true;
    }
    
    private static Collection<String> getExemptedUsers(Settings settings) {
        String userString = settings.getString(EXEMPTED_USERS);
        if (userString != null && !userString.trim().isEmpty()) {
            return Arrays.asList(userString.split(","));
        }
        return Collections.emptyList();
    }
}
