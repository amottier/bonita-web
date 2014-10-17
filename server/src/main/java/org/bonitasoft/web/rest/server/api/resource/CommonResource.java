/**
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.web.rest.server.api.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.rest.server.datastore.filter.Filters;
import org.bonitasoft.web.rest.server.datastore.utils.SearchOptionsCreator;
import org.bonitasoft.web.rest.server.datastore.utils.Sorts;
import org.bonitasoft.web.rest.server.framework.APIServletCall;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.json.JSONObject;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ServerResource;

/**
 * @author Emmanuel Duchastenier
 */
public class CommonResource extends ServerResource {

    private APISession sessionSingleton = null;

    protected String toJson(final Object p) {
        return new JSONObject(p).toString();
    }

    protected <T> String toJsonArray(final List<T> list) {
        final List<JSONObject> jsonObj = new ArrayList<JSONObject>(list.size());
        for (final T object : list) {
            jsonObj.add(new JSONObject(object));
        }
        return jsonObj.toString();
    }

    /**
     * Get the tenant session to access the engine APIs
     */
    protected APISession getEngineSession() {
        if (sessionSingleton == null) {
            sessionSingleton = (APISession) ServletUtils.getRequest(getRequest()).getSession().getAttribute("apiSession");
        }
        return sessionSingleton;
    }

    protected ProcessAPI getEngineProcessAPI() {
        try {
            return TenantAPIAccessor.getProcessAPI(getEngineSession());
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    protected ProfileAPI getEngineProfileAPI() {
        try {
            return TenantAPIAccessor.getProfileAPI(getEngineSession());
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    protected Map<String, String> getSearchFilters() {
        return parseFilters(getParameterAsList(APIServletCall.PARAMETER_FILTER, null));
    }

    /**
     * Builds a map where keys are Engine constants defining filter keys, and values are values corresponding to those keys.
     *
     * @param parameters The filters passed as string according to the form ["key1=value1", "key2=value2"].
     * @return a map of the form: [key1: value1, key2: value2].
     */
    protected Map<String, String> parseFilters(final List<String> parameters) {
        if (parameters == null) {
            return null;
        }
        final Map<String, String> results = new HashMap<String, String>();
        for (final String parameter : parameters) {
            final String[] split = parameter.split("=");
            if (split.length < 2) {
                results.put(split[0], null);
            } else {
                results.put(split[0], split[1]);
            }
        }
        return results;
    }

    protected String getSearchOrder() {
        return getParameter(APIServletCall.PARAMETER_ORDER, false);
    }

    protected int getSearchPageNumber() {
        return getIntegerParameter(APIServletCall.PARAMETER_PAGE, false);
    }

    protected int getSearchPageSize() {
        return getIntegerParameter(APIServletCall.PARAMETER_LIMIT, false);
    }

    protected String getSearchTerm() {
        return getParameter(APIServletCall.PARAMETER_SEARCH, false);
    }

    protected Integer getIntegerParameter(final String parameterName, final boolean mandatory) {
        final String parameterValue = getParameter(parameterName, mandatory);
        if (parameterValue != null) {
            return Integer.parseInt(parameterValue);
        }
        return null;
    }

    protected Long getLongParameter(final String parameterName, final boolean mandatory) {
        final String parameterValue = getParameter(parameterName, mandatory);
        if (parameterValue != null) {
            return Long.parseLong(parameterValue);
        }
        return null;
    }

    //    @SuppressWarnings("unchecked")
    //    protected <T> T getMandatoryParameter(final Class<T> parameterType, final String parameterName) {
    //        final String parameter = getParameter(parameterName);
    //        verifyNotNullParameter(parameter, parameterName);
    //        return (T) parameter;
    //    }

    protected String getParameter(final String parameterName, final boolean mandatory) {
        final String parameter = getRequestParameter(parameterName);
        if (mandatory) {
            verifyNotNullParameter(parameter, parameterName);
        }
        return parameter;
    }

    protected String getRequestParameter(final String parameterName) {
        return ServletUtils.getRequest(getRequest()).getParameter(parameterName);
    }

    protected void verifyNotNullParameter(final Object parameter, final String parameterName) throws APIException {
        if (parameter == null) {
            throw new APIException("Parameter " + parameterName + " is mandatory.");
        }
    }

    /**
     * Get a list of parameter values by name.
     *
     * @param name
     *        The name of the parameter (case sensitive).
     * @param defaultValue
     *        The value to return if the parameter is not defined.
     * @return This method returns the values of a parameter as a list of String.
     */
    protected List<String> getParameterAsList(final String name, final String defaultValue) {
        final String[] parameterValues = ServletUtils.getRequest(getRequest()).getParameterValues(name);
        if (parameterValues != null && parameterValues.length > 0) {
            return Arrays.asList(parameterValues);
        }
        //        if (defaultValue != null) {
        //            final List<String> results = new ArrayList<String>();
        //            results.add(defaultValue);
        //            return results;
        //        }
        return null;
    }

    protected SearchOptions buildSearchOptions() {
        return new SearchOptionsCreator(getSearchPageNumber(), getSearchPageSize(), getSearchTerm(), new Sorts(
                getSearchOrder()), new Filters(getSearchFilters())).create();
    }
}
