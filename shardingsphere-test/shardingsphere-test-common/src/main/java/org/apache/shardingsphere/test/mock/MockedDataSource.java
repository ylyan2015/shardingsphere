/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.test.mock;

import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mocked data source.
 */
@Getter
@Setter
public final class MockedDataSource implements DataSource {
    
    private String driverClassName;
    
    private String url;
    
    private String username;
    
    private String password;
    
    private Integer maxPoolSize;
    
    private Integer minPoolSize;
    
    private List<String> connectionInitSqls;
    
    @SuppressWarnings("MagicConstant")
    @Override
    public Connection getConnection() throws SQLException {
        Connection result = mock(Connection.class, RETURNS_DEEP_STUBS);
        when(result.getMetaData().getURL()).thenReturn("jdbc:mock://127.0.0.1/foo_ds");
        when(result.createStatement(anyInt(), anyInt(), anyInt()).getConnection()).thenReturn(result);
        return result;
    }
    
    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return getConnection();
    }
    
    @SuppressWarnings("ReturnOfNull")
    @Override
    public <T> T unwrap(final Class<T> iface) {
        return null;
    }
    
    @Override
    public boolean isWrapperFor(final Class<?> iface) {
        return false;
    }
    
    @SuppressWarnings("ReturnOfNull")
    @Override
    public PrintWriter getLogWriter() {
        return null;
    }
    
    @Override
    public void setLogWriter(final PrintWriter out) {
    }
    
    @Override
    public void setLoginTimeout(final int seconds) {
    }
    
    @Override
    public int getLoginTimeout() {
        return 0;
    }
    
    @SuppressWarnings("ReturnOfNull")
    @Override
    public Logger getParentLogger() {
        return null;
    }
}
