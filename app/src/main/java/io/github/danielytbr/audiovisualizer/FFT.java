package io.github.danielytbr.audiovisualizer;

public class FFT {

    // Performs an in-place FFT on real and imaginary arrays.
    // real[] and imag[] must be the same length and a power of 2.
    public static void fft(float[] real, float[] imag) {
        int n = real.length;
        if (n == 1) return;

        // Bit-reversal permutation
        int bits = (int) (Math.log(n) / Math.log(2));
        for (int i = 0; i < n; i++) {
            int rev = reverseBits(i, bits);
            if (i < rev) {
                float tmpR = real[i];
                float tmpI = imag[i];
                real[i] = real[rev];
                imag[i] = imag[rev];
                real[rev] = tmpR;
                imag[rev] = tmpI;
            }
        }

        // Cooley-Tukey iterative FFT
        for (int len = 2; len <= n; len <<= 1) {
            float angle = -2.0f * (float) Math.PI / len;
            float wlenR = (float) Math.cos(angle);
            float wlenI = (float) Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                float wR = 1.0f;
                float wI = 0.0f;
                for (int j = 0; j < len / 2; j++) {
                    int idx1 = i + j;
                    int idx2 = idx1 + len / 2;
                    float uR = real[idx1];
                    float uI = imag[idx1];
                    float vR = real[idx2] * wR - imag[idx2] * wI;
                    float vI = real[idx2] * wI + imag[idx2] * wR;
                    real[idx1] = uR + vR;
                    imag[idx1] = uI + vI;
                    real[idx2] = uR - vR;
                    imag[idx2] = uI - vI;
                    float nextWr = wR * wlenR - wI * wlenI;
                    float nextWi = wR * wlenI + wI * wlenR;
                    wR = nextWr;
                    wI = nextWi;
                }
            }
        }
    }

    private static int reverseBits(int num, int bits) {
        int rev = 0;
        for (int i = 0; i < bits; i++) {
            rev <<= 1;
            rev |= (num & 1);
            num >>= 1;
        }
        return rev;
    }

    // Apply a Hann window to reduce spectral leakage
    public static void applyHannWindow(float[] data) {
        int n = data.length;
        for (int i = 0; i < n; i++) {
            float window = 0.5f * (1.0f - (float) Math.cos(2.0f * Math.PI * i / (n - 1)));
            data[i] *= window;
        }
    }
}