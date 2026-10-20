package com.pwdog.core.entry

import java.security.SecureRandom

data class PasswordGeneratorOptions(
    val length: Int = 20,
    val lowercase: Boolean = true,
    val uppercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
) {
    init {
        require(length >= 8) { "密码长度至少为 8" }
        require(lowercase || uppercase || digits || symbols) { "至少选择一种字符类型" }
    }
}

object PasswordGenerator {
    private const val LOWERCASE = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?"
    private val random = SecureRandom()

    fun generate(options: PasswordGeneratorOptions = PasswordGeneratorOptions()): String {
        val characterSets = buildList {
            if (options.lowercase) add(LOWERCASE)
            if (options.uppercase) add(UPPERCASE)
            if (options.digits) add(DIGITS)
            if (options.symbols) add(SYMBOLS)
        }
        require(options.length >= characterSets.size) {
            "密码长度不足以包含所有选中的字符类型"
        }

        val allCharacters = characterSets.joinToString("")
        val password = CharArray(options.length)

        characterSets.forEachIndexed { index, characters ->
            password[index] = characters[random.nextInt(characters.length)]
        }
        for (index in characterSets.size until password.size) {
            password[index] = allCharacters[random.nextInt(allCharacters.length)]
        }
        for (index in password.lastIndex downTo 1) {
            val swapWith = random.nextInt(index + 1)
            val temporary = password[index]
            password[index] = password[swapWith]
            password[swapWith] = temporary
        }

        return String(password)
    }
}
