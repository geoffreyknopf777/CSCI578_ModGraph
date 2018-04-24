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
 * A simple logger to handle the task-specific user logs.
 * This class uses the system property <code>hadoop.log.dir</code>.
 * 
 */
package org.apache.hadoop.mapred;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileUtil;
import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.logging.log4j.Logger;

private static class TaskLogsPurgeFilter implements FileFilter {
    long purgeTimeStamp;
  
    TaskLogsPurgeFilter(long purgeTimeStamp) {
      this.purgeTimeStamp = purgeTimeStamp;
    }

    public boolean accept(File file) {
      LOG.debug("PurgeFilter - file: " + file + ", mtime: " + file.lastModified() + ", purge: " + purgeTimeStamp);
      return file.lastModified() < purgeTimeStamp;
    }
  }