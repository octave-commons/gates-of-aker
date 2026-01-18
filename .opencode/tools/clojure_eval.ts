import { tool } from "@opencode-ai/plugin"

export default tool({
  description: "Evaluate Clojure code in a REPL-like environment",
  args: {
    description: tool.schema.string().optional().describe("Optional description of what the code does"),
    code: tool.schema.string().describe("Clojure code to evaluate"),
  },
  async execute(args) {
    const { description, code } = args

    const command = `clojure -M -e "${code.replace(/"/g, '\\"')}"`

    const result = await Bun.$`bash -c ${command}`.text()

    return result.trim()
  },
})
