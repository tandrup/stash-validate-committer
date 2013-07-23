package ut.org.tandrup.stash.hooks.validatecommitter;

import static org.junit.Assert.*;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import java.util.Map;
import java.util.TreeMap;
import net.java.ao.EntityManager;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tandrup.stash.hooks.validatecommitter.PushedBy;

/**
 *
 * @author mtandrup
 */
@RunWith(ActiveObjectsJUnitRunner.class)
public class PushedByTest {
    
    private EntityManager entityManager;
 
    private ActiveObjects ao;
    
    @Before
    public void setUp() {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        ao.migrate(PushedBy.class);
    }
    
    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void create() {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put(PushedBy.CHANGESET_ID, "123");
        params.put(PushedBy.NAME, "name");
        params.put(PushedBy.DISPLAY_NAME, "displayName");
        params.put(PushedBy.EMAIL_ADDRESS, "emailAddress");

        PushedBy pushedBy = ao.create(PushedBy.class, params);
        assertNotNull(pushedBy);
        assertEquals(pushedBy.getChangesetId(), "123");
        assertEquals(pushedBy.getName(), "name");
        assertEquals(pushedBy.getDisplayName(), "displayName");
        assertEquals(pushedBy.getEmailAddress(), "emailAddress");
    }
}