namespace WhatsAppMetaBot.Services;

/// <summary>
/// MVP business logic: maps inbound text to a canned reply. Extracted from
/// <see cref="WhatsAppService"/> so it can be unit-tested (CFG-02). Replace with
/// a real intent/NLP engine and core-banking integration for production.
/// </summary>
internal static class ReplyGenerator
{
    public static string Generate(string text)
    {
        var msg = text.ToLowerInvariant();

        if (msg.Contains("hi") || msg.Contains("hello"))
        {
            return "Hello 👋 Welcome to Fintech MVP Bot";
        }

        if (msg.Contains("balance"))
        {
            return "Your balance feature is coming soon 🚧";
        }

        if (msg.Contains("loan"))
        {
            return "Loan services will be available in next phase 📊";
        }

        return "I received your message 👍 (MVP mode)";
    }
}
