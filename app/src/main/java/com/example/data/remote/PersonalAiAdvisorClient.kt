package com.example.data.remote

import com.example.domain.model.AiProviderType
import com.example.domain.model.PersonalApiKeys
import org.json.JSONArray
import org.json.JSONObject

/**
 * Optional Personal AI API Add-on supporting ChatGPT (OpenAI), Gemini, OpenRouter, Groq, Claude, DeepSeek, and Custom Endpoints.
 * Works completely without an API key (uses local deterministic Daily Safety & Weather Advisor).
 */
class PersonalAiAdvisorClient(private val client: NetworkClient) {

    suspend fun generateOptionalPersonalBrief(
        config: PersonalApiKeys,
        contextSummary: String
    ): String? {
        if (!config.personalAiEnabled || config.personalAiKey.isBlank()) return null

        val provider = config.aiProvider
        val endpoint = config.personalAiEndpoint.ifBlank { provider.defaultEndpoint }
        val modelName = provider.defaultModel

        return when (provider) {
            AiProviderType.CLAUDE -> {
                val resolvedUrl = if (endpoint.isBlank()) "https://api.anthropic.com/v1/messages" else endpoint
                val payload = JSONObject().apply {
                    put("model", modelName.ifBlank { "claude-3-5-sonnet-20241022" })
                    put("max_tokens", 180)
                    put("system", "You are a concise meteorological safety advisor. Give 2 practical sentences based ONLY on the provided data.")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", contextSummary)
                        })
                    })
                }.toString()

                val res = client.postJson(
                    url = resolvedUrl,
                    jsonBody = payload,
                    headers = mapOf(
                        "x-api-key" to config.personalAiKey.trim(),
                        "anthropic-version" to "2023-06-01",
                        "content-type" to "application/json"
                    )
                )
                res.mapCatching { body ->
                    val root = JSONObject(body)
                    root.optJSONArray("content")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.trim()
                }.getOrNull()
            }
            AiProviderType.GEMINI -> {
                val resolvedUrl = if (endpoint.isBlank()) "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${config.personalAiKey.trim()}" else "$endpoint?key=${config.personalAiKey.trim()}"
                val payload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "You are a concise meteorological safety advisor. Give 2 practical sentences based ONLY on the provided weather data: $contextSummary")
                                })
                            })
                        })
                    })
                }.toString()

                val res = client.postJson(
                    url = resolvedUrl,
                    jsonBody = payload,
                    headers = mapOf("content-type" to "application/json")
                )
                res.mapCatching { body ->
                    val root = JSONObject(body)
                    root.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.trim()
                }.getOrNull()
            }
            else -> {
                // OpenAI compatible format (ChatGPT, OpenRouter, Groq, DeepSeek, Custom)
                val resolvedUrl = if (endpoint.isBlank()) "https://api.openai.com/v1/chat/completions" else endpoint
                val payload = JSONObject().apply {
                    put("model", modelName.ifBlank { "gpt-4o-mini" })
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
                    url = resolvedUrl,
                    jsonBody = payload,
                    headers = mapOf(
                        "Authorization" to "Bearer ${config.personalAiKey.trim()}",
                        "content-type" to "application/json"
                    )
                )
                res.mapCatching { body ->
                    val root = JSONObject(body)
                    root.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        ?.trim()
                }.getOrNull()
            }
        }
    }
}
