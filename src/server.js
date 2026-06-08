import dotenv from "dotenv";
dotenv.config();

import app from "./app.js";
import { logger } from "./utils/logger.js";

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  logger.info(`🚀 WhatsApp bot running on port ${PORT}`);
});