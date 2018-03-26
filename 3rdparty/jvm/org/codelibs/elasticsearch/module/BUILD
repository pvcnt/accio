load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "lang_painless",
    exports = [
        "//3rdparty/jvm/org/antlr:antlr4_runtime",
        "//3rdparty/jvm/org/ow2/asm:asm_debug_all",
        "//external:jar/org/codelibs/elasticsearch/module/lang_painless"
    ],
    visibility = [
        "//visibility:public"
    ]
)


