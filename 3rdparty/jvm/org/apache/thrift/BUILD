load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "libthrift",
    exports = [
        "//3rdparty/jvm/org/apache/httpcomponents:httpclient",
        "//3rdparty/jvm/org/apache/httpcomponents:httpcore",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        "//external:jar/org/apache/thrift/libthrift"
    ],
    visibility = [
        "//visibility:public"
    ]
)


