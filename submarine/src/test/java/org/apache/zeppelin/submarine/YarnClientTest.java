/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.submarine;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.submarine.commons.SubmarineConstants;
import org.apache.zeppelin.submarine.hadoop.YarnClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.zeppelin.submarine.commons.SubmarineConstants.ZEPPELIN_SUBMARINE_AUTH_TYPE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class YarnClientTest {

  private static YarnClient yarnClient = null;

  @BeforeAll
  public static void initEnv() {
    Properties properties = new Properties();
    properties.setProperty(ZEPPELIN_SUBMARINE_AUTH_TYPE, "simple");
    properties.setProperty("zeppelin.python.useIPython", "false");
    properties.setProperty("zeppelin.python.gatewayserver_address", "127.0.0.1");
    properties.setProperty(SubmarineConstants.SUBMARINE_HADOOP_KEYTAB, "keytab");
    properties.setProperty(SubmarineConstants.SUBMARINE_HADOOP_PRINCIPAL, "user");
    yarnClient = new YarnClient(properties, ZeppelinConfiguration.load());
  }

  @Test
  void testParseAppAttempts() throws IOException {
    String jsonFile = "ws-v1-cluster-apps-application_id-appattempts.json";
    URL urlJson = Resources.getResource(jsonFile);
    String jsonContent = Resources.toString(urlJson, Charsets.UTF_8);

    List<Map<String, Object>> list = yarnClient.parseAppAttempts(jsonContent);

    assertNotNull(list);
  }

  @Test
  void testParseAppAttemptsContainers() throws IOException {
    String jsonFile = "ws-v1-cluster-apps-application_id-appattempts-appattempt_id-containers.json";
    URL urlJson = Resources.getResource(jsonFile);
    String jsonContent = Resources.toString(urlJson, Charsets.UTF_8);

    List<Map<String, Object>> list = yarnClient.parseAppAttemptsContainers(jsonContent);

    assertNotNull(list.get(0).get(YarnClient.HOST_IP));
    assertNotNull(list.get(0).get(YarnClient.HOST_PORT));
    assertNotNull(list.get(0).get(YarnClient.CONTAINER_PORT));

    assertNotNull(list);
  }

  @Test
  void testParseClusterApps() throws IOException {
    String jsonFile = "ws-v1-cluster-apps-application_id-finished.json";
    URL urlJson = Resources.getResource(jsonFile);
    String jsonContent = Resources.toString(urlJson, Charsets.UTF_8);

    Map<String, Object> list = yarnClient.parseClusterApps(jsonContent);

    assertNotNull(list);
  }
}
