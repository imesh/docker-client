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

package io.fabric8.docker.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.docker.api.model.Doneable;
import io.fabric8.docker.client.utils.Utils;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "stream",
        "errorDetail",
        "error"
})
@Buildable(editableEnabled = true, validationEnabled = true, generateBuilderPackage = false, builderPackage = "io.fabric8.docker.api.builder", inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done"))
public class ProgressEvent {

    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private String status;
    @JsonProperty("progressDetail")
    private String progressDetail;
    @JsonProperty("stream")
    private String stream;
    @JsonProperty("errorDetail")
    private ErrorDetail errorDetail;
    @JsonProperty("error")
    private String error;

    public ProgressEvent() {
    }

    public ProgressEvent(String stream, ErrorDetail errorDetail, String error) {
        this.stream = stream;
        this.error = error;
        this.errorDetail = errorDetail;
    }

    public String getStream() {
        return stream;
    }

    public String getError() {
        return error;
    }

    public ErrorDetail getErrorDetail() {
        return errorDetail;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {

        @JsonProperty("message")
        private String message;

        public ErrorDetail() {
            this(null);
        }
        public ErrorDetail(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (Utils.isNotNullOrEmpty(id)) {
            sb.append(id).append(":");
        }

        if (Utils.isNotNullOrEmpty(status)) {
            sb.append(status);
        }

        if (Utils.isNotNullOrEmpty(stream)) {
            sb.append(stream);
        }

        if (Utils.isNotNullOrEmpty(error)) {
            sb.append(error);
            if (errorDetail != null) {
                sb.append(":").append(errorDetail);
            }
        }
        return sb.toString();
    }
}
