RESTHeart Customization Examples
=========

> [RESTHeart](restheart.org) - The leading Web API for MongoDB. 
> 
> &nbsp;&nbsp;Done with **love** by the [SoftInstigate Team](http://www.softinstigate.com)


The following customization examples are included:
----
* **ExampleIdentityManager** an Identity Manager, see [documentation](https://softinstigate.atlassian.net/wiki/x/HADM)
* **ExampleAccessManager** an Access Manager, see [documentation](https://softinstigate.atlassian.net/wiki/x/HgDM)
* **ExampleAggregateHandler** a simple Application Logic Handler that executes an aggregation query, see [documentation](https://softinstigate.atlassian.net/wiki/x/IoCw)

How to run the examples
----

```
$ git clone https://github.com/SoftInstigate/restheart-customization-examples.git
$ cd restheart-customization-examples
$ mvn package
$ cp <RESTHEART_DIR>/restheart.jar .
$ java -cp restheart.jar:target/restheart-customization-examples-1.0-SNAPSHOT.jar org.restheart.Bootstrapper etc/restheart.yml 
```

Create test data
----

You need to install [httpie](https://github.com/jkbrzt/httpie)

```
$ http -a admin:nimda PUT 127.0.0.1:8080/test desc="a db for testing"
$ http -a admin:nimda PUT 127.0.0.1:8080/test/bands desc="a collection to hold gread bands data"
$ http -a admin:nimda PUT "127.0.0.1:8080/test/bands/Pink Floyd" albums:='["The Piper at the Gates of Dawn","A Saucerful of Secrets","More" \
,"Ummagumma","Atom Heart Mother","Meddle","Obscured by Clouds","The Dark Side of the Moon","Wish You Were Here","Animals" \
,"The Wall","The Final Cut","A Momentary Lapse of Reason","The Division Bell","The Endless River"]'


$ http -a ciao:oaic 127.0.0.1:8080/_logic/aggregate
HTTP/1.1 200 OK
...

{
    "Pink Floyd": 15, 
    "_embedded": {}, 
    "_links": {
        "self": {
            "href": "/_logic/aggregate"
        }
    }
}

```

ExampleIdentityManager
---

This IDM verifies the password to be the flipped id string, i.e. id="username" => pwd="emanresu"


ExampleAccessManager
---

This Access Manager gives read access to the /test/band collection and /_logic/aggregate to any authenticated user. It also gives write access to user "admin"

ExampleAggregateHandler
---

This custom Hanlder is boud to /_logic/aggregate URI. It execute a simply aggregation query that returs the count of ablums published by bands.
