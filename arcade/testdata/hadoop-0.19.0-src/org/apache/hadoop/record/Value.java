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
 * XML Deserializer.
 */
package org.apache.hadoop.record;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

static private class Value {
    private String type;
    private StringBuffer sb;
        
    public Value(String t) {
      type = t;
      sb = new StringBuffer();
    }
    public void addChars(char[] buf, int offset, int len) {
      sb.append(buf, offset, len);
    }
    public String getValue() { return sb.toString(); }
    public String getType() { return type; }
  }