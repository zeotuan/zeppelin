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

package org.apache.zeppelin.interpreter.remote;

import org.apache.zeppelin.interpreter.Interpreter.FormType;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.ManagedInterpreterGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteInterpreterIdleActivityTest {

  private ManagedInterpreterGroup interpreterGroup;
  private RemoteInterpreterProcess interpreterProcess;
  private RemoteInterpreter interpreter;
  private InterpreterContext context;

  @BeforeEach
  void setUp() throws Exception {
    interpreterGroup = mock(ManagedInterpreterGroup.class);
    interpreterProcess = mock(RemoteInterpreterProcess.class);
    when(interpreterGroup.getOrCreateInterpreterProcess(anyString(), any(Properties.class)))
        .thenReturn(interpreterProcess);

    interpreter = new RemoteInterpreter(new Properties(), "session_1", "TestInterpreter",
        "user1", null);
    interpreter.setInterpreterGroup(interpreterGroup);
    interpreter.setOpened(true);
    context = InterpreterContext.builder().setNoteId("note_1").setParagraphId("p_1").build();
  }

  @Test
  void successfulInterpretMarksGroupUsed() throws Exception {
    when(interpreterProcess.isRunning()).thenReturn(true);
    when(interpreterProcess.callRemoteFunction(any()))
        .thenReturn(FormType.SIMPLE)
        .thenReturn(new InterpreterResult(InterpreterResult.Code.SUCCESS));

    interpreter.interpret("1 + 1", context);

    verify(interpreterGroup, times(2)).onInterpreterUse();
  }

  @Test
  void interpretAgainstStoppedProcessDoesNotMarkGroupUsed() throws Exception {
    when(interpreterProcess.isRunning()).thenReturn(false);
    when(interpreterProcess.callRemoteFunction(any())).thenReturn(FormType.SIMPLE);

    InterpreterResult result = interpreter.interpret("1 + 1", context);

    assertEquals(InterpreterResult.Code.ERROR, result.code());
    verify(interpreterGroup, never()).onInterpreterUse();
  }

  @Test
  void successfulProgressPollMarksGroupUsed() throws Exception {
    when(interpreterProcess.callRemoteFunction(any())).thenReturn(50);

    assertEquals(50, interpreter.getProgress(context));

    verify(interpreterGroup).onInterpreterUse();
  }

  @Test
  void failedProgressPollsDoNotMarkGroupUsed() {
    when(interpreterProcess.callRemoteFunction(any()))
        .thenThrow(new RuntimeException("connection refused"));

    for (int i = 0; i < 3; i++) {
      assertThrows(RuntimeException.class, () -> interpreter.getProgress(context));
    }

    verify(interpreterGroup, never()).onInterpreterUse();
  }

  @Test
  void successfulStatusPollMarksGroupUsed() {
    when(interpreterProcess.callRemoteFunction(any())).thenReturn("RUNNING");

    assertEquals("RUNNING", interpreter.getStatus("job_1"));

    verify(interpreterGroup).onInterpreterUse();
  }

  @Test
  void failedStatusPollsDoNotMarkGroupUsed() {
    when(interpreterProcess.callRemoteFunction(any()))
        .thenThrow(new RuntimeException("connection refused"));

    for (int i = 0; i < 3; i++) {
      assertThrows(RuntimeException.class, () -> interpreter.getStatus("job_1"));
    }

    verify(interpreterGroup, never()).onInterpreterUse();
  }

  @Test
  void statusPollWithoutAnyClientDoesNotMarkGroupUsed() {
    when(interpreterProcess.callRemoteFunction(any())).thenReturn(null);

    interpreter.getStatus("job_1");

    verify(interpreterGroup, never()).onInterpreterUse();
  }
}
