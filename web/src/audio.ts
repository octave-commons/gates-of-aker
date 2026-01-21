import { CONFIG } from "./config/constants";

const PENTATONIC_SCALE = [261.63, 293.66, 329.63, 392.00, 440.00, 523.25] as const;
const DEFAULT_NOTE_DURATION = 0.12;
const DEFAULT_NOTE_GAP = 0.04;

type AudioState = {
  context: AudioContext | null;
  masterGain: GainNode | null;
  isMuted: boolean;
  isInitialized: boolean;
  hasUserInteracted: boolean;
};

const state: AudioState = {
  context: null,
  masterGain: null,
  isMuted: true,
  isInitialized: false,
  hasUserInteracted: false,
};

function ensureInitialized() {
  if (!state.isInitialized) {
    try {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContextClass) {
        console.warn("[AUDIO] Web Audio API not supported");
        return false;
      }
      state.context = new AudioContextClass();
      state.masterGain = state.context.createGain();
      state.masterGain.gain.value = state.isMuted ? 0 : CONFIG.audio.MASTER_GAIN;
      state.masterGain.connect(state.context.destination);
      state.isInitialized = true;
    } catch (e) {
      console.error("[AUDIO] Failed to initialize AudioContext:", e);
      return false;
    }
  }
  return true;
}

function resumeContext() {
  if (state.context && state.context.state === "suspended") {
    state.context.resume();
  }
}

function hexToRgb(hex: string): [number, number, number] | null {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex.replace(/^#/, ""));
  return result ? [
    parseInt(result[1], 16),
    parseInt(result[2], 16),
    parseInt(result[3], 16)
  ] : null;
}

function rgbToHsl(r: number, g: number, b: number): number {
  r /= 255;
  g /= 255;
  b /= 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  let h = 0;
  if (max === min) {
    h = 0;
  } else if (max === r) {
    h = ((g - b) / (max - min)) % 6;
  } else if (max === g) {
    h = (b - r) / (max - min) + 2;
  } else {
    h = (r - g) / (max - min) + 4;
  }
  h = Math.round(h * 60);
  return h < 0 ? h + 360 : h;
}

export function hexToFrequency(hex: string): number {
  const rgb = hexToRgb(hex);
  if (!rgb) {
    return 440;
  }
  const [r, g, b] = rgb;
  const hue = rgbToHsl(r, g, b);
  const index = Math.floor((hue / 360) * PENTATONIC_SCALE.length);
  return PENTATONIC_SCALE[Math.min(index, PENTATONIC_SCALE.length - 1)];
}

export function getScaleFrequency(index: number, octaveShift: number = 0): number {
  const safeIndex = ((index % PENTATONIC_SCALE.length) + PENTATONIC_SCALE.length) % PENTATONIC_SCALE.length;
  const base = PENTATONIC_SCALE[safeIndex];
  return base * Math.pow(2, octaveShift);
}

export function markUserInteraction(): void {
  state.hasUserInteracted = true;
}

export function playTone(frequency: number, duration: number = 0.1): void {
  if (!state.hasUserInteracted) {
    return;
  }
  
  if (!ensureInitialized()) {
    return;
  }
  
  resumeContext();
  
  if (state.isMuted || !state.context || !state.masterGain) {
    return;
  }
  
  try {
    const oscillator = state.context.createOscillator();
    const gainNode = state.context.createGain();

    oscillator.type = "sine";
    oscillator.frequency.value = Math.max(CONFIG.audio.MIN_FREQUENCY, Math.min(CONFIG.audio.MAX_FREQUENCY, frequency));

    gainNode.gain.setValueAtTime(0, state.context.currentTime);
    gainNode.gain.linearRampToValueAtTime(CONFIG.audio.MASTER_GAIN, state.context.currentTime + CONFIG.audio.GAIN_RAMP_TIME);
    gainNode.gain.exponentialRampToValueAtTime(0.01, state.context.currentTime + duration);
    
    oscillator.connect(gainNode);
    gainNode.connect(state.masterGain);
    
    oscillator.start(state.context.currentTime);
    oscillator.stop(state.context.currentTime + duration);
    
    oscillator.onended = () => {
      oscillator.disconnect();
      gainNode.disconnect();
    };
  } catch (e) {
    console.error("[AUDIO] Failed to play tone:", e);
  }
}

type ToneSequenceOptions = {
  noteDuration?: number;
  gap?: number;
  startDelay?: number;
  type?: OscillatorType;
  gain?: number;
};

export function playToneSequence(
  frequencies: number[],
  options: ToneSequenceOptions = {}
): void {
  if (!state.hasUserInteracted) {
    return;
  }

  if (!ensureInitialized()) {
    return;
  }

  resumeContext();

  if (state.isMuted || !state.context || !state.masterGain) {
    return;
  }

  const noteDuration = options.noteDuration ?? DEFAULT_NOTE_DURATION;
  const gap = options.gap ?? DEFAULT_NOTE_GAP;
  const startDelay = options.startDelay ?? 0;
  const oscillatorType = options.type ?? "sine";
  const gainMultiplier = options.gain ?? 1.0;

  try {
    const baseTime = state.context.currentTime + startDelay;
    frequencies.forEach((frequency, idx) => {
      const startTime = baseTime + idx * (noteDuration + gap);
      const oscillator = state.context!.createOscillator();
      const gainNode = state.context!.createGain();

      oscillator.type = oscillatorType;
      oscillator.frequency.value = Math.max(CONFIG.audio.MIN_FREQUENCY, Math.min(CONFIG.audio.MAX_FREQUENCY, frequency));

      gainNode.gain.setValueAtTime(0, startTime);
      gainNode.gain.linearRampToValueAtTime(CONFIG.audio.MASTER_GAIN * gainMultiplier, startTime + CONFIG.audio.GAIN_RAMP_TIME);
      gainNode.gain.exponentialRampToValueAtTime(0.01, startTime + noteDuration);

      oscillator.connect(gainNode);
      gainNode.connect(state.masterGain!);

      oscillator.start(startTime);
      oscillator.stop(startTime + noteDuration);

      oscillator.onended = () => {
        oscillator.disconnect();
        gainNode.disconnect();
      };
    });
  } catch (e) {
    console.error("[AUDIO] Failed to play tone sequence:", e);
  }
}

export function playDeathTone(): void {
  if (!state.hasUserInteracted) {
    return;
  }

  if (!ensureInitialized()) {
    return;
  }

  resumeContext();

  if (state.isMuted || !state.context || !state.masterGain) {
    return;
  }

  try {
    const duration = 0.8;
    const oscillator = state.context.createOscillator();
    const gainNode = state.context.createGain();

    oscillator.type = "triangle";
    oscillator.frequency.setValueAtTime(220, state.context.currentTime);
    oscillator.frequency.exponentialRampToValueAtTime(110, state.context.currentTime + duration);

    gainNode.gain.setValueAtTime(0, state.context.currentTime);
    gainNode.gain.linearRampToValueAtTime(CONFIG.audio.MASTER_GAIN * 1.4, state.context.currentTime + 0.05);
    gainNode.gain.exponentialRampToValueAtTime(0.01, state.context.currentTime + duration);

    oscillator.connect(gainNode);
    gainNode.connect(state.masterGain);

    oscillator.start(state.context.currentTime);
    oscillator.stop(state.context.currentTime + duration);

    oscillator.onended = () => {
      oscillator.disconnect();
      gainNode.disconnect();
    };
  } catch (e) {
    console.error("[AUDIO] Failed to play death tone:", e);
  }
}

export function setMute(muted: boolean): void {
  state.isMuted = muted;
  if (state.masterGain) {
    state.masterGain.gain.setValueAtTime(muted ? 0 : CONFIG.audio.MASTER_GAIN, state.masterGain.context.currentTime);
  }
}

export function isMuted(): boolean {
  return state.isMuted;
}

export function toggleMute(): boolean {
  const newState = !state.isMuted;
  setMute(newState);
  return newState;
}
