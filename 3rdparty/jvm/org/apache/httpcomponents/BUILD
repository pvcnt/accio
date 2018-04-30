load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "httpclient",
    exports = [
        "//3rdparty/jvm/commons_codec:commons_codec",
        "//3rdparty/jvm/commons_logging:commons_logging",
        "//external:jar/org/apache/httpcomponents/httpclient"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "httpcore",
    exports = [
        "//external:jar/org/apache/httpcomponents/httpcore"
    ],
    visibility = [
        "//visibility:public"
    ]
)


