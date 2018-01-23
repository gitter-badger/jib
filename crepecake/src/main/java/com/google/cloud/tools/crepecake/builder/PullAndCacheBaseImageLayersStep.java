/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.crepecake.builder;

import com.google.cloud.tools.crepecake.cache.Cache;
import com.google.cloud.tools.crepecake.cache.CachedLayer;
import com.google.cloud.tools.crepecake.http.Authorization;
import com.google.cloud.tools.crepecake.image.DuplicateLayerException;
import com.google.cloud.tools.crepecake.image.Image;
import com.google.cloud.tools.crepecake.image.ImageLayers;
import com.google.cloud.tools.crepecake.image.Layer;
import com.google.cloud.tools.crepecake.image.LayerPropertyNotFoundException;
import com.google.cloud.tools.crepecake.registry.RegistryClient;
import com.google.cloud.tools.crepecake.registry.RegistryException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

class PullAndCacheBaseImageLayersStep implements Callable<List<ListenableFuture<CachedLayer>>> {

  private final BuildConfiguration buildConfiguration;
  private final Cache cache;
  private final ListeningExecutorService listeningExecutorService;
  private final ListenableFuture<Authorization> pullAuthorizationFuture;
  private final ListenableFuture<Image> baseImageFuture;


  PullAndCacheBaseImageLayersStep(
      BuildConfiguration buildConfiguration,
      Cache cache,
      ListeningExecutorService listeningExecutorService,
      ListenableFuture<Authorization> pullAuthorizationFuture,
      ListenableFuture<Image> baseImageFuture) {
    this.buildConfiguration = buildConfiguration;
    this.cache = cache;
    this.listeningExecutorService = listeningExecutorService;
    this.pullAuthorizationFuture = pullAuthorizationFuture;
    this.baseImageFuture = baseImageFuture;
  }

  @Override
  public List<ListenableFuture<CachedLayer>> call()
      throws ExecutionException, InterruptedException {
    List<ListenableFuture<CachedLayer>> pullAndCacheBaseImageLayerFutures = new ArrayList<>();
    for (Layer layer : baseImageFuture.get().getLayers()) {
      pullAndCacheBaseImageLayerFutures.add(Futures.whenAllSucceed(pullAuthorizationFuture, baseImageFuture).call(new PullAndCacheBaseImageLayerStep(buildConfiguration, cache, layer, pullAuthorizationFuture), listeningExecutorService));
    }

    return pullAndCacheBaseImageLayerFutures;
  }
}