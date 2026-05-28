package com.expenso.app.core.domain.upi

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiUriParserTest {

    @Test
    fun parsesStaticQrWithoutAmount() {
        val r = UpiUriParser.parse("upi://pay?pa=chai@okicici&pn=Chai%20Tapri")
        assertTrue(r is UpiParseResult.Success)
        val req = (r as UpiParseResult.Success).request
        assertEquals("chai@okicici", req.payeeVpa)
        assertEquals("Chai Tapri", req.payeeName)
        assertNull(req.amountRupees)
        assertEquals("INR", req.currency)
        assertFalse(req.isSigned)
    }

    @Test
    fun parsesDynamicQrWithSignedParams() {
        val url =
            "upi://pay?pa=m.abc@paytm&pn=Merchant&am=249.50&cu=INR&tn=Order%2FXYZ&tr=TXN9001&sign=abc&orgid=400009&mode=02"
        val r = UpiUriParser.parse(url)
        assertTrue(r is UpiParseResult.Success)
        val req = (r as UpiParseResult.Success).request
        assertEquals(BigDecimal("249.50"), req.amountRupees)
        assertEquals("Order/XYZ", req.transactionNote)
        assertEquals("TXN9001", req.transactionRef)
        assertTrue(req.isSigned)
        assertEquals("400009", req.rawParams["orgid"])
    }

    @Test
    fun rejectsNonUpiUri() {
        val r = UpiUriParser.parse("https://example.com/pay?x=1")
        assertTrue(r is UpiParseResult.Failure)
    }

    @Test
    fun rejectsMissingVpa() {
        val r = UpiUriParser.parse("upi://pay?pn=Foo&am=1")
        assertTrue(r is UpiParseResult.Failure)
    }

    @Test
    fun rejectsInvalidVpa() {
        val r = UpiUriParser.parse("upi://pay?pa=notavpa&pn=Foo")
        assertTrue(r is UpiParseResult.Failure)
    }

    @Test
    fun normalisesVpaCaseAndSanitisesName() {
        val r = UpiUriParser.parse("upi://pay?pa=MixedCase@okAXIS&pn=Shop%09%00Name")
        assertTrue(r is UpiParseResult.Success)
        val req = (r as UpiParseResult.Success).request
        assertEquals("mixedcase@okaxis", req.payeeVpa)
        assertEquals("ShopName", req.payeeName)
    }

    @Test
    fun ignoresUpiUnsupportedActions() {
        val r = UpiUriParser.parse("upi://mandate?pa=foo@bar")
        assertTrue(r is UpiParseResult.Failure)
    }

    @Test
    fun rejectsEmpty() {
        assertTrue(UpiUriParser.parse(null) is UpiParseResult.Failure)
        assertTrue(UpiUriParser.parse("") is UpiParseResult.Failure)
    }
}
