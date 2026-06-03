import express from "express";
import webhookRoutes from "./routes/webhook.js";

const app = express();

app.use(express.json());

// Basic health check
app.get("/", (req, res) => {
  res.send("WhatsApp Meta Bot is running 🚀");
});

// DEBUG endpoint (VERY useful for Meta testing)
app.post("/debug", (req, res) => {
  console.log("📦 DEBUG PAYLOAD:");
  console.log(JSON.stringify(req.body, null, 2));
  res.sendStatus(200);
});

// Webhook route
app.use("/webhook", webhookRoutes);

export default app;