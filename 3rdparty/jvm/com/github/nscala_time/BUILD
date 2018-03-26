load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
scala_import(
    name = "nscala_time",
    exports = [
        "//3rdparty/jvm/joda_time:joda_time",
        "//3rdparty/jvm/org/joda:joda_convert",
        "//3rdparty/jvm/org/scala_lang:scala_library"
    ],
    jars = [
        "//external:jar/com/github/nscala_time/nscala_time_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)


