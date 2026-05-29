package com.expenso.app.core.domain.upi

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiIntentBuilderTest {

    @Test
    fun appendAmountToRawUri_keepsSignedBytesIntact() {
        val src =
            "upi://pay?pa=merchant@hdfcbank&pn=Some%20Shop&tn=Order%2F123&tr=TXN9001&sign=AbCdEf123%2F%2B%3D&orgid=400009&mode=02"
        val out = UpiIntentBuilder.appendAmountToRawUri(src, BigDecimal("249.50"))
        assertTrue("output must preserve original signed bytes", out.startsWith(src))
        assertTrue(out.contains("&am=249.50"))
        assertTrue(out.contains("&cu=INR"))
    }

    @Test
    fun appendAmountToRawUri_skipsCurrencyWhenAlreadyPresent() {
        val src = "upi://pay?pa=foo@okicici&cu=INR&sign=ZZ"
        val out = UpiIntentBuilder.appendAmountToRawUri(src, BigDecimal("100.00"))
        assertEquals(1, out.split("cu=").size - 1)
        assertTrue(out.contains("&am=100.00"))
    }

    @Test
    fun appendAmountToRawUri_handlesUriWithoutQuery() {
        val src = "upi://pay"
        val out = UpiIntentBuilder.appendAmountToRawUri(src, BigDecimal("1.00"))
        assertEquals("upi://pay?am=1.00&cu=INR", out)
    }

    @Test
    fun appendAmountToRawUri_formatsAmountToTwoDecimalsHalfUp() {
        val src = "upi://pay?pa=x@y&cu=INR&sign=zz"
        val out = UpiIntentBuilder.appendAmountToRawUri(src, BigDecimal("12.345"))
        assertTrue(out.contains("&am=12.35"))
    }

    @Test
    fun normalizeBankHandle_lowercasesEntireVpa() {
        assertEquals("foo@okhdfc", UpiIntentBuilder.normalizeBankHandle("Foo@OKHDFC"))
        assertEquals("mixed@axisbank", UpiIntentBuilder.normalizeBankHandle("Mixed@AxisBank"))
    }

    @Test
    fun generateTxnRef_isAlphanumericAndBounded() {
        repeat(20) {
            val tr = UpiIntentBuilder.generateTxnRef()
            assertTrue(tr.length in 1..35)
            assertTrue(tr.all { it.isLetterOrDigit() })
        }
    }

    // Note: assertions on `buildUri` query-string output (e.g. ensuring `mode=`
    // is not emitted for P2P flows, but is forwarded from extraParams) require
    // android.net.Uri and need a Robolectric runtime. We cover that contract
    // by inspection in UpiIntentBuilder.kt and end-to-end via Timber logs of
    // outbound URIs in the Pay flow.
}
