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

package org.apache.shardingsphere.traffic.algorithm.loadbalance;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.traffic.spi.TrafficLoadBalanceAlgorithm;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin traffic load balance algorithm.
 */
@Getter
@Setter
public final class RoundRobinTrafficLoadBalanceAlgorithm implements TrafficLoadBalanceAlgorithm {
    
    private static final ConcurrentHashMap<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    
    private Properties props = new Properties();
    
    @Override
    public String getInstanceId(final String name, final List<String> instanceIds) {
        AtomicInteger count = COUNTS.containsKey(name) ? COUNTS.get(name) : new AtomicInteger(0);
        COUNTS.putIfAbsent(name, count);
        count.compareAndSet(instanceIds.size(), 0);
        return instanceIds.get(Math.abs(count.getAndIncrement()) % instanceIds.size());
    }
    
    @Override
    public String getType() {
        return "ROUND_ROBIN";
    }
    
    @Override
    public boolean isDefault() {
        return true;
    }
}
