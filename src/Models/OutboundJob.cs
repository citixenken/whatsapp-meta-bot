namespace WhatsAppMetaBot.Models;

/// <summary>A queued outbound text reply: recipient number + message body.</summary>
public sealed record OutboundJob(string To, string Body);
