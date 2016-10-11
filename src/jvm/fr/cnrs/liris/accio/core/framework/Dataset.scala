package fr.cnrs.liris.accio.core.framework

import com.fasterxml.jackson.annotation.JsonProperty

case class Dataset(uri: String, @JsonProperty("type") kind: String)
