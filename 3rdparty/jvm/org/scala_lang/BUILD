load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "scala_compiler",
    exports = [
        "//3rdparty/jvm/org/scala_lang/modules:scala_xml_2_11",
        "//external:jar/org/scala_lang/scala_compiler"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "scala_library",
    exports = [
        "//external:jar/org/scala_lang/scala_library"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "scala_reflect",
    exports = [
        "//external:jar/org/scala_lang/scala_reflect"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "scalap",
    exports = [
        "//external:jar/org/scala_lang/scalap",
        ":scala_compiler"
    ],
    visibility = [
        "//visibility:public"
    ]
)


