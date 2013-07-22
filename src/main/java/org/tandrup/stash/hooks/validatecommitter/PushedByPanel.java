/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tandrup.stash.hooks.validatecommitter;

import com.atlassian.plugin.web.model.WebPanel;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 *
 * @author mtandrup
 */
public class PushedByPanel implements WebPanel {

    @Override
    public String getHtml(Map<String, Object> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.append(entry.getKey());
            result.append(" = ");
            result.append(entry.getValue());
            result.append("<br/>");
        }
        return result.toString();
    }

    @Override
    public void writeHtml(Writer writer, Map<String, Object> map) throws IOException {
        writer.append(getHtml(map));
    }
}
