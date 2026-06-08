import express from "express";
import webhookRoutes from "./routes/webhook.js";
import { logger } from "./utils/logger.js";

const app = express();

app.use(express.json());

// Basic health check
app.get("/", (req, res) => {
  res.send("WhatsApp Meta Bot is running 🚀");
});

// DEBUG endpoint (VERY useful for Meta testing)
app.post("/debug", (req, res) => {
  logger.info("📦 DEBUG PAYLOAD:");
  logger.info(JSON.stringify(req.body, null, 2));
  res.sendStatus(200);
});

// Webhook route
app.use("/webhook", webhookRoutes);

export default app;