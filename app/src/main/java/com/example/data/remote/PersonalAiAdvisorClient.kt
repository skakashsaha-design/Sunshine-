package com.example.data.remote

import com.example.domain.model.PersonalApiKeys
import org.json.JSONArray
import org.json.JSONObject

/**
 * Optional Personal AI API Add-on.
 * Works completely without an API key (uses local deterministic Daily Safety & Weather Advisor).
 * Only sends a request if the user explicitly enables it and provides their own personal endpoint/key.
 */
class PersonalAiAdvisorClient(private val client: NetworkClient) {

    suspend fun generateOptionalPersonalBrief(
        config: PersonalApiKeys,
        contextSummary: String
    ): String? {
        if (!config.personalAiEnabled || config.personalAiKey.isBlank()) return null
        val endpoint = config.personalAiEndpoint.ifBlank {
            "https://api.openai.com/v1/chat/completions"
        }
        val payload = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.3)
            put("max_tokens", 180)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a concise meteorological safety advisor. Give 2 practical sentences based ONLY on the provided data.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contextSummary)
                    })
                }
            )
        }.toString()

        val res = client.postJson(
            url = endpoint,
            jsonBody = payload,
            headers = mapOf("Authorization" to "Bearer ${config.personalAiKey.trim()}")
        )
        return res.mapCatching { body ->
            val root = JSONObject(body)
            root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
        }.getOrNull()
    }
}
