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

package io.opentracing.contrib.specialagent;

import java.lang.reflect.Field;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.ProxyMockTracer;
import io.opentracing.util.GlobalTracer;

/**
 * Support utility methods for {@code AgentRunner}. This class is deliberately
 * placed in the main path, because it is supposed to be loaded in the bootstrap
 * class loader.
 *
 * @author Seva Safris
 */
public class AgentRunnerUtil {
  private static final Logger logger = Logger.getLogger(AgentRunnerUtil.class);
  private static Tracer tracer = null;
  private static final Object tracerMutex = new Object();

  /**
   * Returns the OpenTracing {@link Tracer} to be used for the duration of the
   * test process. The {@link Tracer} is initialized on first invocation to this
   * method in a synchronized, thread-safe manner. If the {@code "-javaagent"}
   * argument is not specified for the current process, this function will
   * return {@code null}.
   *
   * @return The OpenTracing {@link Tracer} to be used for the duration of the
   *         test process, or {@code null} if the {@code "-javaagent"} argument
   *         is not specified for the current process.
   */
  @SuppressWarnings("resource")
  public static Tracer getTracer() {
    if (tracer != null)
      return tracer;

    synchronized (tracerMutex) {
      if (tracer != null)
        return tracer;

      final Tracer deferredTracer = SpecialAgent.getDeferredTracer();
      final MockTracer tracer;
      if (GlobalTracer.isRegistered()) {
        try {
          final Field field = GlobalTracer.class.getDeclaredField("tracer");
          field.setAccessible(true);
          final Tracer registered = (Tracer)field.get(null);
          if (deferredTracer == null) {
            tracer = registered instanceof MockTracer ? (MockTracer)registered : new ProxyMockTracer(registered);
          }
          else if (registered instanceof MockTracer) {
            tracer = new ProxyMockTracer((MockTracer)registered, deferredTracer);
          }
          else {
            throw new IllegalStateException("There is already a registered global Tracer.");
          }

          field.set(null, tracer);
        }
        catch (final IllegalAccessException | NoSuchFieldException e) {
          throw new IllegalStateException(e);
        }
      }
      else {
        tracer = deferredTracer != null ? new ProxyMockTracer(deferredTracer) : new MockTracer();
        if (!GlobalTracer.registerIfAbsent(tracer))
          throw new IllegalStateException("There is already a registered global Tracer.");
      }

      if (logger.isLoggable(Level.FINER)) {
        logger.finer("Registering tracer for AgentRunner: " + tracer);
        logger.finer("  Tracer ClassLoader: " + tracer.getClass().getClassLoader());
        logger.finer("  Tracer Location: " + ClassLoader.getSystemClassLoader().getResource(tracer.getClass().getName().replace('.', '/').concat(".class")));
        logger.finer("  GlobalTracer ClassLoader: " + GlobalTracer.class.getClassLoader());
        logger.finer("  GlobalTracer Location: " + ClassLoader.getSystemClassLoader().getResource(GlobalTracer.class.getName().replace('.', '/').concat(".class")));
      }

      return AgentRunnerUtil.tracer = tracer;
    }
  }
}