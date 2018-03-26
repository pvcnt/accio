load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
scala_import(
    name = "breeze",
    exports = [
        "//3rdparty/jvm/com/chuusai:shapeless_2_11",
        "//3rdparty/jvm/com/github/fommil/netlib:core",
        "//3rdparty/jvm/com/github/rwl:jtransforms",
        "//3rdparty/jvm/net/sf/opencsv:opencsv",
        "//3rdparty/jvm/net/sourceforge/f2j:arpack_combined_all",
        "//3rdparty/jvm/org/apache/commons:commons_math3",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        "//3rdparty/jvm/org/spire_math:spire_2_11",
        ":breeze_macros_2_11"
    ],
    jars = [
        "//external:jar/org/scalanlp/breeze_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "breeze_macros_2_11",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_reflect",
        "//external:jar/org/scalanlp/breeze_macros_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)


