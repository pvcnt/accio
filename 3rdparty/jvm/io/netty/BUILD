load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
java_library(
    name = "netty",
    exports = [
        "//external:jar/io/netty/netty"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_buffer",
    exports = [
        "//external:jar/io/netty/netty_buffer"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_codec",
    exports = [
        "//external:jar/io/netty/netty_codec"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_codec_http",
    exports = [
        "//external:jar/io/netty/netty_codec_http",
        ":netty_codec"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_codec_http2",
    exports = [
        "//external:jar/io/netty/netty_codec_http2"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_codec_socks",
    exports = [
        "//external:jar/io/netty/netty_codec_socks"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_common",
    exports = [
        "//external:jar/io/netty/netty_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_handler",
    exports = [
        "//external:jar/io/netty/netty_handler",
        ":netty_buffer",
        ":netty_codec"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_handler_proxy",
    exports = [
        "//external:jar/io/netty/netty_handler_proxy",
        ":netty_codec_http",
        ":netty_codec_socks"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_resolver",
    exports = [
        "//external:jar/io/netty/netty_resolver"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_tcnative_boringssl_static",
    exports = [
        "//external:jar/io/netty/netty_tcnative_boringssl_static"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_transport",
    exports = [
        "//external:jar/io/netty/netty_transport",
        ":netty_resolver"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_transport_native_epoll",
    exports = [
        "//external:jar/io/netty/netty_transport_native_epoll",
        ":netty_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "netty_transport_native_unix_common",
    exports = [
        "//external:jar/io/netty/netty_transport_native_unix_common"
    ],
    visibility = [
        "//visibility:public"
    ]
)


