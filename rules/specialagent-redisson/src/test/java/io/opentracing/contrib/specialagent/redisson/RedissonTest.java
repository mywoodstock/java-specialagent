/* Copyright 2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentracing.contrib.specialagent.redisson;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import io.opentracing.contrib.specialagent.AgentRunner;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import redis.embedded.RedisServer;

@RunWith(AgentRunner.class)
public class RedissonTest {
  private RedisServer redisServer;

  @Before
  public void before(final MockTracer tracer) throws IOException {
    tracer.reset();

    redisServer = new RedisServer();
    redisServer.start();
  }

  @After
  public void after() {
    if (redisServer != null)
      redisServer.stop();
  }

  @Test
  public void test(final MockTracer tracer) {
    final Config config = new Config();
    config.useSingleServer().setAddress("redis://127.0.0.1:6379");

    final RedissonClient redissonClient = Redisson.create(config);
    final RMap<String,String> map = redissonClient.getMap("map");

    map.put("key", "value");
    assertEquals("value", map.get("key"));

    final List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());

    redissonClient.shutdown();
  }
}