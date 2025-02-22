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

package org.apache.shardingsphere.infra.config.datasource.pool.creator.impl;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.infra.config.datasource.DataSourceConfiguration;
import org.apache.shardingsphere.infra.config.datasource.pool.creator.DataSourcePoolCreator;
import org.apache.shardingsphere.test.mock.MockedDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public final class HikariDataSourcePoolCreatorTest {
    
    @Test
    public void assertCreateDataSourceConfigurationWithoutDriverClassName() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1/foo_ds");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        DataSourceConfiguration dataSourceConfiguration = new DataSourcePoolCreator(HikariDataSource.class.getCanonicalName()).createDataSourceConfiguration(dataSource);
        Map<String, Object> props = dataSourceConfiguration.getProps();
        assertFalse(props.containsKey("driverClassName") && null == props.get("driverClassName"));
    }
    
    @Test
    public void assertCreateDataSourceConfiguration() {
        DataSourcePoolCreator dataSourcePoolCreator = new DataSourcePoolCreator(HikariDataSource.class.getCanonicalName());
        DataSourceConfiguration configuration = dataSourcePoolCreator.createDataSourceConfiguration(dataSourcePoolCreator.createDataSource(createDataSourceConfiguration()));
        assertThat(configuration.getDataSourceClassName(), is("com.zaxxer.hikari.HikariDataSource"));
        assertThat(configuration.getProps().get("jdbcUrl"), is("jdbc:mysql://127.0.0.1/foo_ds"));
        assertThat(configuration.getProps().get("driverClassName"), is(MockedDataSource.class.getCanonicalName()));
        assertThat(configuration.getProps().get("username"), is("root"));
        assertThat(configuration.getProps().get("password"), is("root"));
        assertThat(configuration.getProps().get("maximumPoolSize"), is(10));
        assertThat(configuration.getProps().get("minimumIdle"), is(1));
        assertDataSourceProperties((Properties) configuration.getProps().get("dataSourceProperties"));
    }
    
    @Test
    public void assertCreateDataSource() {
        DataSourcePoolCreator dataSourcePoolCreator = new DataSourcePoolCreator(HikariDataSource.class.getCanonicalName());
        DataSource dataSource = dataSourcePoolCreator.createDataSource(createDataSourceConfiguration());
        assertThat(dataSource, instanceOf(HikariDataSource.class));
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertThat(hikariDataSource.getJdbcUrl(), is("jdbc:mysql://127.0.0.1/foo_ds"));
        assertThat(hikariDataSource.getDriverClassName(), is(MockedDataSource.class.getCanonicalName()));
        assertThat(hikariDataSource.getUsername(), is("root"));
        assertThat(hikariDataSource.getPassword(), is("root"));
        assertThat(hikariDataSource.getMaximumPoolSize(), is(10));
        assertThat(hikariDataSource.getMinimumIdle(), is(1));
        assertDataSourceProperties(hikariDataSource.getDataSourceProperties());
    }
    
    private DataSourceConfiguration createDataSourceConfiguration() {
        Map<String, Object> props = new HashMap<>(16, 1);
        props.put("jdbcUrl", "jdbc:mysql://127.0.0.1/foo_ds");
        props.put("driverClassName", MockedDataSource.class.getCanonicalName());
        props.put("username", "root");
        props.put("password", "root");
        props.put("maxPoolSize", 10);
        props.put("minPoolSize", 1);
        props.put("dataSourceProperties", getDataSourceProperties());
        DataSourceConfiguration result = new DataSourceConfiguration("com.zaxxer.hikari.HikariDataSource");
        result.getProps().putAll(props);
        return result;
    }
    
    private Properties getDataSourceProperties() {
        Properties result = new Properties();
        result.put("prepStmtCacheSqlLimit", 1024);
        result.put("cachePrepStmts", true);
        result.put("prepStmtCacheSize", 1000);
        return result;
    }
    
    private void assertDataSourceProperties(final Properties props) {
        assertThat(props.get("prepStmtCacheSqlLimit"), is(1024));
        assertThat(props.get("cachePrepStmts"), is(true));
        assertThat(props.get("prepStmtCacheSize"), is(1000));
        assertThat(props.get("useServerPrepStmts"), is(Boolean.TRUE.toString()));
        assertThat(props.get("useLocalSessionState"), is(Boolean.TRUE.toString()));
        assertThat(props.get("rewriteBatchedStatements"), is(Boolean.TRUE.toString()));
        assertThat(props.get("cacheResultSetMetadata"), is(Boolean.FALSE.toString()));
        assertThat(props.get("cacheServerConfiguration"), is(Boolean.TRUE.toString()));
        assertThat(props.get("elideSetAutoCommits"), is(Boolean.TRUE.toString()));
        assertThat(props.get("maintainTimeStats"), is(Boolean.FALSE.toString()));
        assertThat(props.get("netTimeoutForStreamingResults"), is("0"));
        assertThat(props.get("tinyInt1isBit"), is(Boolean.FALSE.toString()));
        assertThat(props.get("useSSL"), is(Boolean.FALSE.toString()));
        assertThat(props.get("serverTimezone"), is("UTC"));
    }
}
