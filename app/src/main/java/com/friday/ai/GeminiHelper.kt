package com.friday.ai

class GeminiHelper {

    fun getReply(userInput: String): String {
        val text = userInput.lowercase()

        return when {
            text.contains("hello") ->
                "Hello Boss, I am FriDay."

            text.contains("who are you") ->
                "I am FriDay, your personal AI assistant."

            text.contains("what can you do") ->
                "I can listen to your voice and control your phone."

            else ->
                "I understood your command."
        }
    }
}
