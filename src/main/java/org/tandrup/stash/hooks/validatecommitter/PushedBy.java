/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tandrup.stash.hooks.validatecommitter;

import net.java.ao.Preload;
import net.java.ao.Accessor;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

/**
 *
 * @author mtandrup
 */
@Preload
public interface PushedBy extends RawEntity<String> {
    static final String CHANGESET_ID = "CHANGESET_ID";
    static final String NAME = "NAME";
    static final String DISPLAY_NAME = "DISPLAY_NAME";
    static final String EMAIL_ADDRESS = "EMAIL_ADDRESS";

    @PrimaryKey(CHANGESET_ID)
    @StringLength(40)
    String getChangesetId();

    @Accessor(NAME)
    @NotNull
    String getName();

    @Accessor(DISPLAY_NAME)
    String getDisplayName();

    @Accessor(EMAIL_ADDRESS)
    String getEmailAddress();
}
