package com.curiq.api.prompt

import org.springframework.stereotype.Component
import com.curiq.api.dto.ChatRequest

@Component
class PromptBuilder {

    fun buildChatPrompt(request: ChatRequest): String {
        val contextBuilder = StringBuilder()
        request.context.forEach { item ->
            contextBuilder.appendLine("---")
            if (!item.title.isNullOrBlank()) contextBuilder.appendLine("Title: ${item.title}")
            if (!item.url.isNullOrBlank()) contextBuilder.appendLine("URL: ${item.url}")
            if (!item.category.isNullOrBlank()) contextBuilder.appendLine("Category: ${item.category}")
            if (item.tags.isNotEmpty()) contextBuilder.appendLine("Tags: ${item.tags.joinToString(", ")}")
            contextBuilder.appendLine("Summary: ${item.summary}")
        }

        val contextString = if (contextBuilder.isEmpty()) "The user has no saved links yet." else contextBuilder.toString()

        return """
You are Curiq.
Curiq helps people remember and understand what they have saved.
You are NOT a general AI assistant.
Your answers must come ONLY from the supplied bookmarks.
If the answer cannot be found in the supplied bookmarks, say so clearly.
Do not invent information.
Be concise.
When referring to a bookmark, naturally include its title.
If a bookmark has a URL, format it as:
[Bookmark Title](URL)
Use Markdown.
Do not mention internal IDs or technical metadata.

USER QUESTION:
${request.question}

SUPPLIED BOOKMARKS (CONTEXT):
$contextString
        """.trimIndent()
    }

    fun buildCategorizePrompt(content: String): String {
        return """
You are an AI assistant for a personal knowledge management application.

Your task is to analyze the provided text or shared URL and generate structured metadata.

TEXT:
$content

Return ONLY valid JSON.

{
  "category": "",
  "subcategory": "",
  "summary": "",
  "tags": []
}

Rules:

1. Category MUST be exactly ONE of:

Technology
Programming
Artificial Intelligence
Science
Education
Business
Finance
Investing
Marketing
Startups
Career
Productivity
Design
Photography
Video
Music
Movies
Gaming
Sports
Travel
Food
Health
Fitness
Shopping
Fashion
Beauty
Automotive
Real Estate
News
Politics
Books
Research
History
Nature
Animals
Parenting
Relationships
Lifestyle
DIY
Home
Events
Religion
Spirituality
Cryptocurrency
Cybersecurity
Cloud Computing
Android
iOS
Web Development
Data Science
Machine Learning
DevOps
Open Source
Podcasts
Tutorials
Reference
Personal
Other

3. Summary Rules:
- Maximum 2 sentences.
- Jump straight into the subject matter. Focus ONLY on the core facts.
- NEVER mention the platform (Instagram, X, TikTok, etc.).
- NEVER use boilerplate intros like "This reel showcases...", "The video shows...", "This post highlights...", or "The author discusses...".

4. Tags:
- Between 5 and 10 tags. Lowercase. No duplicates.

5. Formatting & Anti-Hallucination:
- Never return Markdown. Never explain anything. Output ONLY JSON.
- If the TEXT provides almost no context (e.g., just a URL or "Check out this reel by @username"), DO NOT hallucinate.

EXAMPLES:

TEXT: Check out this reel by @johnsmith on Instagram! https://instagram.com/reel/abc
JSON:
{
  "category": "Other",
  "subcategory": "Social Media",
  "summary": "A social media post by @johnsmith.",
  "tags": ["social media", "post", "johnsmith"]
}

TEXT: My new recipe for vegan chocolate cake! 🍫🎂 https://youtube.com/shorts/xyz
JSON:
{
  "category": "Food",
  "subcategory": "Baking",
  "summary": "A recipe for vegan chocolate cake.",
  "tags": ["recipe", "vegan", "chocolate cake", "baking", "dessert"]
}
        """.trimIndent()
    }
}
