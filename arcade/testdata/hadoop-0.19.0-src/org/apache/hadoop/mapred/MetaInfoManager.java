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
 * Provides methods for writing to and reading from job history. 
 * Job History works in an append mode, JobHistory and its inner classes provide methods 
 * to log job events. 
 * 
 * JobHistory is split into multiple files, format of each file is plain text where each line 
 * is of the format [type (key=value)*], where type identifies the type of the record. 
 * Type maps to UID of one of the inner classes of this class. 
 * 
 * Job history is maintained in a master index which contains star/stop times of all jobs with
 * a few other job level properties. Apart from this each job's history is maintained in a seperate history 
 * file. name of job history files follows the format jobtrackerId_jobid
 *  
 * For parsing the job history it supports a listener based interface where each line is parsed
 * and passed to listener. The listener can create an object model of history or look for specific 
 * events and discard rest of the history.  
 * 
 * CHANGE LOG :
 * Version 0 : The history has the following format : 
 *             TAG KEY1="VALUE1" KEY2="VALUE2" and so on. 
               TAG can be Job, Task, MapAttempt or ReduceAttempt. 
               Note that a '"' is the line delimiter.
 * Version 1 : Changes the line delimiter to '.'
               Values are now escaped for unambiguous parsing. 
               Added the Meta tag to store version info.
 */
package org.apache.hadoop.mapred;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.StringUtils;

static class MetaInfoManager implements Listener {
    private long version = 0L;
    private KeyValuePair pairs = new KeyValuePair();
    
    // Extract the version of the history that was used to write the history
    public MetaInfoManager(String line) throws IOException {
      if (null != line) {
        // Parse the line
        parseLine(line, this, false);
      }
    }
    
    // Get the line delimiter
    char getLineDelim() {
      if (version == 0) {
        return '"';
      } else {
        return LINE_DELIMITER_CHAR;
      }
    }
    
    // Checks if the values are escaped or not
    boolean isValueEscaped() {
      // Note that the values are not escaped in version 0
      return version != 0;
    }
    
    public void handle(RecordTypes recType, Map<Keys, String> values) 
    throws IOException {
      // Check if the record is of type META
      if (RecordTypes.Meta == recType) {
        pairs.handle(values);
        version = pairs.getLong(Keys.VERSION); // defaults to 0
      }
    }
    
    /**
     * Logs history meta-info to the history file. This needs to be called once
     * per history file. 
     * @param jobId job id, assigned by jobtracker. 
     */
    static void logMetaInfo(ArrayList<PrintWriter> writers){
      if (!disableHistory){
        if (null != writers){
          JobHistory.log(writers, RecordTypes.Meta, 
              new Keys[] {Keys.VERSION},
              new String[] {String.valueOf(VERSION)}); 
        }
      }
    }
  }