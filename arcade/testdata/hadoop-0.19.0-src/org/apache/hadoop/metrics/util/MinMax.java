/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * The MetricsTimeVaryingRate class is for a rate based metric that
 * naturally varies over time (e.g. time taken to create a file).
 * The rate is averaged at each interval heart beat (the interval
 * is set in the metrics config file).
 * This class also keeps track of the min and max rates along with 
 * a method to reset the min-max.
 *
 */
package org.apache.hadoop.metrics.util;
import org.apache.hadoop.metrics.MetricsRecord;
import org.apache.hadoop.util.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

static class MinMax {
    long minTime = -1;
    long maxTime = 0;
    
    void set(final MinMax newVal) {
      minTime = newVal.minTime;
      maxTime = newVal.maxTime;
    }
    
    void reset() {
      minTime = -1;
      maxTime = 0;
    }
    void update(final long time) { // update min max
      minTime = (minTime == -1) ? time : Math.min(minTime, time);
      minTime = Math.min(minTime, time);
      maxTime = Math.max(maxTime, time);
    }
  }