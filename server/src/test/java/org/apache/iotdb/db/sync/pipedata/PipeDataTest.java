/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.sync.pipedata;

import org.apache.iotdb.db.engine.modification.Deletion;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.qp.physical.PhysicalPlan;
import org.apache.iotdb.db.qp.physical.sys.SetStorageGroupPlan;
import org.apache.iotdb.db.utils.EnvironmentUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PipeDataTest {
  private static final Logger logger = LoggerFactory.getLogger(PipeDataTest.class);
  private static final String pipeLogPath = "target/pipelog";

  @Before
  public void setUp() throws Exception {
    EnvironmentUtils.envSetUp();
  }

  @After
  public void tearDown() throws IOException, StorageEngineException {
    EnvironmentUtils.cleanEnv();
    Files.deleteIfExists(Paths.get(pipeLogPath));
  }

  @Test
  public void testSerializeAndDeserialize() {
    try {
      File f1 = new File(pipeLogPath);
      File f2 = new File(pipeLogPath);
      PipeData pipeData1 = new TsFilePipeData("1", 1);
      Deletion deletion = new Deletion(new PartialPath("root.sg1.d1.s1"), 0, 1, 5);
      PipeData pipeData2 = new DeletionPipeData(deletion, 3);
      PhysicalPlan plan = new SetStorageGroupPlan(new PartialPath("root.sg1"));
      PipeData pipeData3 = new SchemaPipeData(plan, 2);
      DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(f2));
      pipeData1.serialize(outputStream);
      outputStream.flush();
      DataInputStream inputStream = new DataInputStream(new FileInputStream(f1));
      Assert.assertEquals(pipeData1, PipeData.deserialize(inputStream));
      pipeData2.serialize(outputStream);
      outputStream.flush();
      Assert.assertEquals(pipeData2, PipeData.deserialize(inputStream));
      pipeData3.serialize(outputStream);
      outputStream.flush();
      Assert.assertEquals(pipeData3, PipeData.deserialize(inputStream));
      inputStream.close();
      outputStream.close();

      Assert.assertEquals(pipeData1, PipeData.deserialize(pipeData1.serialize()));
      Assert.assertEquals(pipeData2, PipeData.deserialize(pipeData2.serialize()));
      Assert.assertEquals(pipeData3, PipeData.deserialize(pipeData3.serialize()));
    } catch (Exception e) {
      logger.error(e.getMessage());
      Assert.fail();
    }
  }
}
