using WhatsAppMetaBot.Services;
using Xunit;

namespace WhatsAppMetaBot.Tests;

public class PiiMaskerTests
{
    [Theory]
    [InlineData("254712345678", "********5678")]
    [InlineData("1234", "****")]
    [InlineData("99", "**")]
    [InlineData("", "(unknown)")]
    [InlineData(null, "(unknown)")]
    public void MaskPhone_masks_all_but_last_four(string? input, string expected)
    {
        Assert.Equal(expected, PiiMasker.MaskPhone(input));
    }
}
