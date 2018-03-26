load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "rest",
    exports = [
        "//3rdparty/jvm/commons_codec:commons_codec",
        "//3rdparty/jvm/commons_logging:commons_logging",
        "//3rdparty/jvm/org/apache/httpcomponents:httpasyncclient",
        "//3rdparty/jvm/org/apache/httpcomponents:httpcore",
        "//3rdparty/jvm/org/apache/httpcomponents:httpcore_nio",
        "//external:jar/org/elasticsearch/client/rest"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "transport",
    exports = [
        "//3rdparty/jvm/org/elasticsearch/plugin:lang_mustache_client",
        "//3rdparty/jvm/org/elasticsearch/plugin:percolator_client",
        "//3rdparty/jvm/org/elasticsearch/plugin:reindex_client",
        "//3rdparty/jvm/org/elasticsearch/plugin:transport_netty3_client",
        "//3rdparty/jvm/org/elasticsearch/plugin:transport_netty4_client",
        "//3rdparty/jvm/org/elasticsearch:elasticsearch",
        "//external:jar/org/elasticsearch/client/transport"
    ],
    visibility = [
        "//visibility:public"
    ]
)


