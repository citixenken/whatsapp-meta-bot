using WhatsAppMetaBot.Services;
using Xunit;

namespace WhatsAppMetaBot.Tests;

public class ReplyGeneratorTests
{
    [Theory]
    [InlineData("hi", "Hello 👋 Welcome to Fintech MVP Bot")]
    [InlineData("Hello there", "Hello 👋 Welcome to Fintech MVP Bot")]
    [InlineData("what is my BALANCE", "Your balance feature is coming soon 🚧")]
    [InlineData("I need a loan", "Loan services will be available in next phase 📊")]
    [InlineData("xyz", "I received your message 👍 (MVP mode)")]
    public void Generate_returns_expected_reply(string input, string expected)
    {
        Assert.Equal(expected, ReplyGenerator.Generate(input));
    }
}
