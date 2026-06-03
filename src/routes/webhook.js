import express from "express";
import {
  verifyWebhook,
  handleIncomingMessage
} from "../services/whatsappService.js";

const router = express.Router();

/**
 * Meta Webhook verification
 */
router.get("/", verifyWebhook);

/**
 * Incoming WhatsApp messages
 */
router.post("/", handleIncomingMessage);

export default router;