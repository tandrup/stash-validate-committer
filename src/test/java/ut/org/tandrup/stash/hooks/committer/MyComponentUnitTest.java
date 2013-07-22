package ut.org.tandrup.stash.hooks.committer;

import org.junit.Test;
import org.tandrup.stash.hooks.committer.MyPluginComponent;
import org.tandrup.stash.hooks.committer.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}