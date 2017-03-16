package fr.cnrs.liris.accio.client.client

import com.twitter.finatra.json.FinatraObjectMapper

class ObjectMapperFactory {
  def create(): FinatraObjectMapper = {
    FinatraObjectMapper.create()
  }
}
