import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class TransformCompressor {
    
    // Преобразование 1: Простой сдвиг
    public String transformShift(String input, int shift) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            int newChar = (c + shift) % 128;
            if (newChar < 32) newChar += 32;
            result.append((char) newChar);
        }
        return result.toString();
    }
    
    // Преобразование 2: Переворот блоков
    public String transformBlockReverse(String input, int blockSize) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i += blockSize) {
            int end = Math.min(i + blockSize, input.length());
            String block = input.substring(i, end);
            result.append(new StringBuilder(block).reverse());
        }
        return result.toString();
    }
    
    // Преобразование 3: XOR с позицией
    public String transformXorWithPosition(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int xorValue = i % 64;
            result.append((char) (c ^ xorValue));
        }
        return result.toString();
    }
    
    // Преобразование 4: Группировка одинаковых символов (упрощенная BWT)
    public String transformGroupSimilar(String input) {
        // Считаем частоты символов
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        // Собираем строку, группируя одинаковые символы
        StringBuilder result = new StringBuilder();
        List<Character> chars = new ArrayList<>();
        for (char c : input.toCharArray()) chars.add(c);
        
        // Сортируем по частоте (часто встречающиеся идут первыми)
        chars.sort((a, b) -> {
            int freqCompare = Integer.compare(freq.get(b), freq.get(a));
            if (freqCompare == 0) return Character.compare(a, b);
            return freqCompare;
        });
        
        for (char c : chars) result.append(c);
        return result.toString();
    }
    
    // Метод для сжатия ZIP
    public byte[] compressZip(String input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(input.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    // Основной тест
    public void testAllTransforms(String original) {
        System.out.println("Тестируем преобразования:");
        System.out.println("=".repeat(60));
        
        // Сохраняем исходный размер
        byte[] originalCompressed = compressZip(original);
        System.out.printf("1. Исходные данные (ZIP): %d байт%n", originalCompressed.length);
        
        // Тест 1: Сдвиг
        String shifted = transformShift(original, 7);
        byte[] shiftedCompressed = compressZip(shifted);
        System.out.printf("2. Сдвиг на 7 (ZIP): %d байт", shiftedCompressed.length);
        printComparison(originalCompressed.length, shiftedCompressed.length);
        
        // Тест 2: Переворот блоков
        String reversed = transformBlockReverse(original, 8);
        byte[] reversedCompressed = compressZip(reversed);
        System.out.printf("3. Переворот блоков 8 (ZIP): %d байт", reversedCompressed.length);
        printComparison(originalCompressed.length, reversedCompressed.length);
        
        // Тест 3: XOR с позицией
        String xored = transformXorWithPosition(original);
        byte[] xoredCompressed = compressZip(xored);
        System.out.printf("4. XOR с позицией (ZIP): %d байт", xoredCompressed.length);
        printComparison(originalCompressed.length, xoredCompressed.length);
        
        // Тест 4: Группировка
        String grouped = transformGroupSimilar(original);
        byte[] groupedCompressed = compressZip(grouped);
        System.out.printf("5. Группировка символов (ZIP): %d байт", groupedCompressed.length);
        printComparison(originalCompressed.length, groupedCompressed.length);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ЗАМЕТКИ:");
        System.out.println("- Если размер УМЕНЬШИЛСЯ, преобразование полезно");
        System.out.println("- Если размер УВЕЛИЧИЛСЯ, преобразование вредит");
        System.out.println("- Цель: найти преобразование, которое ДАЁТ выигрыш");
    }
    
    private void printComparison(int originalSize, int newSize) {
        int diff = newSize - originalSize;
        if (diff < 0) {
            System.out.printf(" (▼ -%d байт)%n", -diff);
        } else if (diff > 0) {
            System.out.printf(" (▲ +%d байт)%n", diff);
        } else {
            System.out.println(" (без изменений)");
        }
    }
    public void deepAnalysis(String input) {
        System.out.println("\n=== ГЛУБОКИЙ АНАЛИЗ ДАННЫХ ===");
        
        // Частотный анализ символов
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        System.out.println("Самые частые символы:");
        freq.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry -> 
                System.out.printf("  '%c' (код %3d) : %3d раз (%.1f%%)%n",
                    entry.getKey(), (int) entry.getKey(), entry.getValue(),
                    entry.getValue() * 100.0 / input.length()));
        
        // Ищем паттерны длины 2
        System.out.println("\nЧастые биграммы (пары символов):");
        Map<String, Integer> bigrams = new HashMap<>();
        for (int i = 0; i < input.length() - 1; i++) {
            String pair = input.substring(i, i + 2);
            bigrams.put(pair, bigrams.getOrDefault(pair, 0) + 1);
        }
        
        bigrams.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry ->
                System.out.printf("  \"%s\" : %d раз%n", entry.getKey(), entry.getValue()));
    }
}