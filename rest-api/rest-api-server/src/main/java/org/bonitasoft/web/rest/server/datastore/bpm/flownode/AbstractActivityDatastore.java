/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.web.rest.server.datastore.bpm.flownode;

import static org.bonitasoft.web.toolkit.client.common.i18n.AbstractI18n._;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.rest.model.bpm.flownode.ActivityDefinition;
import org.bonitasoft.web.rest.model.bpm.flownode.ActivityItem;
import org.bonitasoft.web.rest.model.bpm.flownode.FlowNodeItem;
import org.bonitasoft.web.rest.model.bpm.flownode.HumanTaskItem;
import org.bonitasoft.web.rest.server.engineclient.ActivityEngineClient;
import org.bonitasoft.web.rest.server.engineclient.EngineAPIAccessor;
import org.bonitasoft.web.rest.server.engineclient.EngineClientFactory;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasGet;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasUpdate;
import org.bonitasoft.web.rest.server.framework.json.JacksonDeserializer;
import org.bonitasoft.web.rest.server.framework.utils.SearchOptionsBuilderUtil;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIItemNotFoundException;
import org.bonitasoft.web.toolkit.client.common.texttemplate.Arg;
import org.bonitasoft.web.toolkit.client.common.util.MapUtil;
import org.bonitasoft.web.toolkit.client.common.util.StringUtil;
import org.bonitasoft.web.toolkit.client.data.APIID;

/**
 * @author Séverin Moussel
 * 
 */
public class AbstractActivityDatastore<CONSOLE_ITEM extends ActivityItem, ENGINE_ITEM extends ActivityInstance>
        extends AbstractFlowNodeDatastore<CONSOLE_ITEM, ENGINE_ITEM>
        implements DatastoreHasGet<CONSOLE_ITEM>,
        DatastoreHasUpdate<CONSOLE_ITEM>
{

    public AbstractActivityDatastore(final APISession engineSession) {
        super(engineSession);
    }

    /**
     * Fill a console item using the engine item passed.
     * 
     * @param result
     *            The console item to fill
     * @param item
     *            The engine item to use for filling
     * @return This method returns the result parameter passed.
     */
    protected static ActivityItem fillConsoleItem(final ActivityItem result, final ActivityInstance item) {
        FlowNodeDatastore.fillConsoleItem(result, item);

        result.setReachStateDate(item.getReachedStateDate());
        result.setLastUpdateDate(item.getLastUpdateDate());

        return result;
    }

    @Override
    public CONSOLE_ITEM get(final APIID id) {
        try {
            @SuppressWarnings("unchecked")
            final ENGINE_ITEM activityInstance = (ENGINE_ITEM) getProcessAPI().getActivityInstance(id.toLong());
            return convertEngineToConsoleItem(activityInstance);
        } catch (final ActivityInstanceNotFoundException e) {
            throw new APIItemNotFoundException(ActivityDefinition.TOKEN, id);
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    @Override
    protected SearchResult<ENGINE_ITEM> runSearch(final SearchOptionsBuilder builder, final Map<String, String> filters) {
        try {
            @SuppressWarnings("unchecked")
            final SearchResult<ENGINE_ITEM> results = (SearchResult<ENGINE_ITEM>) getProcessAPI().searchActivities(builder.done());
            return results;
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    @Override
    protected SearchOptionsBuilder makeSearchOptionBuilder(final int page, final int resultsByPage, final String search, final String orders,
            final Map<String, String> filters) {

        final SearchOptionsBuilder builder = SearchOptionsBuilderUtil.buildSearchOptions(page, resultsByPage, orders, search);

        addFilterToSearchBuilder(filters, builder, ActivityItem.ATTRIBUTE_CASE_ID, ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID);
        addFilterToSearchBuilder(filters, builder, ActivityItem.ATTRIBUTE_PROCESS_ID, ActivityInstanceSearchDescriptor.PROCESS_DEFINITION_ID);
        addFilterToSearchBuilder(filters, builder, ActivityItem.ATTRIBUTE_STATE, ActivityInstanceSearchDescriptor.STATE_NAME);
        addFilterToSearchBuilder(filters, builder, ActivityItem.ATTRIBUTE_TYPE, ActivityInstanceSearchDescriptor.ACTIVITY_TYPE, new FlowNodeTypeConverter());
        addFilterToSearchBuilder(filters, builder, ActivityItem.FILTER_SUPERVISOR_ID, ActivityInstanceSearchDescriptor.SUPERVISOR_ID);

        return builder;
    }

    @Override
    public CONSOLE_ITEM update(final APIID id, final Map<String, String> attributes) {
        String value = MapUtil.getValue(attributes, "variables", null);
        if (value != null && !value.isEmpty()) {
            updateActivityVariables(id, value);
        }

        update(get(id), attributes);
        try {
            return get(id);
        } catch (final APIException e) {
            if (e.getCause() instanceof ActivityInstanceNotFoundException) {
                return null;
            }
            throw e;
        }
    }

    private void updateActivityVariables(final APIID id, String value) {
        ActivityEngineClient client = new EngineClientFactory(new EngineAPIAccessor()).createActivityEngineClient(getEngineSession());
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        List<Variable> list = new JacksonDeserializer().deserializeList(value, Variable.class);
        for (Variable variable : list) {
            String name = variable.getName();
            if (StringUtil.isBlank(name)) {
                throw new APIException("Message to be done");
            }
            DataInstance activityDataInstance = client.getDataInstance(name, id.toLong());
            Serializable serializable = getSerializableValue(activityDataInstance.getClassName(), variable);
            map.put(name, serializable);
        }
        client.updateVariables(id.toLong(), map);
    }

    private Serializable getSerializableValue(String className, Variable variable) {
        try {
            Object deserialize = new JacksonDeserializer().convertValue(variable.getValue(), Class.forName(className));
            if (deserialize instanceof Date) {
                System.out.println(((Date) deserialize).getTime());
            }
            return (Serializable) deserialize;
        } catch (ClassNotFoundException e) {
            throw new APIException(_("%className% not found. Only jdk types are supported", new Arg("className", className)));
        } catch (ClassCastException e) {
            throw new APIException(_("%className% is not Serializable", new Arg("className", className)));
        }
    }

    protected void update(final CONSOLE_ITEM item, final Map<String, String> attributes) {
        updateState(item, MapUtil.getValue(attributes, FlowNodeItem.ATTRIBUTE_STATE, null));
    }

    /**
     * @param item
     *            The item to update
     * @param state
     *            The state to set
     */
    protected void updateState(final CONSOLE_ITEM item, final String state) {
        try {
            if (state == null) {
                return;
            }

            if (HumanTaskItem.VALUE_STATE_SKIPPED.equals(state) && item instanceof FlowNodeItem) {
                getProcessAPI().setActivityStateByName(item.getId().toLong(), ActivityStates.SKIPPED_STATE);
            } else if (HumanTaskItem.VALUE_STATE_COMPLETED.equals(state) && item instanceof ActivityItem) {
                getProcessAPI().executeFlowNode(item.getId().toLong());
            } else {
                throw new APIException("Can't update " + item.getClass().getName() + " state to \"" + item.getState() + "\"");
            }
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

}
