package org.restheart.examples.applogic;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.utils.HttpStatus;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.restheart.cache.Cache;
import org.restheart.cache.CacheFactory;
import org.restheart.cache.LoadingCache;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
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

    private static final List<DBObject> AGGREGATION_QUERY;

    static {
        client = MongoDBClientSingleton.getInstance().getClient();

        AGGREGATION_QUERY = Arrays.asList(
                BasicDBObjectBuilder.start()
                .add("$unwind", "$albums") // unwind stage
                .get(),
                BasicDBObjectBuilder.start()
                .push("$group") // group stage
                .add("_id", "$_id")
                .push("count")
                .add("$sum", 1)
                .get(),
                BasicDBObjectBuilder.start()
                .push("$project") // $project stage
                .add("albums", "$count")
                .get()
        );

        LOGGER.debug("query {}", AGGREGATION_QUERY.toString());

        // a 5 seconds cache
        cache = CacheFactory.createLocalLoadingCache(1, Cache.EXPIRE_POLICY.AFTER_WRITE, 5 * 1000, (String key) -> {

            DBCollection coll = client.getDB(DB).getCollection(COLL);

            AggregationOutput agout = coll.aggregate(AGGREGATION_QUERY);

            // wrap result in a BasicDBList
            BasicDBList ret = new BasicDBList();
            agout.results().forEach(dbobj -> {
                ret.add(dbobj);
            });

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

                BasicDBObject properties = new BasicDBObject();
                
                results.forEach( res -> { 
                    DBObject _res = (DBObject) res;
                    
                    properties.append((String) _res.get("_id"), _res.get("albums")); 
                });
                
                rep.addProperties(properties);

                exchange.setResponseCode(HttpStatus.SC_OK);

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.JSON_MEDIA_TYPE);
                exchange.getResponseSender().send(rep.toString());

                exchange.endExchange();
            }
        } else {
            LOGGER.debug("request verb is not GET => NOT ALLOWED");
            exchange.setResponseCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            exchange.endExchange();
        }
    }
}
