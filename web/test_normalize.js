// Test the normalization regex
const normalizeKey = (key) => {
  if (key.includes("[")) {
    return key.replace(/^\[(-?\d+)[,\s]+(-?\d+)\]$/, (_, q, r) => `${q},${r}`);
  }
  return key;
};

// Test cases from backend
const testCases = [
  "[0 0]",
  "[0, 0]",
  "[1,2]",
  "[3 4]",
  "0,0",
  "[10 5]"
];

testCases.forEach(tc => {
  console.log(`normalizeKey("${tc}") = "${normalizeKey(tc)}"`);
});

// Simulate delta tile object
const deltaTiles = {
  "[0 0]": {biome: "forest"},
  "[1, 2]": {biome: "field"},
  "[3 4]": {biome: "rocky"}
};

console.log("\nNormalized delta tiles:");
const normalized = {};
for (const [key, value] of Object.entries(deltaTiles)) {
  const normalizedKey = normalizeKey(key);
  normalized[normalizedKey] = value;
  console.log(`  ${key} -> ${normalizedKey}`);
}
console.log("Access normalized[\"0,0\"]:", normalized["0,0"]);
console.log("Access normalized[\"1,2\"]:", normalized["1,2"]);
console.log("Access normalized[\"3,4\"]:", normalized["3,4"]);
