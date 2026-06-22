namespace WhatsAppMetaBot.Services;

/// <summary>Helpers to keep PII (phone numbers) out of logs (SEC-03).</summary>
internal static class PiiMasker
{
    /// <summary>Masks all but the last 4 digits of a phone number.</summary>
    public static string MaskPhone(string? phone)
    {
        if (string.IsNullOrEmpty(phone))
        {
            return "(unknown)";
        }

        if (phone.Length <= 4)
        {
            return new string('*', phone.Length);
        }

        return string.Concat(new string('*', phone.Length - 4), phone.AsSpan(phone.Length - 4));
    }
}
