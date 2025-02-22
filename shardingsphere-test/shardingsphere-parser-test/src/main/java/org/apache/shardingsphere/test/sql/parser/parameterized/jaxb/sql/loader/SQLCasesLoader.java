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

package org.apache.shardingsphere.test.sql.parser.parameterized.jaxb.sql.loader;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.shardingsphere.test.sql.parser.parameterized.jaxb.Case;
import org.apache.shardingsphere.test.sql.parser.parameterized.jaxb.sql.SQLCase;
import org.apache.shardingsphere.test.sql.parser.parameterized.jaxb.sql.SQLCaseType;
import org.apache.shardingsphere.test.sql.parser.parameterized.jaxb.sql.SQLCases;
import org.apache.shardingsphere.test.sql.parser.parameterized.loader.CasesLoader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL test cases loader.
 */
public final class SQLCasesLoader extends CasesLoader {

    public SQLCasesLoader(final String rootDirection) {
        super(rootDirection);
    }

    @Override
    public void buildCaseMap(final Map<String, Case> sqlCaseMap, final InputStream inputStream) throws JAXBException {
        SQLCases sqlCases = (SQLCases) JAXBContext.newInstance(SQLCases.class).createUnmarshaller().unmarshal(inputStream);
        for (SQLCase each : sqlCases.getSqlCases()) {
            if (null == each.getDatabaseTypes()) {
                each.setDatabaseTypes(sqlCases.getDatabaseTypes());
            }
            Preconditions.checkState(!sqlCaseMap.containsKey(each.getId()), "Find duplicated SQL Case ID: %s", each.getId());
            sqlCaseMap.put(each.getId(), each);
        }
    }
    
    /**
     * Get SQL case.
     *
     * @param sqlCaseId SQL case ID
     * @param sqlCaseType SQL case type
     * @param parameters SQL parameters
     * @param databaseType databaseType
     * @return SQL
     */
    @Override
    public String getCaseValue(final String sqlCaseId, final SQLCaseType sqlCaseType, final List<?> parameters, final String databaseType) {
        switch (sqlCaseType) {
            case Literal:
                return getLiteralSQL(getSQLFromMap(sqlCaseId, super.getCases()), parameters, databaseType);
            case Placeholder:
                return getPlaceholderSQL(getSQLFromMap(sqlCaseId, super.getCases()));
            default:
                throw new UnsupportedOperationException(sqlCaseType.name());
        }
    }
    
    /**
     * Get test parameters for junit parameterized test cases.
     *
     * @param databaseTypes database types
     * @return test parameters for junit parameterized test cases
     */
    @Override
    public Collection<Object[]> getTestParameters(final Collection<String> databaseTypes) {
        Collection<Object[]> result = new LinkedList<>();
        for (Case each : super.getCases().values()) {
            result.addAll(getSQLTestParameters(databaseTypes, (SQLCase) each));
        }
        return result;
    }
    
    private String getSQLFromMap(final String id, final Map<String, Case> sqlCaseMap) {
        Preconditions.checkState(sqlCaseMap.containsKey(id), "Can't find SQL of ID: %s", id);
        SQLCase statement = (SQLCase) sqlCaseMap.get(id);
        return statement.getValue();
    }
    
    private String getPlaceholderSQL(final String sql) {
        return sql;
    }

    private String getLiteralSQL(final String sql, final List<?> parameters, final String databaseType) {
        if (null == parameters || parameters.isEmpty()) {
            return sql;
        }
        return "PostgreSQL".equals(databaseType) || "openGauss".equals(databaseType) ? replace(sql, "\\?|\\$[0-9]+", parameters.toArray()) : replace(sql, "\\?", parameters.toArray());
    }
    
    private Collection<Object[]> getSQLTestParameters(final Collection<String> databaseTypes, final SQLCase sqlCase) {
        Collection<Object[]> result = new LinkedList<>();
        for (SQLCaseType each : SQLCaseType.values()) {
            result.addAll(getSQLTestParameters(databaseTypes, sqlCase, each));
        }
        return result;
    }
    
    private static Collection<Object[]> getSQLTestParameters(final Collection<String> databaseTypes, final SQLCase sqlCase, final SQLCaseType sqlCaseType) {
        Collection<Object[]> result = new LinkedList<>();
        for (String each : getDatabaseTypes(sqlCase.getDatabaseTypes())) {
            if (databaseTypes.contains(each)) {
                Object[] parameters = new Object[3];
                parameters[0] = sqlCase.getId();
                parameters[1] = each;
                parameters[2] = sqlCaseType;
                result.add(parameters);
            }
        }
        return result;
    }
    
    private static Collection<String> getDatabaseTypes(final String databaseTypes) {
        return Strings.isNullOrEmpty(databaseTypes) ? getAllDatabaseTypes() : Splitter.on(',').trimResults().splitToList(databaseTypes);
    }
    
    private static Collection<String> getAllDatabaseTypes() {
        return Arrays.asList("H2", "MySQL", "PostgreSQL", "Oracle", "SQLServer", "SQL92", "openGauss");
    }

    /**
     * Replaces each substring of this string that matches the literal target sequence with
     * literal replacements one by one.
     *
     * @param source the source string need to be replaced
     * @param target the sequence of char values to be replaced
     * @param replacements array of replacement
     * @return the resulting string
     * @throws IllegalArgumentException when replacements is not enough to replace found target.
     */
    private static String replace(final String source, final CharSequence target, final Object... replacements) {
        if (null == source || null == replacements) {
            return source;
        }
        Matcher matcher = Pattern.compile(target.toString()).matcher(source);
        int found = 0;
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            found++;
            if (found > replacements.length) {
                throw new IllegalArgumentException(
                        String.format("Missing replacement for '%s' at [%s].", target, found));
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacements[found - 1].toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
