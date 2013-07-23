package org.tandrup.stash.hooks.validatecommitter;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.history.HistoryService;
import com.atlassian.stash.hook.*;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.scm.CommandOutputHandler;
import com.atlassian.stash.scm.git.GitCommand;
import com.atlassian.stash.scm.git.GitScm;
import com.atlassian.stash.scm.git.GitScmCommandBuilder;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Person;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.utils.process.LineOutputHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ValidateCommitterHook implements PreReceiveRepositoryHook
{
    public static final String VALIDATE_COMMITTER_NAME = "validateCommitterName";
    public static final String VALIDATE_COMMITTER_EMAIL = "validateComitterEmail";
    public static final String EXEMPTED_USERS = "exemptedUsers";
    
    private final StashAuthenticationContext stashAuthenticationContext;
    private final HistoryService historyService;
    private final GitScm gitScm;
    private final ActiveObjects ao;

    public ValidateCommitterHook(StashAuthenticationContext stashAuthenticationContext, HistoryService historyService, GitScm gitScm, ActiveObjects ao) {
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.historyService = historyService;
        this.gitScm = gitScm;
        this.ao = ao;
    }

    @Override
    public boolean onReceive(RepositoryHookContext context, Collection<RefChange> refChanges, HookResponse response) {
        
        final Map<String, Person> rejectedRefs = new HashMap<String, Person>();
        final StashUser currentUser = stashAuthenticationContext.getCurrentUser();
        final List<String> pushedRefs = getRefsPushed(refChanges);
        if (!pushedRefs.isEmpty()) {
            final List<String> ignoreRefs = getExistingRefs(context.getRepository());
            final List<String> brandNewRevs = revList(context.getRepository(), pushedRefs, ignoreRefs);
            for (String refId : brandNewRevs) {
                final Changeset changeset = historyService.getChangeset(context.getRepository(), refId);
                recordPushedBy(changeset, currentUser);
                final Person author = changeset.getAuthor();
                if (!validateCommitter(context.getSettings(), author, currentUser)) {
                    rejectedRefs.put(refId, author);
                }
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
    
    private void recordPushedBy(Changeset changeset, StashUser currentUser) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put(PushedBy.CHANGESET_ID, changeset.getId());
        params.put(PushedBy.NAME, currentUser.getName());
        params.put(PushedBy.DISPLAY_NAME, currentUser.getDisplayName());
        params.put(PushedBy.EMAIL_ADDRESS, currentUser.getEmailAddress());
        PushedBy pushedBy = ao.create(PushedBy.class, params);
        System.out.println("Created pushed by: " + pushedBy.getChangesetId());
    }
    
    private List<String> getRefsPushed(final Collection<RefChange> refChanges) {
        final List<String> toList = new ArrayList();
        for (RefChange refChange : refChanges) {
            if (refChange.getType().equals(RefChangeType.UPDATE)) {
                toList.add(refChange.getFromHash() + ".." + refChange.getToHash());
            }
            else if (refChange.getType().equals(RefChangeType.ADD)) {
                toList.add(refChange.getToHash());
            }
        }
        return toList;
    }
    
     public List<String> getExistingRefs(final Repository repository) {
        final GitScmCommandBuilder builder = this.gitScm.getCommandBuilderFactory().builder(repository);
        final GitCommand<List<String>> command = builder.forEachRef().format("%(refname:short)").pattern("refs/heads/").build(new OnePerLineCommandOutputHandler());
        return (List<String>)command.call();
    }
     
     public List<String> revList(final Repository repository, final List<String> refs, final List<String> ignoreReachableFrom) {
        final GitScmCommandBuilder builder = this.gitScm.getCommandBuilderFactory().builder(repository);
        final List<String> revListArgs = new ArrayList();
        revListArgs.addAll(refs);
        for (String ignore : ignoreReachableFrom) {
            revListArgs.add("^" + ignore);
        }
        final GitCommand<List<String>> revList = builder.revList().revs(revListArgs).build(new OnePerLineCommandOutputHandler());
        return (List<String>)revList.call();
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
    
    private static class OnePerLineCommandOutputHandler extends LineOutputHandler implements CommandOutputHandler<List<String>> {
        List<String> output;

        public OnePerLineCommandOutputHandler() {
            super();
            this.output = new ArrayList();
        }

        public List<String> getOutput() {
            return this.output;
        }

        protected void processLine(int i, String s) {
            this.output.add(s);
        }
    }
}
