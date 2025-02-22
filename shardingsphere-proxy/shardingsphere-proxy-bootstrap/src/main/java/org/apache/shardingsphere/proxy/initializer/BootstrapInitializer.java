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

package org.apache.shardingsphere.proxy.initializer;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.scenario.rulealtered.RuleAlteredContext;
import org.apache.shardingsphere.data.pipeline.scenario.rulealtered.RuleAlteredJobWorker;
import org.apache.shardingsphere.db.protocol.CommonConstants;
import org.apache.shardingsphere.db.protocol.mysql.constant.MySQLServerInfo;
import org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLServerInfo;
import org.apache.shardingsphere.infra.autogen.version.ShardingSphereVersion;
import org.apache.shardingsphere.proxy.config.resource.ProxyResourceConfiguration;
import org.apache.shardingsphere.infra.config.datasource.pool.creator.DataSourcePoolCreatorUtil;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.instance.InstanceDefinition;
import org.apache.shardingsphere.infra.instance.InstanceType;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.yaml.config.swapper.mode.ModeConfigurationYamlSwapper;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.manager.ContextManagerBuilderFactory;
import org.apache.shardingsphere.mode.manager.ContextManagerBuilderParameter;
import org.apache.shardingsphere.mode.metadata.MetaDataContexts;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.config.ProxyConfiguration;
import org.apache.shardingsphere.proxy.config.YamlProxyConfiguration;
import org.apache.shardingsphere.proxy.config.resource.ProxyResourceConfigurationConverter;
import org.apache.shardingsphere.proxy.config.yaml.swapper.YamlProxyConfigurationSwapper;
import org.apache.shardingsphere.proxy.database.DatabaseServerInfo;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Bootstrap initializer.
 */
@RequiredArgsConstructor
@Slf4j
public final class BootstrapInitializer {
    
    /**
     * Initialize.
     *
     * @param yamlConfig YAML proxy configuration
     * @param port proxy port
     * @throws SQLException SQL exception
     */
    public void init(final YamlProxyConfiguration yamlConfig, final int port) throws SQLException {
        ModeConfiguration modeConfig = null == yamlConfig.getServerConfiguration().getMode()
                ? null : new ModeConfigurationYamlSwapper().swapToObject(yamlConfig.getServerConfiguration().getMode());
        ContextManager contextManager = createContextManager(yamlConfig, modeConfig, port);
        initContext(contextManager);
        initRuleAlteredJobWorker(modeConfig, contextManager);
        setDatabaseServerInfo();
    }
    
    private ContextManager createContextManager(final YamlProxyConfiguration yamlConfig, final ModeConfiguration modeConfig, final int port) throws SQLException {
        ProxyConfiguration proxyConfig = new YamlProxyConfigurationSwapper().swap(yamlConfig);
        boolean isOverwrite = null == modeConfig || modeConfig.isOverwrite();
        Map<String, Map<String, DataSource>> dataSourcesMap = getDataSourcesMap(proxyConfig.getSchemaResources());
        ContextManagerBuilderParameter parameter = ContextManagerBuilderParameter.builder().modeConfig(modeConfig).dataSourcesMap(dataSourcesMap).schemaRuleConfigs(proxyConfig.getSchemaRules())
                .globalRuleConfigs(proxyConfig.getGlobalRules()).props(proxyConfig.getProps()).isOverwrite(isOverwrite).labels(proxyConfig.getLabels())
                .instanceDefinition(new InstanceDefinition(InstanceType.PROXY, port)).build();
        return ContextManagerBuilderFactory.newInstance(modeConfig).build(parameter);
    }
    
    private void initContext(final ContextManager contextManager) {
        ProxyContext.getInstance().init(contextManager);
    }
    
    // TODO add ResourceConfiguration param to ContextManagerBuilder to avoid re-build data source
    private Map<String, Map<String, DataSource>> getDataSourcesMap(final Map<String, Map<String, ProxyResourceConfiguration>> resourceConfigMap) {
        Map<String, Map<String, DataSource>> result = new LinkedHashMap<>(resourceConfigMap.size(), 1);
        for (Entry<String, Map<String, ProxyResourceConfiguration>> entry : resourceConfigMap.entrySet()) {
            result.put(entry.getKey(), DataSourcePoolCreatorUtil.getDataSourceMap(ProxyResourceConfigurationConverter.getDataSourceConfigurationMap(entry.getValue())));
        }
        return result;
    }
    
    private void initRuleAlteredJobWorker(final ModeConfiguration modeConfig, final ContextManager contextManager) {
        if (null == modeConfig) {
            return;
        }
        // TODO decouple "Cluster" to pluggable
        if (!"Cluster".equals(modeConfig.getType())) {
            log.info("mode type is not Cluster, ignore initRuleAlteredJobWorker");
            return;
        }
        RuleAlteredContext.initModeConfig(modeConfig);
        RuleAlteredContext.initContextManager(contextManager);
        // TODO init worker only if necessary, e.g. 1) rule altered action configured, 2) enabled job exists, 3) stopped job restarted
        RuleAlteredJobWorker.initWorkerIfNecessary();
    }
    
    private void setDatabaseServerInfo() {
        CommonConstants.PROXY_VERSION.set(getShardingSphereVersion());
        findBackendDataSource().ifPresent(dataSourceSample -> {
            DatabaseServerInfo databaseServerInfo = new DatabaseServerInfo(dataSourceSample);
            log.info(databaseServerInfo.toString());
            switch (databaseServerInfo.getDatabaseName()) {
                case "MySQL":
                    MySQLServerInfo.setServerVersion(databaseServerInfo.getDatabaseVersion());
                    break;
                case "PostgreSQL":
                    PostgreSQLServerInfo.setServerVersion(databaseServerInfo.getDatabaseVersion());
                    break;
                default:
            }
        });
    }
    
    private String getShardingSphereVersion() {
        String result = ShardingSphereVersion.VERSION;
        if (!ShardingSphereVersion.IS_SNAPSHOT || Strings.isNullOrEmpty(ShardingSphereVersion.BUILD_GIT_COMMIT_ID_ABBREV)) {
            return result;
        }
        result += ShardingSphereVersion.BUILD_GIT_DIRTY ? "-dirty" : "";
        result += "-" + ShardingSphereVersion.BUILD_GIT_COMMIT_ID_ABBREV;
        return result;
    }
    
    private Optional<DataSource> findBackendDataSource() {
        MetaDataContexts metaDataContexts = ProxyContext.getInstance().getContextManager().getMetaDataContexts();
        Optional<ShardingSphereMetaData> metaData = metaDataContexts.getMetaDataMap().values().stream().filter(ShardingSphereMetaData::isComplete).findFirst();
        return metaData.flatMap(optional -> optional.getResource().getDataSources().values().stream().findFirst());
    }
}
