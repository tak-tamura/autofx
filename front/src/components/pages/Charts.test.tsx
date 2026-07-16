import { DEFAULT_BBANDS_K, DEFAULT_BBANDS_N } from "../../config/chartDefaults";

describe("Charts Bollinger Bands defaults", () => {
  it("uses a 20-period band with a 2-sigma multiplier", () => {
    expect(DEFAULT_BBANDS_N).toBe(20);
    expect(DEFAULT_BBANDS_K).toBe(2);
  });
});
