/*
 * Copyright © 2011-2013 the spray project <http://spray.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.http

import org.specs2.mutable.Specification
import org.specs2.matcher.MatchResult
import parser.HttpParser
import CacheDirectives._
import HttpHeaders._
import MediaTypes._
import MediaRanges._
import HttpCharsets._
import HttpEncodings._
import HttpMethods._
import spray.util._

class HttpHeaderSpec extends Specification {
  val `application/vnd.spray` = MediaTypes.register(MediaType.custom("application/vnd.spray"))

  "The HTTP header model must correctly parse and render the following headers" >> {

    "Accept" in {
      "Accept: audio/midi;q=0.2, audio/basic" =!=
        Accept(`audio/midi` withQValue 0.2, `audio/basic`)
      "Accept: text/plain;q=0.5, text/html,\r\n text/css;q=0.8" =!=
        Accept(`text/plain` withQValue 0.5, `text/html`, `text/css` withQValue 0.8).renderedTo(
          "text/plain;q=0.5, text/html, text/css;q=0.8")
      "Accept: text/html, image/gif, image/jpeg, *;q=.2, */*;q=.2" =!=
        Accept(`text/html`, `image/gif`, `image/jpeg`, `*/*` withQValue 0.2, `*/*` withQValue 0.2).renderedTo(
          "text/html, image/gif, image/jpeg, */*;q=0.2, */*;q=0.2")
      "Accept: application/vnd.spray" =!=
        Accept(`application/vnd.spray`)
      "Accept: */*, text/*; foo=bar, custom/custom; bar=\"b>az\"" =!=
        Accept(`*/*`,
          MediaRange.custom("text", Map("foo" -> "bar")),
          MediaType.custom("custom", "custom", parameters = Map("bar" -> "b>az")))
      "Accept: application/*+xml; version=2" =!=
        Accept(MediaType.custom("application", "*+xml", parameters = Map("version" -> "2")))
    }

    "Accept-Charset" in {
      "Accept-Charset: *" =!= `Accept-Charset`(HttpCharsetRange.`*`)
      "Accept-Charset: UTF-8" =!= `Accept-Charset`(`UTF-8`)
      "Accept-Charset: utf16;q=1" =!= `Accept-Charset`(`UTF-16`).renderedTo("UTF-16")
      "Accept-Charset: utf-8; q=0.5, *" =!= `Accept-Charset`(`UTF-8` withQValue 0.5, HttpCharsetRange.`*`).renderedTo("UTF-8;q=0.5, *")
      "Accept-Charset: latin1, UTf-16; q=0, *;q=0.8" =!=
        `Accept-Charset`(`ISO-8859-1`, `UTF-16` withQValue 0, HttpCharsetRange.`*` withQValue 0.8).renderedTo(
          "ISO-8859-1, UTF-16;q=0.0, *;q=0.8")
    }

    "Accept-Ranges" in {
      "Accept-Ranges: bytes" =!= `Accept-Ranges`(RangeUnit.Bytes)
      "Accept-Ranges: bytes, sausages" =!= `Accept-Ranges`(RangeUnit.Bytes, RangeUnit.Other("sausages"))
      "Accept-Ranges: none" =!= `Accept-Ranges`(Nil)
    }

    "Accept-Encoding" in {
      "Accept-Encoding: compress, gzip, fancy" =!=
        `Accept-Encoding`(compress, gzip, HttpEncoding.custom("fancy"))
      "Accept-Encoding: gzip, identity;q=0.5, *;q=0.0" =!=
        `Accept-Encoding`(gzip, identity withQValue 0.5, HttpEncodingRange.`*` withQValue 0)
        .renderedTo("gzip, identity;q=0.5, *;q=0.0")
      "Accept-Encoding: " =!= `Accept-Encoding`(identity).renderedTo("identity")
    }

    "Accept-Language" in {
      "Accept-Language: da, en-gb;q=0.8, en;q=0.7" =!=
        `Accept-Language`(Language("da"), Language("en", "gb") withQValue 0.8f, Language("en") withQValue 0.7f)
      "Accept-Language: de-CH-1901, *;q=0.0" =!=
        `Accept-Language`(Language("de", "CH", "1901"), LanguageRanges.`*` withQValue 0f)
      "Accept-Language: es-419, es" =!= `Accept-Language`(Language("es", "419"), Language("es"))
    }

    "Access-Control-Allow-Credentials" in {
      "Access-Control-Allow-Credentials: true" =!= `Access-Control-Allow-Credentials`(allow = true)
    }

    "Access-Control-Allow-Headers" in {
      "Access-Control-Allow-Headers: Accept, X-My-Header" =!= `Access-Control-Allow-Headers`("Accept", "X-My-Header")
    }

    "Access-Control-Allow-Methods" in {
      "Access-Control-Allow-Methods: GET, POST" =!= `Access-Control-Allow-Methods`(GET, POST)
    }

    "Access-Control-Allow-Origin" in {
      "Access-Control-Allow-Origin: *" =!= `Access-Control-Allow-Origin`(AllOrigins)
      "Access-Control-Allow-Origin: null" =!= `Access-Control-Allow-Origin`(SomeOrigins(Nil))
      "Access-Control-Allow-Origin: http://spray.io" =!= `Access-Control-Allow-Origin`(SomeOrigins(Seq("http://spray.io")))
    }

    "Access-Control-Expose-Headers" in {
      "Access-Control-Expose-Headers: Accept, X-My-Header" =!= `Access-Control-Expose-Headers`("Accept", "X-My-Header")
    }

    "Access-Control-Max-Age" in {
      "Access-Control-Max-Age: 3600" =!= `Access-Control-Max-Age`(3600)
    }

    "Access-Control-Request-Headers" in {
      "Access-Control-Request-Headers: Accept, X-My-Header" =!= `Access-Control-Request-Headers`("Accept", "X-My-Header")
    }

    "Access-Control-Request-Method" in {
      "Access-Control-Request-Method: POST" =!= `Access-Control-Request-Method`(POST)
    }

    "Allow" in {
      "Allow: " =!= Allow()
      "Allow: GET, PUT" =!= Allow(GET, PUT)
    }

    "Authorization" in {
      "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==" =!=
        Authorization(BasicHttpCredentials("Aladdin", "open sesame"))
      "Authorization: bAsIc QWxhZGRpbjpvcGVuIHNlc2FtZQ==" =!=
        Authorization(BasicHttpCredentials("Aladdin", "open sesame")).renderedTo(
          "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")
      """Authorization: Fancy yes="n:o", nonce=42""" =!=
        Authorization(GenericHttpCredentials("Fancy", Map("yes" -> "n:o", "nonce" -> "42"))).renderedTo(
          """Fancy yes="n:o",nonce=42""")
      """Authorization: Fancy yes=no,nonce="4\\2"""" =!=
        Authorization(GenericHttpCredentials("Fancy", Map("yes" -> "no", "nonce" -> """4\2""")))
      "Authorization: Basic Qm9iOg==" =!=
        Authorization(BasicHttpCredentials("Bob", ""))
      """Authorization: Digest name=Bob""" =!=
        Authorization(GenericHttpCredentials("Digest", Map("name" -> "Bob")))
      """Authorization: Bearer mF_9.B5f-4.1JqM/""" =!=
        Authorization(OAuth2BearerToken("mF_9.B5f-4.1JqM/"))
      "Authorization: NoParamScheme" =!=
        Authorization(GenericHttpCredentials("NoParamScheme", Map.empty[String, String]))
      "Authorization: OAuth sf_v1a-stg;V5DrRS1KfA=" =!=
        Authorization(GenericHttpCredentials("OAuth", "sf_v1a-stg;V5DrRS1KfA="))
    }

    "Cache-Control" in {
      "Cache-Control: no-cache, max-age=0" =!=
        `Cache-Control`(`no-cache`, `max-age`(0))
      "Cache-Control: private=\"Some-Field\"" =!=
        `Cache-Control`(`private`("Some-Field"))
      "Cache-Control: private, community=\"<UCI>\"" =!=
        `Cache-Control`(`private`(), CacheDirective.custom("community", Some("<UCI>")))
    }

    "Connection" in {
      "Connection: close" =!= Connection("close")
      "Connection: pipapo, close" =!= Connection("pipapo", "close")
    }

    "Content-Disposition" in {
      "Content-Disposition: form-data" =!= `Content-Disposition`("form-data")
      "Content-Disposition: attachment; name=field1; filename=\"file/txt\"" =!=
        `Content-Disposition`("attachment", Map("name" -> "field1", "filename" -> "file/txt"))
    }

    "Content-Encoding" in {
      "Content-Encoding: gzip" =!= `Content-Encoding`(gzip)
      "Content-Encoding: pipapo" =!= `Content-Encoding`(HttpEncoding.custom("pipapo"))
    }

    "Content-Length" in {
      "Content-Length: 42" =!= `Content-Length`(42)
    }

    "Content-Type" in {
      "Content-Type: application/pdf" =!=
        `Content-Type`(`application/pdf`)
      "Content-Type: text/plain; charset=utf8" =!=
        `Content-Type`(ContentType(`text/plain`, `UTF-8`)).renderedTo("text/plain; charset=UTF-8")
      "Content-Type: text/xml; version=3; charset=windows-1252" =!=
        `Content-Type`(ContentType(MediaType.custom("text", "xml", parameters = Map("version" -> "3")), HttpCharsets.getForKey("windows-1252")))
      "Content-Type: text/plain; charset=fancy-pants" =!=
        ErrorInfo("Illegal HTTP header 'Content-Type': Unsupported charset", "fancy-pants")
      "Content-Type: multipart/mixed; boundary=ABC123" =!=
        `Content-Type`(ContentType(`multipart/mixed` withBoundary "ABC123"))
      "Content-Type: multipart/mixed; boundary=\"ABC/123\"" =!=
        `Content-Type`(ContentType(`multipart/mixed` withBoundary "ABC/123"))
      "Content-Type: application/*" =!=
        `Content-Type`(ContentType(MediaType.custom("application", "*", allowArbitrarySubtypes = true)))
    }

    "Content-Range" in {
      "Content-Range: bytes 0-9/10" =!= `Content-Range`(ContentRange(0, 9, 10))
      "Content-Range: bytes 1-42/*" =!= `Content-Range`(ContentRange(1, 42))
      "Content-Range: bytes */*" =!= `Content-Range`(ContentRange.Unsatisfiable)
    }

    "Cookie" in {
      "Cookie: SID=31d4d96e407aad42" =!= Cookie(HttpCookie("SID", "31d4d96e407aad42"))
      "Cookie: SID=31d4d96e407aad42; lang=en>US" =!= Cookie(HttpCookie("SID", "31d4d96e407aad42"), HttpCookie("lang", "en>US"))
      "Cookie: a=1;b=2" =!= Cookie(HttpCookie("a", "1"), HttpCookie("b", "2")).renderedTo("a=1; b=2")
      "Cookie: a=1 ;b=2" =!= Cookie(HttpCookie("a", "1"), HttpCookie("b", "2")).renderedTo("a=1; b=2")
      "Cookie: a=1; b=2" =!= Cookie(HttpCookie("a", "1"), HttpCookie("b", "2"))
      "Cookie: a=1,b=2" =!= Cookie(HttpCookie("a", "1"), HttpCookie("b", "2")).renderedTo("a=1; b=2")
      Cookie(HttpCookie("SID", "31d4d96e407aad42",
        domain = Some("example.com"),
        expires = Some(DateTime(2021, 6, 9, 10, 18, 14)),
        path = Some("/hello"),
        httpOnly = true,
        extension = Some("fancyPants"),
        secure = true)).toString === "Cookie: SID=31d4d96e407aad42"
    }

    "Date" in {
      "Date: Wed, 13 Jul 2011 08:12:31 GMT" =!= Date(DateTime(2011, 7, 13, 8, 12, 31))
      "Date: Fri, 23 Mar 1804 12:11:10 UTC" =!= Date(DateTime(1804, 3, 23, 12, 11, 10)).renderedTo(
        "Fri, 23 Mar 1804 12:11:10 GMT")
    }

    "ETag" in {
      """ETag: "938fz3f83z3z38z"""" =!= ETag("938fz3f83z3z38z", weak = false)
      """ETag: W/"938fz3f83z3z38z"""" =!= ETag("938fz3f83z3z38z", weak = true)
    }

    "Expect" in {
      "Expect: 100-continue" =!= Expect("100-continue")
    }

    "Host" in {
      "Host: www.spray.io:8080" =!= Host("www.spray.io", 8080)
      "Host: spray.io" =!= Host("spray.io")
      "Host: [2001:db8::1]:8080" =!= Host("[2001:db8::1]", 8080)
      "Host: [2001:db8::1]" =!= Host("[2001:db8::1]")
      "Host: [::FFFF:129.144.52.38]" =!= Host("[::FFFF:129.144.52.38]")
    }

    "If-Match" in {
      """If-Match: *""" =!= `If-Match`.`*`
      """If-Match: "938fz3f83z3z38z"""" =!= `If-Match`(EntityTag("938fz3f83z3z38z"))
      """If-Match: "938fz3f83z3z38z", "0293f34hhv0nc"""" =!=
        `If-Match`(EntityTag("938fz3f83z3z38z"), EntityTag("0293f34hhv0nc"))
    }

    "If-Modified-Since" in {
      "If-Modified-Since: Wed, 13 Jul 2011 08:12:31 GMT" =!= `If-Modified-Since`(DateTime(2011, 7, 13, 8, 12, 31))
    }

    "If-None-Match" in {
      """If-None-Match: *""" =!= `If-None-Match`.`*`
      """If-None-Match: "938fz3f83z3z38z"""" =!= `If-None-Match`(EntityTag("938fz3f83z3z38z"))
      """If-None-Match: "938fz3f83z3z38z", "0293f34hhv0nc"""" =!=
        `If-None-Match`(EntityTag("938fz3f83z3z38z"), EntityTag("0293f34hhv0nc"))
      """If-None-Match: W/"938fz3f83z3z38z"""" =!= `If-None-Match`(EntityTag("938fz3f83z3z38z", weak = true))
    }

    "If-Range" in {
      """If-Range: "abcdefg"""" =!= `If-Range`(Left(EntityTag("abcdefg")))
      """If-Range: Wed, 13 Jul 2011 08:12:31 GMT""" =!= `If-Range`(Right(DateTime(2011, 7, 13, 8, 12, 31)))
    }

    "If-Unmodified-Since" in {
      "If-Unmodified-Since: Wed, 13 Jul 2011 08:12:31 GMT" =!= `If-Unmodified-Since`(DateTime(2011, 7, 13, 8, 12, 31))
    }

    "Last-Modified" in {
      "Last-Modified: Wed, 13 Jul 2011 08:12:31 GMT" =!= `Last-Modified`(DateTime(2011, 7, 13, 8, 12, 31))
    }

    "Location" in {
      "Location: https://spray.io/secure" =!= Location(Uri("https://spray.io/secure"))
      "Location: /en-us/default.aspx" =!= Location(Uri("/en-us/default.aspx"))
      "Location: https://spray.io/{sec}" =!= Location(Uri("https://spray.io/{sec}")).renderedTo(
        "https://spray.io/%7Bsec%7D")
      "Location: https://spray.io/ sec" =!= ErrorInfo("Illegal HTTP header 'Location': Illegal URI " +
        "reference, unexpected character ' ' at position 17", "\nhttps://spray.io/ sec\n                 ^\n")
    }

    "Link" in {
      "Link: </?page=2>; rel=next" =!= Link(Uri("/?page=2"), Link.next)
      "Link: <https://spray.io>; rel=next" =!= Link(Uri("https://spray.io"), Link.next)
      """Link: </>; rel=prev, </page/2>; rel="next"""" =!=
        Link(Link.Value(Uri("/"), Link.prev), Link.Value(Uri("/page/2"), Link.next)).renderedTo("</>; rel=prev, </page/2>; rel=next")

      """Link: </>; rel="x.y-z http://spray.io"""" =!= Link(Uri("/"), Link.rel("x.y-z http://spray.io"))
      """Link: </>; title="My Title"""" =!= Link(Uri("/"), Link.title("My Title"))
      """Link: </>; rel=next; title="My Title"""" =!= Link(Uri("/"), Link.next, Link.title("My Title"))
      """Link: </>; anchor="http://example.com"""" =!= Link(Uri("/"), Link.anchor(Uri("http://example.com")))
      """Link: </>; rev=foo; hreflang=de-de; media=print; type=application/json""" =!=
        Link(Uri("/"), Link.rev("foo"), Link.hreflang(Language("de", "de")), Link.media("print"), Link.`type`(`application/json`))

      /* RFC 5988 examples */
      """Link: <http://example.com/TheBook/chapter2>; rel="previous"; title="previous chapter"""" =!=
        Link(Uri("http://example.com/TheBook/chapter2"), Link.rel("previous"), Link.title("previous chapter"))
        .renderedTo("""<http://example.com/TheBook/chapter2>; rel=previous; title="previous chapter"""")

      """Link: </>; rel="http://example.net/foo"""" =!= Link(Uri("/"), Link.rel("http://example.net/foo"))
        .renderedTo("</>; rel=http://example.net/foo")

      """Link: <http://example.org/>; rel="start http://example.net/relation/other"""" =!= Link(Uri("http://example.org/"),
        Link.rel("start http://example.net/relation/other"))

      // only one 'rel=' is allowed, http://tools.ietf.org/html/rfc5988#section-5.3 requires any subsequent ones to be skipped
      "Link: </>; rel=prev; rel=next" =!=> "</>; rel=prev"
    }

    "Origin" in {
      "Origin: http://spray.io" =!= Origin(Seq("http://spray.io"))
    }

    "Proxy-Authenticate" in {
      "Proxy-Authenticate: Basic realm=WallyWorld,attr=\"val>ue\", Fancy realm=yeah" =!=
        `Proxy-Authenticate`(HttpChallenge("Basic", "WallyWorld", Map("attr" -> "val>ue")), HttpChallenge("Fancy", "yeah"))
    }

    "Proxy-Authorization" in {
      """Proxy-Authorization: Fancy yes=no,nonce="4\\2"""" =!=
        `Proxy-Authorization`(GenericHttpCredentials("Fancy", Map("yes" -> "no", "nonce" -> """4\2""")))
    }

    "Remote-Address" in {
      "Remote-Address: 111.22.3.4" =!= `Remote-Address`("111.22.3.4")
    }

    "Server" in {
      "Server: as fghf.fdf/xx" =!= `Server`(Seq(ProductVersion("as"), ProductVersion("fghf.fdf", "xx")))
    }

    "Transfer-Encoding" in {
      "Transfer-Encoding: chunked" =!= `Transfer-Encoding`("chunked")
    }

    "Range" in {
      "Range: bytes=0-1" =!= Range(ByteRange(0, 1))
      "Range: bytes=0-" =!= Range(ByteRange.fromOffset(0))
      "Range: bytes=-1" =!= Range(ByteRange.suffix(1))
      "Range: bytes=0-1, 2-3, -99" =!= Range(ByteRange(0, 1), ByteRange(2, 3), ByteRange.suffix(99))
    }

    "Set-Cookie" in {
      "Set-Cookie: SID=\"31d4d96e407aad42\"" =!=
        `Set-Cookie`(HttpCookie("SID", "31d4d96e407aad42")).renderedTo("SID=31d4d96e407aad42")
      "Set-Cookie: SID=31d4d96e407aad42; Domain=example.com; Path=/" =!=
        `Set-Cookie`(HttpCookie("SID", "31d4d96e407aad42", path = Some("/"), domain = Some("example.com")))
      "Set-Cookie: lang=en-US; Expires=Wed, 09 Jun 2021 10:18:14 GMT; Path=/hello" =!=
        `Set-Cookie`(HttpCookie("lang", "en-US", expires = Some(DateTime(2021, 6, 9, 10, 18, 14)), path = Some("/hello")))
      "Set-Cookie: name=123; Max-Age=12345; Secure" =!=
        `Set-Cookie`(HttpCookie("name", "123", maxAge = Some(12345), secure = true))
      "Set-Cookie: name=123; HttpOnly; fancyPants" =!=
        `Set-Cookie`(HttpCookie("name", "123", httpOnly = true, extension = Some("fancyPants")))
      "Set-Cookie: foo=bar; domain=example.com; Path=/this is a path with blanks; extension with blanks" =!=
        `Set-Cookie`(HttpCookie("foo", "bar", domain = Some("example.com"), path = Some("/this is a path with blanks"),
          extension = Some("extension with blanks"))).renderedTo(
          "foo=bar; Domain=example.com; Path=/this is a path with blanks; extension with blanks")
    }

    "User-Agent" in {
      "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_3) AppleWebKit/537.31" =!=
        `User-Agent`(ProductVersion("Mozilla", "5.0", "Macintosh; Intel Mac OS X 10_8_3"), ProductVersion("AppleWebKit", "537.31"))
      "User-Agent: foo(bar)(baz)" =!=
        `User-Agent`(ProductVersion("foo", "", "bar"), ProductVersion(comment = "baz")).renderedTo("foo (bar) (baz)")
    }

    "WWW-Authenticate" in {
      "WWW-Authenticate: Basic realm=WallyWorld" =!=
        `WWW-Authenticate`(HttpChallenge("Basic", "WallyWorld"))
      "WWW-Authenticate: BaSiC rEaLm=WallyWorld" =!=
        `WWW-Authenticate`(HttpChallenge("BaSiC", "WallyWorld")).renderedTo("BaSiC realm=WallyWorld")
      "WWW-Authenticate: Basic realm=\"foo<bar\"" =!= `WWW-Authenticate`(HttpChallenge("Basic", "foo<bar"))
      """WWW-Authenticate: Digest
                           realm="testrealm@host.com",
                           qop="auth,auth-int",
                           nonce=dcd98b7102dd2f0e8b11d0f600bfb0c093,
                           opaque=5ccc069c403ebaf9f0171e9517f40e41""".stripMarginWithNewline("\r\n") =!=
        `WWW-Authenticate`(HttpChallenge("Digest", "testrealm@host.com", Map("qop" -> "auth,auth-int",
          "nonce" -> "dcd98b7102dd2f0e8b11d0f600bfb0c093", "opaque" -> "5ccc069c403ebaf9f0171e9517f40e41"))).renderedTo(
          "Digest realm=\"testrealm@host.com\",qop=\"auth,auth-int\",nonce=dcd98b7102dd2f0e8b11d0f600bfb0c093,opaque=5ccc069c403ebaf9f0171e9517f40e41")
      "WWW-Authenticate: Basic realm=WallyWorld,attr=\"val>ue\", Fancy realm=yeah" =!=
        `WWW-Authenticate`(HttpChallenge("Basic", "WallyWorld", Map("attr" -> "val>ue")), HttpChallenge("Fancy", "yeah"))
      """WWW-Authenticate: Fancy realm="Secure Area",nonce=42""" =!=
        `WWW-Authenticate`(HttpChallenge("Fancy", "Secure Area", Map("nonce" -> "42")))
    }

    "X-Forwarded-For" in {
      "X-Forwarded-For: 1.2.3.4" =!= `X-Forwarded-For`("1.2.3.4")
      "X-Forwarded-For: 234.123.5.6, 8.8.8.8" =!= `X-Forwarded-For`("234.123.5.6", "8.8.8.8")
      "X-Forwarded-For: 1.2.3.4, unknown" =!= `X-Forwarded-For`(RemoteAddress("1.2.3.4"), RemoteAddress.Unknown)
      "X-Forwarded-For: 192.0.2.43, 2001:db8:cafe:0:0:0:0:17" =!= `X-Forwarded-For`("192.0.2.43", "2001:db8:cafe::17")
      "X-Forwarded-For: 1234:5678:9abc:def1:2345:6789:abcd:ef00" =!= `X-Forwarded-For`("1234:5678:9abc:def1:2345:6789:abcd:ef00")
      "X-Forwarded-For: 1234:567:9a:d:2:67:abc:ef00" =!= `X-Forwarded-For`("1234:567:9a:d:2:67:abc:ef00")
      "X-Forwarded-For: 2001:db8:85a3::8a2e:370:7334" =!=> "2001:db8:85a3:0:0:8a2e:370:7334"
      "X-Forwarded-For: 1:2:3:4:5:6:7:8" =!= `X-Forwarded-For`("1:2:3:4:5:6:7:8")
      "X-Forwarded-For: ::2:3:4:5:6:7:8" =!=> "0:2:3:4:5:6:7:8"
      "X-Forwarded-For: ::3:4:5:6:7:8" =!=> "0:0:3:4:5:6:7:8"
      "X-Forwarded-For: ::4:5:6:7:8" =!=> "0:0:0:4:5:6:7:8"
      "X-Forwarded-For: ::5:6:7:8" =!=> "0:0:0:0:5:6:7:8"
      "X-Forwarded-For: ::6:7:8" =!=> "0:0:0:0:0:6:7:8"
      "X-Forwarded-For: ::7:8" =!=> "0:0:0:0:0:0:7:8"
      "X-Forwarded-For: ::8" =!=> "0:0:0:0:0:0:0:8"
      "X-Forwarded-For: 1:2:3:4:5:6:7::" =!=> "1:2:3:4:5:6:7:0"
      "X-Forwarded-For: 1:2:3:4:5:6::" =!=> "1:2:3:4:5:6:0:0"
      "X-Forwarded-For: 1:2:3:4:5::" =!=> "1:2:3:4:5:0:0:0"
      "X-Forwarded-For: 1:2:3:4::" =!=> "1:2:3:4:0:0:0:0"
      "X-Forwarded-For: 1:2:3::" =!=> "1:2:3:0:0:0:0:0"
      "X-Forwarded-For: 1:2::" =!=> "1:2:0:0:0:0:0:0"
      "X-Forwarded-For: 1::" =!=> "1:0:0:0:0:0:0:0"
      "X-Forwarded-For: 1::3:4:5:6:7:8" =!=> "1:0:3:4:5:6:7:8"
      "X-Forwarded-For: 1:2::4:5:6:7:8" =!=> "1:2:0:4:5:6:7:8"
      "X-Forwarded-For: 1:2:3::5:6:7:8" =!=> "1:2:3:0:5:6:7:8"
      "X-Forwarded-For: 1:2:3:4::6:7:8" =!=> "1:2:3:4:0:6:7:8"
      "X-Forwarded-For: 1:2:3:4:5::7:8" =!=> "1:2:3:4:5:0:7:8"
      "X-Forwarded-For: 1:2:3:4:5:6::8" =!=> "1:2:3:4:5:6:0:8"
      "X-Forwarded-For: ::" =!=> "0:0:0:0:0:0:0:0"
    }

    "RawHeader" in {
      "X-Space-Ranger: no, this rock!" =!= RawHeader("X-Space-Ranger", "no, this rock!")
    }
  }

  implicit class TestLine(line: String) {
    def =!=(testHeader: TestExample) = testHeader(line)
    def =!=>(expectedRendering: String): MatchResult[Any] = {
      val Array(name, value) = line.split(": ", 2)
      val Right(header) = HttpParser.parseHeader(RawHeader(name, value))
      header.toString === {
        header match {
          case x: ModeledHeader ⇒ x.name + ": " + expectedRendering
          case _                ⇒ expectedRendering
        }
      }
    }
  }
  sealed trait TestExample extends (String ⇒ MatchResult[Any])
  implicit class TestHeader(header: HttpHeader) extends TestExample {
    def apply(line: String) = {
      val Array(name, value) = line.split(": ", 2)
      val h = HttpParser.parseHeader(RawHeader(name, value))
      if (h.isRight && h.right.get.isInstanceOf[RawHeader] && !header.isInstanceOf[RawHeader])
        failure(s"`$line` was not parsed into a ModeledHeader")
      h === Right(header) and rendersTo(line)
    }
    protected def rendersTo(line: String) = header.toString === line
    def renderedTo(expectedRendering: String): TestHeader =
      new TestHeader(header) {
        override protected def rendersTo(line: String) =
          header.toString === {
            header match {
              case x: ModeledHeader ⇒ x.name + ": " + expectedRendering
              case _                ⇒ expectedRendering
            }
          }
      }
  }
  implicit class TestError(expectedError: ErrorInfo) extends TestExample {
    def apply(line: String) = {
      val Array(name, value) = line.split(": ", 2)
      HttpParser.parseHeader(RawHeader(name, value)) === Left(expectedError)
    }
  }
}
