/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.web.toolkit.client.ui.component.form.entry;

import org.bonitasoft.web.toolkit.client.ui.JsId;

/**
 * @author Séverin Moussel
 * 
 */
public class Password extends Text {

    public Password(final JsId jsid, final String label, final String tooltip, final String description, final String example) {
        super(jsid, label, tooltip, description, example);
    }

    public Password(final JsId jsid, final String label, final String tooltip, final String description) {
        super(jsid, label, tooltip, description);
    }

    public Password(final JsId jsid, final String label, final String tooltip) {
        super(jsid, label, tooltip);
    }

    @Override
    protected String getInputType() {
        return "password";
    }

}
