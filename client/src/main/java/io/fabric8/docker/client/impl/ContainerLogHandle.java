/*
 * Copyright (C) 2016 Original Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.fabric8.docker.client.impl;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import io.fabric8.docker.client.DockerClientException;
import io.fabric8.docker.client.DockerStreamData;
import io.fabric8.docker.client.utils.DockerStreamPumper;
import io.fabric8.docker.dsl.OutputErrorHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContainerLogHandle implements OutputErrorHandle, Callback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerLogHandle.class);

    private final OutputStream out;
    private final OutputStream err;

    private final PipedInputStream output;
    private final PipedInputStream error;
    private final AtomicBoolean started = new AtomicBoolean(false);

    protected final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private DockerStreamPumper pumper;

    public ContainerLogHandle(OutputStream out, OutputStream err, PipedInputStream outputPipe, PipedInputStream errorPipe) {
        this.out = outputStreamOrPipe(out, outputPipe);
        this.err = outputStreamOrPipe(err, errorPipe);

        this.output = outputPipe;
        this.error = errorPipe;
    }

    @Override
    public void close() {
        executorService.shutdown();

        try {
            if (executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (Throwable t) {
            throw DockerClientException.launderThrowable(t);
        }
    }

    public void waitUntilReady() {
        try {
            Object obj = queue.poll(10, TimeUnit.SECONDS);
            if (obj instanceof Boolean && ((Boolean) obj)) {
                return;
            } else {
                if (obj instanceof Throwable) {
                    throw (Throwable) obj;
                }
            }
        } catch (Throwable t) {
            throw DockerClientException.launderThrowable(t);
        }
    }
    @Override
    public void onFailure(Call call, IOException ioe) {
        LOGGER.error("Request Failure.", ioe);
        //We only need to queue startup failures.
        if (!started.get()) {
            queue.add(ioe);
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (out instanceof PipedOutputStream && output != null) {
            output.connect((PipedOutputStream) out);
        }

        pumper = new DockerStreamPumper(response.body().source(), new io.fabric8.docker.api.model.Callback<DockerStreamData, Void>() {

            @Override
            public Void call(DockerStreamData input) {
                try {
                    switch (input.streamType()) {
                        case STDIN:
                        case STDOUT:
                            if (out != null) {
                                out.write(input.payload());
                            }
                            break;
                        case STDERR:
                            if (err != null) {
                                err.write(input.payload());
                            }
                            break;
                        default:
                            throw new IOException("Unknown stream ID " + input.streamType());
                    }
                } catch (IOException e) {
                    throw DockerClientException.launderThrowable(e);
                }
                return null;
            }
        });
        executorService.submit(pumper);
        started.set(true);
        queue.add(true);
    }


    public InputStream getOutput() {
        return output;
    }

    public InputStream getError() {
        return error;
    }

    private static OutputStream outputStreamOrPipe(OutputStream stream, PipedInputStream in) {
        if (stream != null) {
            return stream;
        } else if (in != null) {
            return new PipedOutputStream();
        } else {
            return null;
        }
    }


}
