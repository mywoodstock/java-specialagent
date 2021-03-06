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

package io.opentracing.contrib.specialagent.grizzly.http.server;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;

import io.opentracing.contrib.grizzly.http.server.TracedFilterChainBuilder;
import io.opentracing.contrib.specialagent.AgentRuleUtil;
import io.opentracing.util.GlobalTracer;
import net.bytebuddy.asm.Advice;

public class FilterChainAgentIntercept {
  public static Object enter(final @Advice.This Object thiz) {
    if (AgentRuleUtil.callerEquals(1, 3, "io.opentracing.contrib.grizzly.http.server.TracedFilterChainBuilder.build"))
      return null;

    return new TracedFilterChainBuilder((FilterChainBuilder)thiz, GlobalTracer.get()).build();
  }
}