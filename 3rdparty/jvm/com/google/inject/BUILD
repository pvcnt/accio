load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "guice",
    exports = [
        "//3rdparty/jvm/aopalliance:aopalliance",
        "//3rdparty/jvm/com/google/guava:guava",
        "//3rdparty/jvm/javax/inject:javax_inject",
        "//external:jar/com/google/inject/guice"
    ],
    visibility = [
        "//3rdparty/jvm:__subpackages__"
    ]
)


