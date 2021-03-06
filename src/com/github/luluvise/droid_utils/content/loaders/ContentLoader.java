/*
 * Copyright 2013 Luluvise Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.luluvise.droid_utils.content.loaders;

import javax.annotation.Nonnull;

import com.github.luluvise.droid_utils.content.ContentProxy;
import com.github.luluvise.droid_utils.content.ContentProxy.ActionType;
import com.github.luluvise.droid_utils.json.model.JsonModel;
import com.github.luluvise.droid_utils.json.rest.AbstractModelRequest;
import com.google.common.annotations.Beta;

/**
 * Generic interface for components that take care of loading {@link JsonModel}s
 * contents from a content proxy, using caches or network requests through a
 * provided {@link AbstractModelRequest}.
 * 
 * @param <R>
 *            Requests extending {@link AbstractModelRequest}
 * @param <M>
 *            Models extending {@link JsonModel}
 * 
 * @since 1.0
 * @author Marco Salis
 */
@Beta
public interface ContentLoader<R extends AbstractModelRequest<M>, M extends JsonModel> {

	/**
	 * Starts the content retrieval.
	 * 
	 * @param action
	 *            The {@link ActionType} to use
	 * @param request
	 *            The {@link AbstractModelRequest} to retrieve the content
	 * @param callback
	 *            An optional {@link ContentUpdateCallback}
	 * @return The retrieved {@JsonModel}, or null if unsuccessful.
	 * @throws Exception
	 *             if an exception was thrown during the retrieval.
	 */
	public M load(ActionType action, @Nonnull final R request, ContentUpdateCallback<M> callback)
			throws Exception;

	/**
	 * Interface for custom handlers that a {@link ContentLoader} uses to
	 * validate and execute an {@link AbstractModelRequest}.
	 * 
	 * The purpose is to abstract as much as possible the request execution,
	 * allowing callers to inject custom actions when the content loader asks
	 * for a request execution.
	 * 
	 * @param <R>
	 *            The type of request (can be a subclass of
	 *            {@link AbstractModelRequest})
	 */
	@Beta
	public static interface RequestHandler {

		/**
		 * Validates a request before execution. This can be used to perform
		 * customizations or set values that may affect the request hash (or
		 * URL) in a non-immutable request prior to passing them through the
		 * {@link ContentLoader} and being executed.
		 * 
		 * See {@link AbstractModelRequest#setRequestUrl(String)} for more
		 * information on how a request hash might change.
		 * 
		 * @param request
		 *            The {@link AbstractModelRequest} to validate
		 * @return true if the passed request has been modified, false otherwise
		 */
		public boolean validateRequest(@Nonnull AbstractModelRequest<?> request);

		/**
		 * Executes the passed request with the handler.
		 * 
		 * @param request
		 *            The {@link AbstractModelRequest} to execute
		 * @return The retrieved {@link JsonModel} or null
		 * @throws Exception
		 *             if the request threw an exception
		 */
		public JsonModel execRequest(@Nonnull AbstractModelRequest<?> request) throws Exception;
	}

	/**
	 * Interface that {@link ContentProxy} can implement to pass themselves to a
	 * task and be notified when a {@link JsonModel} content has been refreshed
	 * from the remote location, in order to perform specific
	 * "cache maintenance" operations.
	 * 
	 * Sometimes, for example, a model update from the server can cause other
	 * existing wrapped/dependent model items in cache to be invalidated. By
	 * passing this callback to a {@link ContentProxy} it can handle this update
	 * transparently.
	 * 
	 * @param <CONTENT>
	 *            The model updated
	 */
	public static interface ContentUpdateCallback<CONTENT extends JsonModel> {

		/**
		 * Called when a new content has been updated from outside the local
		 * cache.
		 * 
		 * @param newContent
		 *            The new {@link JsonModel} content
		 */
		public void onContentUpdated(@Nonnull CONTENT newContent);
	}

}