load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "jackson_module_paranamer",
    exports = [
        "//3rdparty/jvm/com/thoughtworks/paranamer:paranamer",
        "//external:jar/com/fasterxml/jackson/module/jackson_module_paranamer"
    ],
    visibility = [
        "//3rdparty/jvm:__subpackages__"
    ]
)



java_library(
    name = "jackson_module_scala_2_11",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_reflect",
        "//external:jar/com/fasterxml/jackson/module/jackson_module_scala_2_11",
        ":jackson_module_paranamer"
    ],
    visibility = [
        "//3rdparty/jvm:__subpackages__"
    ]
)


