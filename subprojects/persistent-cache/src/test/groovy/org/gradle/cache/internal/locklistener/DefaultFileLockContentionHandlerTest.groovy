/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.cache.internal.locklistener

import org.gradle.internal.MutableBoolean
import org.gradle.internal.concurrent.ExecutorFactory
import org.gradle.internal.concurrent.ManagedExecutor
import org.gradle.internal.remote.internal.inet.InetAddressFactory
import org.gradle.util.ConcurrentSpecification

import static org.gradle.test.fixtures.ConcurrentTestUtil.poll

class DefaultFileLockContentionHandlerTest extends ConcurrentSpecification {
    def addressFactory = new InetAddressFactory()
    def handler = new DefaultFileLockContentionHandler(executorFactory, addressFactory)
    def client = new DefaultFileLockContentionHandler(executorFactory, addressFactory)

    def cleanup() {
        handler?.stop()
        client?.stop()
    }

    def "manages contention for multiple locks"() {
        def action1 = new MutableBoolean()
        def action2 = new MutableBoolean()

        when:
        int port = handler.reservePort()
        handler.start(10, { action1.set(true) })
        handler.start(11, { action2.set(true) })

        client.maybePingOwner(port, 10, "lock 1", 50000)
        client.maybePingOwner(port, 11, "lock 2", 50000)

        then:
        poll {
            assert action1.get() && action2.get()
        }
    }

    def "there are only two executors: one lock request listener and one release lock action executor"() {
        def factory = Mock(ExecutorFactory)
        handler = new DefaultFileLockContentionHandler(factory, addressFactory)

        when:
        handler.reservePort()
        handler.start(10, {} as Runnable)
        handler.start(11, {} as Runnable)
        handler.start(12, {} as Runnable)

        then:
        2 * factory.create(_ as String) >> Mock(ManagedExecutor)
    }

    def "cannot start contention handling when the handler was stopped"() {
        handler.stop()

        when:
        handler.start(10, {} as Runnable)

        then:
        thrown(IllegalStateException)
    }

    def "cannot start contention handling when the handler was not initialized"() {
        when:
        handler.start(10, {} as Runnable)

        then:
        thrown(IllegalStateException)
    }

    def "handler can be closed and contended action does not run"() {
        when:
        int port = handler.reservePort();
        handler.start(10, { throw new RuntimeException("Boo!") } as Runnable)
        handler.stop()

        client.maybePingOwner(port, 10, "lock 1", 50000)

        then:
        noExceptionThrown()
    }

    def "can receive request for lock that is already closed"() {
        when:
        int port = handler.reservePort()
        handler.start(10, { assert false } as Runnable)
        sleep(300) //so that it starts receiving

        //close the lock
        handler.stop(10)

        //receive request for lock that is already closed
        client.maybePingOwner(port, 10, "lock 1", 50000)

        then:
        canHandleMoreRequests()
    }

    private void canHandleMoreRequests() {
        def executed = new MutableBoolean()
        int port = handler.reservePort();
        handler.start(15, { executed.set(true) } as Runnable)
        client.maybePingOwner(port, 15, "lock", 50000)
        poll { assert executed.get() }
    }

    def "reserving port is safely reentrant"() {
        when:
        int port = handler.reservePort()

        then:
        handler.reservePort() == port
    }

    def "cannot reserve port when the handler was stopped"() {
        handler.stop()

        when:
        handler.reservePort()

        then:
        thrown(IllegalStateException)
    }

    def "reserving port does not start the thread"() {
        def factory = Mock(ExecutorFactory)
        handler = new DefaultFileLockContentionHandler(factory, addressFactory)

        when:
        handler.reservePort()

        then:
        0 * factory._
    }

    def "stopping the handler stops both executors"() {
        def factory = Mock(ExecutorFactory)
        def lockRequestListener = Mock(ManagedExecutor)
        def releaseLockActionExecutor = Mock(ManagedExecutor)
        handler = new DefaultFileLockContentionHandler(factory, addressFactory)

        when:
        handler.reservePort()
        handler.start(10, {} as Runnable)
        handler.stop()

        then:
        1 * factory.create(_ as String) >> lockRequestListener
        1 * factory.create(_ as String) >> releaseLockActionExecutor
        1 * lockRequestListener.stop()
        1 * releaseLockActionExecutor.stop()
    }

    def "stopping is safe even if the handler was not initialized"() {
        when:
        handler.stop()

        then:
        noExceptionThrown()
    }

    def "stopping is safe even if the executor was not initialized"() {
        handler.reservePort()

        when:
        handler.stop()

        then:
        noExceptionThrown()
    }
}
