/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tandrup.stash.hooks.validatecommitter;

import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.history.HistoryService;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.scm.CommandOutputHandler;
import com.atlassian.stash.scm.git.GitCommand;
import com.atlassian.stash.scm.git.GitScm;
import com.atlassian.stash.scm.git.GitScmCommandBuilder;
import com.atlassian.utils.process.LineOutputHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mtandrup
 */
public class RefChangeHelper {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final HistoryService historyService;
    private final GitScm gitScm;
    
    public RefChangeHelper(HistoryService historyService, GitScm gitScm) {
        this.historyService = historyService;
        this.gitScm = gitScm;
    }
    
    public Collection<Changeset> getNewChangesets(Repository repository, Collection<RefChange> refChanges) {
        final Collection<Changeset> result = new ArrayList<Changeset>();
        final Collection<String> hashPushed = getHashPushed(refChanges);
        if (!hashPushed.isEmpty()) {
            final List<String> changesetIds = getNewChangesetIds(repository, refChanges);
            for (String changesetId : changesetIds) {
                result.add(historyService.getChangeset(repository, changesetId));
            }
        }
        return result;
    }
    
    private Collection<String> getHashPushed(final Collection<RefChange> refChanges) {
        final Collection<String> result = new ArrayList();
        for (RefChange refChange : refChanges) {
            switch (refChange.getType()) {
                case UPDATE:
                    result.add(refChange.getFromHash() + ".." + refChange.getToHash());
                    break;
                case ADD:
                    result.add(refChange.getToHash());
                    break;
            }
        }
        return result;
    }
    
    public List<String> getExistingRefs(final Repository repository) {
        final GitScmCommandBuilder builder = gitScm.getCommandBuilderFactory().builder(repository);
        final GitCommand<List<String>> command = builder.forEachRef().format("%(refname)").pattern("refs/heads/").build(new ArrayOutputHandler());
        return (List<String>)command.call();
    }
    
    public List<String> getNewChangesetIds(final Repository repository, Collection<RefChange> refChanges) {
        final List<String> revListArgs = new ArrayList(getHashPushed(refChanges));
        final Set<String> existingRefs = new TreeSet<String>(getExistingRefs(repository));
        for (RefChange refChange : refChanges) {
            existingRefs.remove(refChange.getRefId());
        }
        for (String ignore : existingRefs) {
            revListArgs.add("^" + ignore);
        }
        final GitScmCommandBuilder builder = gitScm.getCommandBuilderFactory().builder(repository);
        final GitCommand<List<String>> revList = builder.revList().revs(revListArgs).build(new ArrayOutputHandler());
        List<String> result = (List<String>)revList.call();
        log.debug("New changeset IDs: {}", result);
        return result;
    }

    void debug(String prefix, Collection<RefChange> refChanges) {
        if (log.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder(prefix);
            for (RefChange refChange : refChanges) {
                msg.append('\n');
                msg.append('\t').append(refChange.getType());
                msg.append('\t').append(refChange.getRefId());
                msg.append('\t').append(refChange.getFromHash());
                msg.append('\t').append(refChange.getToHash());
            }
            log.debug(msg.toString());
        }
    }
    
    private static class ArrayOutputHandler extends LineOutputHandler implements CommandOutputHandler<List<String>> {
        List<String> output;
        
        public ArrayOutputHandler() {
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
