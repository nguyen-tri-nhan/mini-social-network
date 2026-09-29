package com.nhan.social.auth.dto

import jakarta.validation.Validation
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SignUpRequestValidationTest {

    private val validator = Validation.byDefaultProvider().configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory().validator

    private val valid = SignUpRequest("nhan", "nhan@example.com", "secret1", "Nhan", "Nguyen")

    private fun violatedFields(request: SignUpRequest) =
        validator.validate(request).map { it.propertyPath.toString() }.toSet()

    @Test
    fun `valid request has no violations`() {
        assertTrue(violatedFields(valid).isEmpty())
    }

    @Test
    fun `firstname and lastname longer than user_profile column are rejected`() {
        val tooLong = "a".repeat(101)
        assertEquals(setOf("firstname", "lastname"), violatedFields(valid.copy(firstname = tooLong, lastname = tooLong)))
    }

    @Test
    fun `username longer than 50 is rejected`() {
        assertEquals(setOf("username"), violatedFields(valid.copy(username = "u".repeat(51))))
    }

    @Test
    fun `boundary lengths are accepted`() {
        val atLimit = valid.copy(username = "u".repeat(50), firstname = "a".repeat(100), lastname = "b".repeat(100))
        assertTrue(violatedFields(atLimit).isEmpty())
    }
}
