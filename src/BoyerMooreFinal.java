import java.io.*;
import java.util.*;

public class BoyerMooreFinal {

    private static long comparisonCount;

    // ========== АЛГОРИТМ БОЙЕРА-МУРА  ==========
    public static int search(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        if (pattern.length() > text.length()) return -1;
        int n = text.length(), m = pattern.length();
        comparisonCount = 0;

        Map<Character, Integer> badChar = new HashMap<>();
        for (int i = 0; i < m; i++) badChar.put(pattern.charAt(i), i);

        int[] goodSuffix = buildGoodSuffixTable(pattern);

        int shift = 0;
        while (shift <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                comparisonCount++;
                j--;
            }
            if (j < 0) {
                comparisonCount++;
                return shift;
            }
            comparisonCount++;

            char bad = text.charAt(shift + j);
            int badShift = j - badChar.getOrDefault(bad, -1);
            int goodShift = goodSuffix[j];
            shift += Math.max(1, Math.max(badShift, goodShift));
        }
        return -1;
    }

    private static int[] buildGoodSuffixTable(String p) {
        int m = p.length();
        int[] good = new int[m];
        int[] border = new int[m + 1];
        int i = m, j = m + 1;
        border[i] = j;
        while (i > 0) {
            while (j <= m && p.charAt(i - 1) != p.charAt(j - 1)) {
                if (good[j - 1] == 0) good[j - 1] = j - i;
                j = border[j];
            }
            i--;
            j--;
            border[i] = j;
        }
        j = border[0];
        for (i = 0; i < m; i++) {
            if (good[i] == 0) good[i] = j;
            if (i == j - 1) j = border[j];
        }
        return good;
    }

    // ========== ГЕНЕРАЦИЯ ТЕКСТА С ГАРАНТИРОВАННЫМ ТОЛЬКО ОДНИМ ВХОЖДЕНИЕМ ==========
    // Используем символ, которого нет в паттерне
    private static String generateSafeText(int length, String pattern) {
        char safeChar;
        // Выбираем символ, отсутствующий в pattern
        if (pattern.indexOf('U') == -1) safeChar = 'U';
        else if (pattern.indexOf('W') == -1) safeChar = 'W';
        else safeChar = 'X'; // маловероятно, но по циклу можно перебрать
        char[] chars = new char[length];
        Arrays.fill(chars, safeChar);
        int m = pattern.length();
        for (int i = 0; i < m; i++) {
            chars[length - m + i] = pattern.charAt(i);
        }
        return new String(chars);
    }


    public static void main(String[] args) throws IOException {
        int numSizes = 20;                // 20 точек
        int minLen = 20_000;              // 20 тысяч
        int maxLen = 500_000;             // 500 тысяч
        int repeats = 50;                 // 50 замеров для усреднения
        int warmup = 5;                   // прогрев

        String pat3 = "XYZ";
        String pat20 = "ABCDEFGHIJKLMNOPQRST";

        int[] lengths = new int[numSizes];
        double[] time3 = new double[numSizes];
        double[] time20 = new double[numSizes];
        double[] iter3 = new double[numSizes];
        double[] iter20 = new double[numSizes];

        System.out.println("Генерация текстов с гарантированно единственным вхождением паттерна в конце...");
        System.out.println("Длина текста от " + minLen + " до " + maxLen + " символов\n");

        for (int idx = 0; idx < numSizes; idx++) {
            int textLen = minLen + idx * (maxLen - minLen) / (numSizes - 1);
            lengths[idx] = textLen;

            String text3 = generateSafeText(textLen, pat3);
            String text20 = generateSafeText(textLen, pat20);

            // Проверка (для отладки)
            int pos3 = text3.indexOf(pat3);
            int pos20 = text20.indexOf(pat20);
            if (pos3 != textLen - pat3.length())
                System.err.println("Ошибка генерации pat3: позиция " + pos3);
            if (pos20 != textLen - pat20.length())
                System.err.println("Ошибка генерации pat20: позиция " + pos20);

            // Прогрев JVM
            for (int w = 0; w < warmup; w++) {
                search(text3, pat3);
                search(text20, pat20);
            }

            long totalTime3 = 0, totalIter3 = 0;
            long totalTime20 = 0, totalIter20 = 0;

            for (int r = 0; r < repeats; r++) {
                long start = System.nanoTime();
                int p3 = search(text3, pat3);
                long end = System.nanoTime();
                totalTime3 += (end - start);
                totalIter3 += comparisonCount;
                if (p3 != textLen - pat3.length())
                    System.err.println("Ошибка поиска pat3: " + p3);

                start = System.nanoTime();
                int p20 = search(text20, pat20);
                end = System.nanoTime();
                totalTime20 += (end - start);
                totalIter20 += comparisonCount;
                if (p20 != textLen - pat20.length())
                    System.err.println("Ошибка поиска pat20: " + p20);
            }

            time3[idx] = (totalTime3 / repeats) / 1000.0;   // микросекунды
            iter3[idx] = (double) totalIter3 / repeats;
            time20[idx] = (totalTime20 / repeats) / 1000.0;
            iter20[idx] = (double) totalIter20 / repeats;

            System.out.printf("Длина: %6d | pat3: %8.1f мкс | pat20: %8.1f мкс | отношение: %.2f%n",
                    textLen, time3[idx], time20[idx], time3[idx] / time20[idx]);
        }

        // Сохраняем CSV
        try (FileWriter w = new FileWriter("boyer_moore_fixed.csv")) {
            w.write('\uFEFF');
            w.write("TextLength;TimeMicros_Pattern3;TimeMicros_Pattern20;Iterations_Pattern3;Iterations_Pattern20\n");
            for (int i = 0; i < numSizes; i++) {
                w.write(String.format(Locale.US, "%d;%.2f;%.2f;%.0f;%.0f\n",
                        lengths[i], time3[i], time20[i], iter3[i], iter20[i]));
            }
        }

        System.out.println("\nФайл 'boyer_moore_fixed.csv' создан.");

    }
}