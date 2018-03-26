load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "elasticsearch",
    exports = [
        "//3rdparty/jvm/com/carrotsearch:hppc",
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_core",
        "//3rdparty/jvm/com/fasterxml/jackson/dataformat:jackson_dataformat_cbor",
        "//3rdparty/jvm/com/fasterxml/jackson/dataformat:jackson_dataformat_smile",
        "//3rdparty/jvm/com/fasterxml/jackson/dataformat:jackson_dataformat_yaml",
        "//3rdparty/jvm/com/tdunning:t_digest",
        "//3rdparty/jvm/joda_time:joda_time",
        "//3rdparty/jvm/net/java/dev/jna:jna",
        "//3rdparty/jvm/net/sf/jopt_simple:jopt_simple",
        "//3rdparty/jvm/org/hdrhistogram:HdrHistogram",
        "//3rdparty/jvm/org/yaml:snakeyaml",
        "//external:jar/org/elasticsearch/elasticsearch",
        ":securesm"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "securesm",
    exports = [
        "//external:jar/org/elasticsearch/securesm"
    ],
    visibility = [
        "//visibility:public"
    ]
)


