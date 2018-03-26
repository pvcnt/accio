load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
scala_import(
    name = "elastic4s_core",
    exports = [
        "//3rdparty/jvm/com/sksamuel/exts:exts_2_11",
        "//3rdparty/jvm/io/netty:netty_all",
        "//3rdparty/jvm/org/apache/httpcomponents:httpclient",
        "//3rdparty/jvm/org/apache/logging/log4j:log4j_api",
        "//3rdparty/jvm/org/apache/lucene:lucene_analyzers_common",
        "//3rdparty/jvm/org/apache/lucene:lucene_backward_codecs",
        "//3rdparty/jvm/org/apache/lucene:lucene_core",
        "//3rdparty/jvm/org/apache/lucene:lucene_grouping",
        "//3rdparty/jvm/org/apache/lucene:lucene_highlighter",
        "//3rdparty/jvm/org/apache/lucene:lucene_join",
        "//3rdparty/jvm/org/apache/lucene:lucene_memory",
        "//3rdparty/jvm/org/apache/lucene:lucene_misc",
        "//3rdparty/jvm/org/apache/lucene:lucene_queries",
        "//3rdparty/jvm/org/apache/lucene:lucene_queryparser",
        "//3rdparty/jvm/org/apache/lucene:lucene_sandbox",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial3d",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial_extras",
        "//3rdparty/jvm/org/apache/lucene:lucene_suggest",
        "//3rdparty/jvm/org/elasticsearch/client:rest",
        "//3rdparty/jvm/org/elasticsearch/client:transport",
        "//3rdparty/jvm/org/locationtech/spatial4j:spatial4j",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        "//3rdparty/jvm/org/typelevel:cats_2_11"
    ],
    jars = [
        "//external:jar/com/sksamuel/elastic4s/elastic4s_core_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "elastic4s_embedded_2_11",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/dataformat:jackson_dataformat_cbor",
        "//3rdparty/jvm/com/fasterxml/jackson/dataformat:jackson_dataformat_smile",
        "//3rdparty/jvm/org/apache/logging/log4j:log4j_1_2_api",
        "//3rdparty/jvm/org/apache/logging/log4j:log4j_core",
        "//3rdparty/jvm/org/elasticsearch:elasticsearch",
        "//external:jar/com/sksamuel/elastic4s/elastic4s_embedded_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "elastic4s_jackson",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_core",
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_databind",
        "//3rdparty/jvm/com/fasterxml/jackson/datatype:jackson_datatype_joda",
        "//3rdparty/jvm/com/fasterxml/jackson/module:jackson_module_scala_2_11",
        "//3rdparty/jvm/com/sksamuel/exts:exts_2_11",
        "//3rdparty/jvm/io/netty:netty_all",
        "//3rdparty/jvm/org/apache/httpcomponents:httpclient",
        "//3rdparty/jvm/org/apache/logging/log4j:log4j_api",
        "//3rdparty/jvm/org/apache/lucene:lucene_analyzers_common",
        "//3rdparty/jvm/org/apache/lucene:lucene_backward_codecs",
        "//3rdparty/jvm/org/apache/lucene:lucene_core",
        "//3rdparty/jvm/org/apache/lucene:lucene_grouping",
        "//3rdparty/jvm/org/apache/lucene:lucene_highlighter",
        "//3rdparty/jvm/org/apache/lucene:lucene_join",
        "//3rdparty/jvm/org/apache/lucene:lucene_memory",
        "//3rdparty/jvm/org/apache/lucene:lucene_misc",
        "//3rdparty/jvm/org/apache/lucene:lucene_queries",
        "//3rdparty/jvm/org/apache/lucene:lucene_queryparser",
        "//3rdparty/jvm/org/apache/lucene:lucene_sandbox",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial3d",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial_extras",
        "//3rdparty/jvm/org/apache/lucene:lucene_suggest",
        "//3rdparty/jvm/org/elasticsearch/client:rest",
        "//3rdparty/jvm/org/elasticsearch/client:transport",
        "//3rdparty/jvm/org/locationtech/spatial4j:spatial4j",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        "//3rdparty/jvm/org/typelevel:cats_2_11",
        ":elastic4s_core"
    ],
    jars = [
        "//external:jar/com/sksamuel/elastic4s/elastic4s_jackson_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "elastic4s_testkit",
    exports = [
        "//3rdparty/jvm/com/sksamuel/exts:exts_2_11",
        "//3rdparty/jvm/io/netty:netty_all",
        "//3rdparty/jvm/org/apache/httpcomponents:httpclient",
        "//3rdparty/jvm/org/apache/logging/log4j:log4j_api",
        "//3rdparty/jvm/org/apache/lucene:lucene_analyzers_common",
        "//3rdparty/jvm/org/apache/lucene:lucene_backward_codecs",
        "//3rdparty/jvm/org/apache/lucene:lucene_core",
        "//3rdparty/jvm/org/apache/lucene:lucene_grouping",
        "//3rdparty/jvm/org/apache/lucene:lucene_highlighter",
        "//3rdparty/jvm/org/apache/lucene:lucene_join",
        "//3rdparty/jvm/org/apache/lucene:lucene_memory",
        "//3rdparty/jvm/org/apache/lucene:lucene_misc",
        "//3rdparty/jvm/org/apache/lucene:lucene_queries",
        "//3rdparty/jvm/org/apache/lucene:lucene_queryparser",
        "//3rdparty/jvm/org/apache/lucene:lucene_sandbox",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial3d",
        "//3rdparty/jvm/org/apache/lucene:lucene_spatial_extras",
        "//3rdparty/jvm/org/apache/lucene:lucene_suggest",
        "//3rdparty/jvm/org/elasticsearch/client:rest",
        "//3rdparty/jvm/org/elasticsearch/client:transport",
        "//3rdparty/jvm/org/locationtech/spatial4j:spatial4j",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/scalatest:scalatest_2_11",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        "//3rdparty/jvm/org/typelevel:cats_2_11",
        ":elastic4s_core",
        ":elastic4s_embedded_2_11"
    ],
    jars = [
        "//external:jar/com/sksamuel/elastic4s/elastic4s_testkit_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)


