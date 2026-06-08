import axios from "axios";
import { logger } from "../utils/logger.js";

/**
 * VERIFY webhook (Meta requirement)
 */
export const verifyWebhook = (req, res) => {
  const mode = req.query["hub.mode"];
  const token = req.query["hub.verify_token"];
  const challenge = req.query["hub.challenge"];

  if (
    mode === "subscribe" &&
    token === process.env.VERIFY_TOKEN
  ) {
    logger.info("✅ Webhook verified");
    return res.status(200).send(challenge);
  }

  return res.sendStatus(403);
};

/**
 * Handle incoming WhatsApp messages (REAL-WORLD SAFE VERSION)
 */
export const handleIncomingMessage = async (req, res) => {
  try {
    const entry = req.body.entry || [];

    for (const e of entry) {
      const changes = e.changes || [];

      for (const change of changes) {
        const value = change.value;

        const messages = value?.messages;

        if (!messages) continue;

        for (const message of messages) {
          const from = message.from;

          // Only handle text messages
          if (message.type !== "text") {
            sendWhatsAppMessage(
              from,
              "⚠️ Only text messages are supported in this MVP."
            ).catch(err => logger.error("Send error:", err.message));
            continue;
          }

          const text = message.text?.body || "";

          logger.info(`📩 Incoming from ${from}: ${text}`);

          const reply = generateReply(text);

          // IMPORTANT: non-blocking send (Meta best practice)
          sendWhatsAppMessage(from, reply)
            .catch(err => logger.error("Send error:", err.message));
        }
      }
    }

    // Always respond fast to Meta
    return res.sendStatus(200);

  } catch (error) {
    logger.error("Webhook error:", error.message);
    return res.sendStatus(200);
  }
};

/**
 * MVP logic
 */
const generateReply = (text) => {
  const msg = text.toLowerCase();

  if (msg.includes("hi") || msg.includes("hello")) {
    return "Hello 👋 Welcome to Fintech MVP Bot";
  }

  if (msg.includes("balance")) {
    return "Your balance feature is coming soon 🚧";
  }

  if (msg.includes("loan")) {
    return "Loan services will be available in next phase 📊";
  }

  return "I received your message 👍 (MVP mode)";
};

/**
 * Send WhatsApp message via Meta Cloud API
 */
const sendWhatsAppMessage = async (to, body) => {
  const url = `https://graph.facebook.com/v20.0/${process.env.PHONE_NUMBER_ID}/messages`;

  const payload = {
    messaging_product: "whatsapp",
    to,
    type: "text",
    text: { body }
  };

  try {
    await axios.post(url, payload, {
      headers: {
        Authorization: `Bearer ${process.env.WHATSAPP_TOKEN}`,
        "Content-Type": "application/json"
      }
    });

    logger.info(`📤 Sent to ${to}`);
  } catch (error) {
    logger.error(
      "Meta send error:",
      error.response?.data || error.message
    );
  }
};