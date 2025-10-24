/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.eealba.payper.core.web.internal;

import io.github.eealba.payper.core.exceptions.PayperException;
import io.github.eealba.payper.core.web.Request;
import io.github.eealba.payper.core.web.Response;
import io.github.eealba.payper.core.web.WebClient;
import io.github.eealba.payper.core.web.WebClientConfig;
import okhttp3.Call;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Author: Edgar Alba
 */
class WebClientImpl extends WebClient {
    private final OkHttpClient client;
    private final Mapper mapper = MapperImpl.getInstance();

    WebClientImpl(WebClientConfig config) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (config.connectTimeout().isPresent()) {
            builder.connectTimeout(config.connectTimeout().get());
        }
        if (config.proxySelector().isPresent()){
            builder.proxySelector(config.proxySelector().get());
        }
        this.client = builder.build();
    }

    @Override
    public <T> CompletableFuture<Response<T>> sendAsync(Request request, Response.BodyHandler<T> responseBodyHandler) {
        okhttp3.Request httpRequest = mapper.mapRequest(request);

        Call call = client.newCall(httpRequest);
        if (request.timeout().isPresent())
            call.timeout().timeout(request.timeout().get().toMillis(), TimeUnit.MILLISECONDS);

        return CompletableFuture.supplyAsync(() -> new ByteArrayResponse<>(exec(call), responseBodyHandler));
    }

    public CompletableFuture<Response<Void>> sendAsync(Request request) {
        okhttp3.Request httpRequest = mapper.mapRequest(request);

        Call call = client.newCall(httpRequest);
        if (request.timeout().isPresent())
            call.timeout().timeout(request.timeout().get().toMillis(), TimeUnit.MILLISECONDS);

        return CompletableFuture.supplyAsync(() -> new EmptyResponseImpl<>(exec(call)));
    }

    @Override
    public <T> Response<T> send(Request request, Response.BodyHandler<T> bodyHandler){
        okhttp3.Request httpRequest = mapper.mapRequest(request);

        Call call = client.newCall(httpRequest);
        if (request.timeout().isPresent())
            call.timeout().timeout(request.timeout().get().toMillis(), TimeUnit.MILLISECONDS);

        return new ByteArrayResponse<>(exec(call), bodyHandler);
    }

    private okhttp3.Response exec(Call call) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new PayperException(e);
        }
    }
}