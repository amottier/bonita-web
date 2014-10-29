/**
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.bonitasoft.web.rest.security

import org.bonitasoft.engine.api.APIAccessor
import org.bonitasoft.engine.api.IdentityAPI
import org.bonitasoft.engine.api.Logger
import org.bonitasoft.engine.api.ProcessAPI
import org.bonitasoft.engine.api.permission.APICallContext
import org.bonitasoft.engine.api.permission.PermissionRule
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.session.APISession
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.mock

@RunWith(MockitoJUnitRunner.class)
public class ProcessPermissionRuleTest {

    @Mock
    def APISession apiSession
    @Mock
    def APICallContext apiCallContext
    @Mock
    def APIAccessor apiAccessor
    @Mock
    def Logger logger
    def PermissionRule rule = new ProcessPermissionRule()
    @Mock
    def ProcessAPI processAPI
    @Mock
    def IdentityAPI identityAPI
    @Mock
    def User user
    def long currentUserId = 16l

    @Before
    public void before() {

        doReturn(processAPI).when(apiAccessor).getProcessAPI()
        doReturn(identityAPI).when(apiAccessor).getIdentityAPI()
        doReturn(user).when(identityAPI).getUser(currentUserId)
        doReturn(currentUserId).when(apiSession).getUserId()
    }

    @Test
    public void should_check_verify_filters_on_GET_with_different_user() {
        //given
        havingFilters([user_id: "15"])
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void should_check_verify_filters_on_GET_with_diff_team_manager_id() {
        //given
        havingFilters([team_manager_id: "15"])
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isFalse();
    }

    @Test
    public void should_check_verify_filters_on_GET_with_diff_supervisor_id() {
        //given
        havingFilters([supervisor_id: "15"])
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isFalse();
    }
    @Test
    public void should_check_verify_filters_on_GET_with_same_user() {
        //given
        havingFilters([user_id: "16"])
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void should_check_verify_filters_on_GET_with_same_team_manager_id() {
        //given
        havingFilters([team_manager_id: "16"])
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void should_check_verify_filters_on_GET_with_same_supervisor_id() {
        //given
        havingFilters([supervisor_id: "16"])
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isTrue();
    }

    def havingFilters(Map filters) {
        doReturn("GET").when(apiCallContext).getMethod()
        doReturn(filters).when(apiCallContext).getFilters()
    }


    @Test
    public void should_check_verify_resourceId_isInvolved_on_GET() {
        //given
        havingResourceId(currentUserId)
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void should_check_verify_resourceId_not_isInvolved_on_GET() {
        //given
        havingResourceId(15)
        //when
        def isAuthorized = rule.check(apiSession, apiCallContext, apiAccessor, logger)
        //then
        assertThat(isAuthorized).isFalse();
    }

    def havingResourceId(long deployedBy) {
        doReturn(currentUserId).when(apiSession).getUserId()
        doReturn("GET").when(apiCallContext).getMethod()
        doReturn("process").when(apiCallContext).getResourceName()
        doReturn("45").when(apiCallContext).getResourceId()

        def info = mock(ProcessDeploymentInfo.class)
        doReturn(deployedBy).when(info).getDeployedBy()
        doReturn(info).when(processAPI).getProcessDeploymentInfo(45l);
    }

}
