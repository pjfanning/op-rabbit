package com.github.pjfanning.op_rabbit

import java.security.SecureRandom
import javax.net.ssl._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ConnectionParamsSslSpec extends AnyFunSpec with Matchers {

  private val defaultSocketFactory = SSLContext.getDefault.getSocketFactory

  // An SSLContext whose socket factory is a known instance, so we can tell whether it was applied
  private class StubSslContextSpi(socketFactory: SSLSocketFactory) extends SSLContextSpi {
    override def engineInit(km: Array[KeyManager], tm: Array[TrustManager], sr: SecureRandom): Unit = ()
    override def engineGetSocketFactory(): SSLSocketFactory = socketFactory
    override def engineGetServerSocketFactory(): SSLServerSocketFactory = throw new UnsupportedOperationException
    override def engineCreateSSLEngine(): SSLEngine = throw new UnsupportedOperationException
    override def engineCreateSSLEngine(host: String, port: Int): SSLEngine = throw new UnsupportedOperationException
    override def engineGetServerSessionContext(): SSLSessionContext = throw new UnsupportedOperationException
    override def engineGetClientSessionContext(): SSLSessionContext = throw new UnsupportedOperationException
  }
  private class StubSslContext(spi: SSLContextSpi) extends SSLContext(spi, null, "TLS")

  private def applied(params: ConnectionParams): ClusterConnectionFactory = {
    val factory = new ClusterConnectionFactory
    params.applyTo(factory)
    factory
  }

  describe("sslContext") {
    it("is used when ssl is enabled") {
      val context = new StubSslContext(new StubSslContextSpi(defaultSocketFactory))
      val factory = applied(ConnectionParams(ssl = true, sslContext = Some(context)))
      factory.isSSL shouldBe true
      factory.getSocketFactory should be theSameInstanceAs defaultSocketFactory
    }

    it("takes precedence over trustEverything") {
      val context = new StubSslContext(new StubSslContextSpi(defaultSocketFactory))
      val factory = applied(ConnectionParams(ssl = true, trustEverything = true, sslContext = Some(context)))
      factory.isSSL shouldBe true
      factory.getSocketFactory should be theSameInstanceAs defaultSocketFactory
    }

    it("is ignored when ssl is disabled") {
      val context = new StubSslContext(new StubSslContextSpi(defaultSocketFactory))
      val factory = applied(ConnectionParams(ssl = false, sslContext = Some(context)))
      factory.isSSL shouldBe false
      factory.getSocketFactory should not be theSameInstanceAs (defaultSocketFactory)
    }

    it("defaults to None, keeping the default trust manager behaviour") {
      ConnectionParams().sslContext shouldBe None
      applied(ConnectionParams(ssl = true)).isSSL shouldBe true
    }
  }
}
