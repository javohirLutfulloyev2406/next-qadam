package uz.nextqadam.bot.ai;

public interface AiClient {

    /**
     * Gemini'dan QAT'IY JSON javob so'raydi (response_mime_type=application/json) — Goal decomposition
     * va Brain Dump kabi struktura kutilgan holatlar uchun.
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * Gemini'dan erkin, tabiiy matn javob so'raydi (JSON mime type cheklovisiz) — Companion modulidagi
     * erkin suhbat, motivatsiya va SOS mikro-qadam kabi holatlar uchun.
     */
    String completeText(String systemPrompt, String userPrompt);
}