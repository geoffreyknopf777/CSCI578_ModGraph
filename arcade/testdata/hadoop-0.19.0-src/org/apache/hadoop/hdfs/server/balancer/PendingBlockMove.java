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
/** <p>The balancer is a tool that balances disk space usage on an HDFS cluster
 * when some datanodes become full or when new empty nodes join the cluster.
 * The tool is deployed as an application program that can be run by the 
 * cluster administrator on a live HDFS cluster while applications
 * adding and deleting files.
 * 
 * <p>SYNOPSIS
 * <pre>
 * To start:
 *      bin/start-balancer.sh [-threshold <threshold>]
 *      Example: bin/ start-balancer.sh 
 *                     start the balancer with a default threshold of 10%
 *               bin/ start-balancer.sh -threshold 5
 *                     start the balancer with a threshold of 5%
 * To stop:
 *      bin/ stop-balancer.sh
 * </pre>
 * 
 * <p>DESCRIPTION
 * <p>The threshold parameter is a fraction in the range of (0%, 100%) with a 
 * default value of 10%. The threshold sets a target for whether the cluster 
 * is balanced. A cluster is balanced if for each datanode, the utilization 
 * of the node (ratio of used space at the node to total capacity of the node) 
 * differs from the utilization of the (ratio of used space in the cluster 
 * to total capacity of the cluster) by no more than the threshold value. 
 * The smaller the threshold, the more balanced a cluster will become. 
 * It takes more time to run the balancer for small threshold values. 
 * Also for a very small threshold the cluster may not be able to reach the 
 * balanced state when applications write and delete files concurrently.
 * 
 * <p>The tool moves blocks from highly utilized datanodes to poorly 
 * utilized datanodes iteratively. In each iteration a datanode moves or 
 * receives no more than the lesser of 10G bytes or the threshold fraction 
 * of its capacity. Each iteration runs no more than 20 minutes.
 * At the end of each iteration, the balancer obtains updated datanodes
 * information from the namenode.
 * 
 * <p>A system property that limits the balancer's use of bandwidth is 
 * defined in the default configuration file:
 * <pre>
 * <property>
 *   <name>dfs.balance.bandwidthPerSec</name>
 *   <value>1048576</value>
 * <description>  Specifies the maximum bandwidth that each datanode 
 * can utilize for the balancing purpose in term of the number of bytes 
 * per second. </description>
 * </property>
 * </pre>
 * 
 * <p>This property determines the maximum speed at which a block will be 
 * moved from one datanode to another. The default value is 1MB/s. The higher 
 * the bandwidth, the faster a cluster can reach the balanced state, 
 * but with greater competition with application processes. If an 
 * administrator changes the value of this property in the configuration 
 * file, the change is observed when HDFS is next restarted.
 * 
 * <p>MONITERING BALANCER PROGRESS
 * <p>After the balancer is started, an output file name where the balancer 
 * progress will be recorded is printed on the screen.  The administrator 
 * can monitor the running of the balancer by reading the output file. 
 * The output shows the balancer's status iteration by iteration. In each 
 * iteration it prints the starting time, the iteration number, the total 
 * number of bytes that have been moved in the previous iterations, 
 * the total number of bytes that are left to move in order for the cluster 
 * to be balanced, and the number of bytes that are being moved in this 
 * iteration. Normally "Bytes Already Moved" is increasing while "Bytes Left 
 * To Move" is decreasing.
 * 
 * <p>Running multiple instances of the balancer in an HDFS cluster is 
 * prohibited by the tool.
 * 
 * <p>The balancer automatically exits when any of the following five 
 * conditions is satisfied:
 * <ol>
 * <li>The cluster is balanced;
 * <li>No block can be moved;
 * <li>No block has been moved for five consecutive iterations;
 * <li>An IOException occurs while communicating with the namenode;
 * <li>Another balancer is running.
 * </ol>
 * 
 * <p>Upon exit, a balancer returns an exit code and prints one of the 
 * following messages to the output file in corresponding to the above exit 
 * reasons:
 * <ol>
 * <li>The cluster is balanced. Exiting
 * <li>No block can be moved. Exiting...
 * <li>No block has been moved for 3 iterations. Exiting...
 * <li>Received an IO exception: failure reason. Exiting...
 * <li>Another balancer is running. Exiting...
 * </ol>
 * 
 * <p>The administrator can interrupt the execution of the balancer at any 
 * time by running the command "stop-balancer.sh" on the machine where the 
 * balancer is running.
 */
package org.apache.hadoop.hdfs.server.balancer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.common.HdfsConstants;
import org.apache.hadoop.hdfs.server.common.Util;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.protocol.*;
import org.apache.hadoop.hdfs.protocol.FSConstants.DatanodeReportType;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.BlocksWithLocations.BlockWithLocations;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.retry.RetryPolicies;
import org.apache.hadoop.io.retry.RetryPolicy;
import org.apache.hadoop.io.retry.RetryProxy;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.security.UnixUserGroupInformation;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

private class PendingBlockMove {
    private BalancerBlock block;
    private Source source;
    private BalancerDatanode proxySource;
    private BalancerDatanode target;
    
    /** constructor */
    private PendingBlockMove() {
    }
    
    /* choose a block & a proxy source for this pendingMove 
     * whose source & target have already been chosen.
     * 
     * Return true if a block and its proxy are chosen; false otherwise
     */
    private boolean chooseBlockAndProxy() {
      // iterate all source's blocks until find a good one    
      for (Iterator<BalancerBlock> blocks=
        source.getBlockIterator(); blocks.hasNext();) {
        if (markMovedIfGoodBlock(blocks.next())) {
          blocks.remove();
          return true;
        }
      }
      return false;
    }
    
    /* Return true if the given block is good for the tentative move;
     * If it is good, add it to the moved list to marked as "Moved".
     * A block is good if
     * 1. it is a good candidate; see isGoodBlockCandidate
     * 2. can find a proxy source that's not busy for this move
     */
    private boolean markMovedIfGoodBlock(BalancerBlock block) {
      synchronized(block) {
        synchronized(movedBlocks) {
          if (isGoodBlockCandidate(source, target, block)) {
            this.block = block;
            if ( chooseProxySource() ) {
              addToMoved(block);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Decided to move block "+ block.getBlockId()
                    +" with a length of "+FsShell.byteDesc(block.getNumBytes())
                    + " bytes from " + source.getName() 
                    + " to " + target.getName()
                    + " using proxy source " + proxySource.getName() );
              }
              return true;
            }
          }
        }
      }
      return false;
    }
    
    /* Now we find out source, target, and block, we need to find a proxy
     * 
     * @return true if a proxy is found; otherwise false
     */
    private boolean chooseProxySource() {
      // check if there is replica which is on the same rack with the target
      for (BalancerDatanode loc : block.getLocations()) {
        if (cluster.isOnSameRack(loc.getDatanode(), target.getDatanode())) {
          if (loc.addPendingBlock(this)) {
            proxySource = loc;
            return true;
          }
        }
      }
      // find out a non-busy replica
      for (BalancerDatanode loc : block.getLocations()) {
        if (loc.addPendingBlock(this)) {
          proxySource = loc;
          return true;
        }
      }
      return false;
    }
    
    /* Dispatch the block move task to the proxy source & wait for the response
     */
    private void dispatch() {
      Socket sock = new Socket();
      DataOutputStream out = null;
      DataInputStream in = null;
      try {
        sock.connect(NetUtils.createSocketAddr(
            target.datanode.getName()), HdfsConstants.READ_TIMEOUT);
        sock.setKeepAlive(true);
        out = new DataOutputStream( new BufferedOutputStream(
            sock.getOutputStream(), FSConstants.BUFFER_SIZE));
        sendRequest(out);
        in = new DataInputStream( new BufferedInputStream(
            sock.getInputStream(), FSConstants.BUFFER_SIZE));
        receiveResponse(in);
        bytesMoved.inc(block.getNumBytes());
        LOG.info( "Moving block " + block.getBlock().getBlockId() +
              " from "+ source.getName() + " to " +
              target.getName() + " through " +
              proxySource.getName() +
              " is succeeded." );
      } catch (IOException e) {
        LOG.warn("Error moving block "+block.getBlockId()+
            " from " + source.getName() + " to " +
            target.getName() + " through " +
            proxySource.getName() +
            ": "+e.getMessage());
      } finally {
        IOUtils.closeStream(out);
        IOUtils.closeStream(in);
        IOUtils.closeSocket(sock);
        
        proxySource.removePendingBlock(this);
        synchronized(target) {
          target.removePendingBlock(this);
        }

        synchronized (this ) {
          reset();
        }
        synchronized (Balancer.this) {
          Balancer.this.notifyAll();
        }
      }
    }
    
    /* Send a block replace request to the output stream*/
    private void sendRequest(DataOutputStream out) throws IOException {
      out.writeShort(DataTransferProtocol.DATA_TRANSFER_VERSION);
      out.writeByte(DataTransferProtocol.OP_REPLACE_BLOCK);
      out.writeLong(block.getBlock().getBlockId());
      out.writeLong(block.getBlock().getGenerationStamp());
      Text.writeString(out, source.getStorageID());
      proxySource.write(out);
      out.flush();
    }
    
    /* Receive a block copy response from the input stream */ 
    private void receiveResponse(DataInputStream in) throws IOException {
      short status = in.readShort();
      if (status != DataTransferProtocol.OP_STATUS_SUCCESS) {
        throw new IOException("block move is failed");
      }
    }

    /* reset the object */
    private void reset() {
      block = null;
      source = null;
      proxySource = null;
      target = null;
    }
    
    /* start a thread to dispatch the block move */
    private void scheduleBlockMove() {
      moverExecutor.execute(new Runnable() {
        public void run() {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Starting moving "+ block.getBlockId() +
                " from " + proxySource.getName() + " to " + target.getName());
          }
          dispatch();
        }
      });
    }
  }