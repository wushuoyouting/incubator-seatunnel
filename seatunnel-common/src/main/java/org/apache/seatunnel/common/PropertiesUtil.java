/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.common;

import org.apache.seatunnel.config.Config;

import java.util.Properties;

public class PropertiesUtil {

    public static void setProperties(Config config, Properties properties, String prefix, boolean keepPrefix) {
        config.entrySet().forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();
            if (key.startsWith(prefix)) {
                if (keepPrefix) {
                    properties.put(key, value);
                } else {
                    properties.put(key.substring(prefix.length()), value);
                }
            }
        });
    }
}
