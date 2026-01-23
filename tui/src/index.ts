#!/usr/bin/env node

import { App } from "./app";

console.log("Starting Fantasia TUI...");
console.log("Make sure the backend is running on http://localhost:3000");
console.log("");

const app = new App();
app.start();
