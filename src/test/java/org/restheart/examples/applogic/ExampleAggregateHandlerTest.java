/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restheart.examples.applogic;

import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mturatti
 */
public class ExampleAggregateHandlerTest {

    public ExampleAggregateHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAggreagationString() throws Exception {
        List<Document> query = Arrays.asList(
                new Document("$unwind", "$albums"),
                new Document("$group", new Document("_id", "$_id")
                        .append("count", new Document("$sum", 1))),
                new Document("$project", new Document("albums", "$count"))
        );

        Assert.assertEquals("[Document{{$unwind=$albums}}, Document{{$group=Document{{_id=$_id, count=Document{{$sum=1}}}}}}, Document{{$project=Document{{albums=$count}}}}]", query.toString());
    }

}
