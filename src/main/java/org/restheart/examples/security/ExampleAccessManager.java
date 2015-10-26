/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package org.restheart.examples.security;

import io.undertow.attribute.ExchangeAttributes;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import java.util.Map;
import java.util.Set;
import org.restheart.examples.applogic.ExampleAggregateHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import static org.restheart.handlers.RequestContext.PATCH;
import org.restheart.handlers.RequestContext.TYPE;
import org.restheart.security.AccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrea Di Cesare <andrea@softinstigate.com>
 */
public class ExampleAccessManager implements AccessManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.examples.security.ExampleAccessManager");

    public static final String AGGREGATE_URI = "/_logic/aggregate";

    public ExampleAccessManager(Map<String, Object> arguments) {
        // args are ignored
    }

    @Override
    public boolean isAllowed(HttpServerExchange exchange, RequestContext context) {
        // *** request info
        String requestURI = exchange.getRequestURI();
        String db = null;
        String collection = null;
        TYPE requestType;
        METHOD requestMethod;

        // context is null for secured non-mongodb resources, e.g. /_logic
        if (context != null) {
            db = context.getDBName();
            collection = context.getCollectionName();
            requestType = context.getType();
            requestMethod = context.getMethod();
        } else {
            db = null;
            collection = null;
            requestType = null;
            requestMethod = selectRequestMethod(exchange.getRequestMethod());
        }

        // *** user info
        String username = ExchangeAttributes.remoteUser().readAttribute(exchange);
        Set<String> roles = null;

        if (exchange.getSecurityContext() != null && exchange.getSecurityContext().getAuthenticatedAccount() != null) {
            roles = exchange.getSecurityContext().getAuthenticatedAccount().getRoles();
        }

        if (username == null || roles == null) {
            LOGGER.warn("DENIED, user is not authenticated.");
            return false;
        }

        // log request
        LOGGER.debug("checking request {} {} from user {} with roles {}", requestMethod, requestURI, username, roles);

        // allow ny authenticated user to GET /_logic/aggregate
        if (AGGREGATE_URI.equals(requestURI)) {
            if (METHOD.GET.equals(requestMethod)) {
                LOGGER.debug("ALLOWED (GET request to {} are allowed)", requestURI);
                return true;
            }
        }

        // allow any authenticated user to GET,POST /test/bands
        if (TYPE.COLLECTION.equals(requestType)) {
            if (ExampleAggregateHandler.DB.equals(db)
                    && ExampleAggregateHandler.COLL.equals(collection)
                    && (METHOD.GET.equals(requestMethod))
                    || METHOD.POST.equals(requestMethod)) {
                LOGGER.debug("ALLOWED (anyone can GET and POST /test/bands)");
                return true;
            }
        }

        // allow any authenticated user to GET /test/bands/<bandid>
        if (TYPE.DOCUMENT.equals(requestType)
                && ExampleAggregateHandler.DB.equals(db)
                && ExampleAggregateHandler.COLL.equals(collection)
                && METHOD.GET.equals(requestMethod)) {
            LOGGER.debug("ALLOWED (anyone can GET /test/bands/<docid>)");
            return true;
        }

        // allow users with ROLE admin to GET,PUT,PATCH,DELETE /test/bands/<bandid>
        if (roles.contains(ExampleIdentityManager.ROLE.ADMIN.name())
                && TYPE.DOCUMENT.equals(requestType)
                && ExampleAggregateHandler.DB.equals(db)
                && ExampleAggregateHandler.COLL.equals(collection)
                && (METHOD.PUT.equals(requestMethod)
                || METHOD.PATCH.equals(requestMethod)
                || METHOD.DELETE.equals(requestMethod))) {
            LOGGER.debug("ALLOWED (admins can PUT, PATCH and DELETE /test/bands/<docid>)");
            return true;
        }

        LOGGER.warn("DENIED, no permission found.");
        return false;
    }

    @Override
    public boolean isAuthenticationRequired(HttpServerExchange exchange) {
        // always require authentication
        return true;
    }
    
    private static METHOD selectRequestMethod(HttpString _method) {
        METHOD method;
        if (Methods.GET.equals(_method)) {
            method = METHOD.GET;
        } else if (Methods.POST.equals(_method)) {
            method = METHOD.POST;
        } else if (Methods.PUT.equals(_method)) {
            method = METHOD.PUT;
        } else if (Methods.DELETE.equals(_method)) {
            method = METHOD.DELETE;
        } else if (PATCH.equals(_method.toString())) {
            method = METHOD.PATCH;
        } else if (Methods.OPTIONS.equals(_method)) {
            method = METHOD.OPTIONS;
        } else {
            method = METHOD.OTHER;
        }
        return method;
    }
}
