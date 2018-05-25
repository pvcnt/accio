load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
scala_import(
    name = "finagle_base_http",
    exports = [
        "//3rdparty/jvm/io/netty:netty_codec_http",
        "//3rdparty/jvm/io/netty:netty_handler",
        "//3rdparty/jvm/io/netty:netty_handler_proxy",
        "//3rdparty/jvm/io/netty:netty_transport",
        "//3rdparty/jvm/io/netty:netty_transport_native_epoll",
        "//3rdparty/jvm/io/netty:netty_transport_native_unix_common",
        ":finagle_core",
        ":finagle_http_cookie_2_11",
        ":finagle_netty3_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_base_http_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_core",
    exports = [
        "//3rdparty/jvm/com/github/ben_manes/caffeine:caffeine",
        "//3rdparty/jvm/com/google/code/findbugs:jsr305",
        "//3rdparty/jvm/org/hdrhistogram:HdrHistogram",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_init_2_11",
        ":finagle_toggle_2_11",
        ":util_app",
        ":util_cache_2_11",
        ":util_codec",
        ":util_core",
        ":util_hashing_2_11",
        ":util_jvm",
        ":util_lint_2_11",
        ":util_logging_2_11",
        ":util_registry_2_11",
        ":util_security_2_11",
        ":util_stats",
        ":util_tunable_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_core_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_exp",
    exports = [
        ":finagle_thrift"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_exp_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_http",
    exports = [
        "//3rdparty/jvm/io/netty:netty_tcnative_boringssl_static",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_base_http",
        ":finagle_http2_2_11",
        ":finagle_netty4_http",
        ":finagle_toggle_2_11",
        ":util_codec",
        ":util_collection_2_11",
        ":util_logging_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_http_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_http2_2_11",
    exports = [
        "//3rdparty/jvm/io/netty:netty_codec_http2",
        "//external:jar/com/twitter/finagle_http2_2_11",
        ":util_cache_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_http_cookie_2_11",
    exports = [
        "//external:jar/com/twitter/finagle_http_cookie_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_init_2_11",
    exports = [
        "//external:jar/com/twitter/finagle_init_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_mux_2_11",
    exports = [
        "//external:jar/com/twitter/finagle_mux_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_mysql",
    exports = [
        "//3rdparty/jvm/com/github/ben_manes/caffeine:caffeine",
        "//3rdparty/jvm/com/google/code/findbugs:jsr305",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_core",
        ":finagle_netty4_2_11",
        ":finagle_toggle_2_11",
        ":util_cache_2_11",
        ":util_logging_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_mysql_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_netty3_2_11",
    exports = [
        "//3rdparty/jvm/io/netty:netty",
        "//external:jar/com/twitter/finagle_netty3_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_netty4_2_11",
    exports = [
        "//3rdparty/jvm/io/netty:netty_handler",
        "//3rdparty/jvm/io/netty:netty_handler_proxy",
        "//3rdparty/jvm/io/netty:netty_transport",
        "//3rdparty/jvm/io/netty:netty_transport_native_epoll",
        "//3rdparty/jvm/io/netty:netty_transport_native_unix_common",
        "//external:jar/com/twitter/finagle_netty4_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_netty4_http",
    exports = [
        "//3rdparty/jvm/commons_lang:commons_lang",
        ":finagle_netty4_2_11",
        ":util_app",
        ":util_core",
        ":util_jvm",
        ":util_stats"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_netty4_http_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_stats",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_core",
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_databind",
        "//3rdparty/jvm/com/fasterxml/jackson/module:jackson_module_scala_2_11",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_core",
        ":finagle_http",
        ":finagle_toggle_2_11",
        ":finagle_tunable_2_11",
        ":util_app",
        ":util_core",
        ":util_lint_2_11",
        ":util_logging_2_11",
        ":util_registry_2_11",
        ":util_stats",
        ":util_tunable_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_stats_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finagle_thrift",
    exports = [
        "//3rdparty/jvm/org/apache/thrift:libthrift",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_core",
        ":finagle_netty4_2_11",
        ":finagle_toggle_2_11",
        ":scrooge_core"
    ],
    jars = [
        "//external:jar/com/twitter/finagle_thrift_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_thriftmux_2_11",
    exports = [
        "//3rdparty/jvm/commons_lang:commons_lang",
        "//external:jar/com/twitter/finagle_thriftmux_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_toggle_2_11",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_core",
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_databind",
        "//3rdparty/jvm/com/fasterxml/jackson/module:jackson_module_scala_2_11",
        "//external:jar/com/twitter/finagle_toggle_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "finagle_tunable_2_11",
    exports = [
        "//external:jar/com/twitter/finagle_tunable_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finatra_http",
    exports = [
        "//3rdparty/jvm/com/github/spullara/mustache/java:compiler",
        "//3rdparty/jvm/com/google/guava:guava",
        "//3rdparty/jvm/commons_fileupload:commons_fileupload",
        "//3rdparty/jvm/javax/servlet:servlet_api",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_exp",
        ":finagle_http",
        ":finatra_jackson",
        ":finatra_utils",
        ":inject_server",
        ":inject_slf4j"
    ],
    jars = [
        "//external:jar/com/twitter/finatra_http_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finatra_httpclient",
    exports = [
        "//3rdparty/jvm/commons_codec:commons_codec",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_core",
        ":finagle_http",
        ":finatra_jackson",
        ":inject_utils",
        ":util_core"
    ],
    jars = [
        "//external:jar/com/twitter/finatra_httpclient_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finatra_jackson",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_databind",
        "//3rdparty/jvm/com/fasterxml/jackson/datatype:jackson_datatype_joda",
        "//3rdparty/jvm/com/fasterxml/jackson/module:jackson_module_scala_2_11",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/scala_lang:scalap",
        ":finagle_http",
        ":inject_utils",
        ":util_core"
    ],
    jars = [
        "//external:jar/com/twitter/finatra_jackson_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finatra_thrift",
    exports = [
        "//3rdparty/jvm/javax/inject:javax_inject",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/yaml:snakeyaml",
        ":finagle_core",
        ":finagle_exp",
        ":finagle_thrift",
        ":finagle_thriftmux_2_11",
        ":finatra_utils",
        ":inject_server",
        ":inject_slf4j",
        ":inject_thrift",
        ":util_core"
    ],
    jars = [
        "//external:jar/com/twitter/finatra_thrift_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "finatra_utils",
    exports = [
        "//3rdparty/jvm/com/github/nscala_time:nscala_time",
        "//3rdparty/jvm/com/google/inject:guice",
        "//3rdparty/jvm/commons_io:commons_io",
        "//3rdparty/jvm/joda_time:joda_time",
        ":finagle_http"
    ],
    jars = [
        "//external:jar/com/twitter/finatra_utils_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_app",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":inject_core",
        ":util_core"
    ],
    jars = [
        "//external:jar/com/twitter/inject_app_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_core",
    exports = [
        "//3rdparty/jvm/com/github/nscala_time:nscala_time",
        "//3rdparty/jvm/com/google/inject/extensions:guice_assistedinject",
        "//3rdparty/jvm/com/google/inject/extensions:guice_multibindings",
        "//3rdparty/jvm/com/google/inject:guice",
        "//3rdparty/jvm/commons_io:commons_io",
        "//3rdparty/jvm/javax/inject:javax_inject",
        "//3rdparty/jvm/joda_time:joda_time",
        "//3rdparty/jvm/net/codingwell:scala_guice_2_11",
        "//3rdparty/jvm/org/joda:joda_convert",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/scala_lang:scalap",
        ":inject_slf4j",
        ":util_app"
    ],
    jars = [
        "//external:jar/com/twitter/inject_core_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_modules",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_core",
        ":inject_core",
        ":util_slf4j_jul_bridge",
        ":util_stats"
    ],
    jars = [
        "//external:jar/com/twitter/inject_modules_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_server",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":finagle_stats",
        ":inject_app",
        ":inject_modules",
        ":inject_slf4j",
        ":inject_utils",
        ":twitter_server"
    ],
    jars = [
        "//external:jar/com/twitter/inject_server_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_slf4j",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_annotations",
        "//3rdparty/jvm/org/slf4j:jcl_over_slf4j",
        "//3rdparty/jvm/org/slf4j:jul_to_slf4j",
        "//3rdparty/jvm/org/slf4j:log4j_over_slf4j",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        ":finagle_core",
        ":util_slf4j_api"
    ],
    jars = [
        "//external:jar/com/twitter/inject_slf4j_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_thrift",
    exports = [
        "//3rdparty/jvm/org/apache/thrift:libthrift",
        ":finagle_mux_2_11",
        ":scrooge_core"
    ],
    jars = [
        "//external:jar/com/twitter/inject_thrift_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "inject_utils",
    exports = [
        "//3rdparty/jvm/commons_lang:commons_lang",
        ":finagle_core",
        ":inject_core"
    ],
    jars = [
        "//external:jar/com/twitter/inject_utils_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "scrooge_core",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":util_core"
    ],
    jars = [
        "//external:jar/com/twitter/scrooge_core_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "scrooge_generator",
    exports = [
        "//3rdparty/jvm/com/github/scopt:scopt_2_11",
        "//3rdparty/jvm/com/github/spullara/mustache/java:compiler",
        "//3rdparty/jvm/com/google/code/findbugs:jsr305",
        "//3rdparty/jvm/commons_cli:commons_cli",
        "//3rdparty/jvm/org/apache/thrift:libthrift",
        "//3rdparty/jvm/org/codehaus/plexus:plexus_utils",
        "//3rdparty/jvm/org/scala_lang/modules:scala_parser_combinators",
        "//3rdparty/jvm/org/scala_lang:scala_library"
    ],
    jars = [
        "//external:jar/com/twitter/scrooge_generator_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "twitter_server",
    exports = [
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_core",
        "//3rdparty/jvm/com/fasterxml/jackson/core:jackson_databind",
        "//3rdparty/jvm/com/fasterxml/jackson/module:jackson_module_scala_2_11",
        ":finagle_core",
        ":finagle_http",
        ":finagle_toggle_2_11",
        ":finagle_tunable_2_11",
        ":util_app",
        ":util_core",
        ":util_jvm",
        ":util_lint_2_11",
        ":util_registry_2_11",
        ":util_slf4j_api",
        ":util_slf4j_jul_bridge",
        ":util_tunable_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/twitter_server_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "twitter_server_logback_classic",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/slf4j:jcl_over_slf4j",
        "//3rdparty/jvm/org/slf4j:jul_to_slf4j",
        "//3rdparty/jvm/org/slf4j:log4j_over_slf4j",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        ":twitter_server"
    ],
    jars = [
        "//external:jar/com/twitter/twitter_server_logback_classic_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_app",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":util_core",
        ":util_registry_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/util_app_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_cache_2_11",
    exports = [
        "//external:jar/com/twitter/util_cache_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_codec",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":util_core"
    ],
    jars = [
        "//external:jar/com/twitter/util_codec_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_collection_2_11",
    exports = [
        "//external:jar/com/twitter/util_collection_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_core",
    exports = [
        "//3rdparty/jvm/org/scala_lang/modules:scala_parser_combinators",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/scala_lang:scala_reflect",
        ":util_function_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/util_core_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_function_2_11",
    exports = [
        "//external:jar/com/twitter/util_function_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_hashing_2_11",
    exports = [
        "//external:jar/com/twitter/util_hashing_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_jvm",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":util_app",
        ":util_core",
        ":util_stats"
    ],
    jars = [
        "//external:jar/com/twitter/util_jvm_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_lint_2_11",
    exports = [
        "//external:jar/com/twitter/util_lint_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_logging_2_11",
    exports = [
        "//external:jar/com/twitter/util_logging_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_registry_2_11",
    exports = [
        "//external:jar/com/twitter/util_registry_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_security_2_11",
    exports = [
        "//external:jar/com/twitter/util_security_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_slf4j_api",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/slf4j:slf4j_api"
    ],
    jars = [
        "//external:jar/com/twitter/util_slf4j_api_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_slf4j_jul_bridge",
    exports = [
        "//3rdparty/jvm/org/scala_lang:scala_library",
        "//3rdparty/jvm/org/slf4j:jul_to_slf4j",
        "//3rdparty/jvm/org/slf4j:slf4j_api",
        ":util_core",
        ":util_slf4j_api"
    ],
    jars = [
        "//external:jar/com/twitter/util_slf4j_jul_bridge_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



scala_import(
    name = "util_stats",
    exports = [
        "//3rdparty/jvm/com/github/ben_manes/caffeine:caffeine",
        "//3rdparty/jvm/com/google/code/findbugs:jsr305",
        "//3rdparty/jvm/org/scala_lang:scala_library",
        ":util_core",
        ":util_lint_2_11"
    ],
    jars = [
        "//external:jar/com/twitter/util_stats_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "util_tunable_2_11",
    exports = [
        "//external:jar/com/twitter/util_tunable_2_11"
    ],
    visibility = [
        "//visibility:public"
    ]
)


