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
 * This is an implementation of the Hadoop Archive 
 * Filesystem. This archive Filesystem has index files
 * of the form _index* and has contents of the form
 * part-*. The index files store the indexes of the 
 * real files. The index files are of the form _masterindex
 * and _index. The master index is a level of indirection 
 * in to the index file to make the look ups faster. the index
 * file is sorted with hash code of the paths that it contains 
 * and the master index contains pointers to the positions in 
 * index for ranges of hashcodes.
 */
package org.apache.hadoop.fs;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;
import org.apache.hadoop.util.Progressable;

static class Store {
    public Store() {
      begin = end = startHash = endHash = 0;
    }
    public Store(long begin, long end, int startHash, int endHash) {
      this.begin = begin;
      this.end = end;
      this.startHash = startHash;
      this.endHash = endHash;
    }
    public long begin;
    public long end;
    public int startHash;
    public int endHash;
  }