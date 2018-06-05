load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "junit_platform_commons",
    exports = [
        "//external:jar/org/junit/platform/junit_platform_commons"
    ],
    visibility = [
        "//3rdparty/jvm:__subpackages__"
    ]
)



java_library(
    name = "junit_platform_engine",
    exports = [
        "//3rdparty/jvm/org/opentest4j:opentest4j",
        "//external:jar/org/junit/platform/junit_platform_engine",
        ":junit_platform_commons"
    ],
    visibility = [
        "//3rdparty/jvm:__subpackages__"
    ]
)


