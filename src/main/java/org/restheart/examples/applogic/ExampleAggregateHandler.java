/*
 * RESTHeart - the Web API for MongoDB
 * Copyright (C) 2014 - 2015 SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.examples.applogic;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.restheart.cache.Cache;
import org.restheart.cache.CacheFactory;
import org.restheart.cache.LoadingCache;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.utils.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrea Di Cesare <andrea@softinstigate.com>
 */
public class ExampleAggregateHandler extends ApplicationLogicHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart.example.AggregateHandler");

    private static final MongoClient client;
    private static final LoadingCache<String, DBObject> cache;

    public static final String DB = "test";
    public static final String COLL = "bands";

    private static final List<Document> AGGREGATION_QUERY;

    static {
        client = MongoDBClientSingleton.getInstance().getClient();

        AGGREGATION_QUERY = Arrays.asList(
                new Document("$unwind", "$albums"),
                new Document("$group", new Document("_id", "$_id")
                        .append("count", new Document("$sum", 1))),
                new Document("$project", new Document("albums", "$count"))
        );

        LOGGER.debug("query {}", AGGREGATION_QUERY.toString());

        // a 5 seconds cache
        cache = CacheFactory.createLocalLoadingCache(1, Cache.EXPIRE_POLICY.AFTER_WRITE, 5 * 1000, (String key) -> {

            MongoCollection<Document> coll = client.getDatabase(DB).getCollection(COLL);

            AggregateIterable<Document> agout = coll.aggregate(AGGREGATION_QUERY);

            // wrap result in a BasicDBList
            BasicDBList ret = new BasicDBList();
            for (Document document : agout) {
                ret.add(document);
            }

            return ret;
        });
    }

    /**
     *
     * @param next
     * @param args
     */
    public ExampleAggregateHandler(PipedHttpHandler next, Map<String, Object> args) {
        super(next, args);
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        LOGGER.debug("got request");

        if (context.getMethod() == METHOD.GET) {
            Optional<DBObject> _results = cache.getLoading("result");

            if (_results.isPresent()) {
                BasicDBList results = (BasicDBList) _results.get();

                Representation rep = new Representation("/_logic/aggregate");

                BsonDocument properties = new BsonDocument();

                results.forEach(res -> {
                    DBObject _res = (DBObject) res;
                    properties.append((String) _res.get("_id"), (BsonValue) _res.get("albums"));
                });

                rep.addProperties(properties);

                exchange.setStatusCode(HttpStatus.SC_OK);

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.JSON_MEDIA_TYPE);
                exchange.getResponseSender().send(rep.toString());

                exchange.endExchange();
            }
        } else {
            LOGGER.debug("request verb is not GET => NOT ALLOWED");
            exchange.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            exchange.endExchange();
        }
    }
}
