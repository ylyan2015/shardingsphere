/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sharding.algorithm.sharding.cosid;

import java.util.Arrays;

/**
 * Parametric test tool.
 */
public final class Arguments {

    @SafeVarargs
    static <T> T[] of(final T... arguments) {
        return arguments;
    }

    @SafeVarargs
    static <T> Iterable<T[]> ofArrayElement(final T[]... arguments) {
        return Arrays.asList(arguments);
    }
}
