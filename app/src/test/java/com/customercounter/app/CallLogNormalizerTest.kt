package com.customercounter.app

import com.customercounter.app.calllog.CallLogNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class CallLogNormalizerTest {
    @Test fun jordanFormatsResolveToSameCanonicalNumber() {
        val local = CallLogNormalizer.normalize("0796020522")
        val international = CallLogNormalizer.normalize("+962796020522")
        val international00 = CallLogNormalizer.normalize("00962796020522")
        assertEquals(local, international)
        assertEquals(local, international00)
    }

    @Test fun invalidNumberReturnsNullOrSafeFallback() {
        val result = CallLogNormalizer.normalize("unknown")
        assertEquals(null, result)
    }
}
