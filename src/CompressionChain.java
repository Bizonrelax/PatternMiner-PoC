import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class CompressionChain {
    
    // Автоматический выбор лучшего преобразования
    public static String autoTransform(String input) {
        // Анализируем данные
        DataAnalysis analysis = analyzeData(input);
        
        // Выбираем лучшее преобразование
        if (analysis.longestRun >= 4) {
            return SelfDescribingTransform.embedTransform(input, "RUN_LENGTH_EMBEDDED");
        } else if (analysis.uniqueChars <= 16) {
            return SelfDescribingTransform.embedTransform(input, "FREQ_GROUP_EMBEDDED");
        } else if (analysis.entropy < 4.0) {
            return SelfDescribingTransform.embedTransform(input, "SORT_EMBEDDED");
        } else {
            return SelfDescribingTransform.embedTransform(input, "PATTERN_CYCLE_EMBEDDED");
        }
    }
    
    // Циклическое сжатие с умной остановкой
    public static ChainResult compressChain(String input, int maxCycles) {
        List<String> transformHistory = new ArrayList<>();
        List<Integer> sizeHistory = new ArrayList<>();
        List<Double> ratioHistory = new ArrayList<>();
        
        String current = input;
        int cycle = 0;
        int bestSize = Integer.MAX_VALUE;
        String bestData = current;
        
        System.out.println("=== УМНАЯ ЦЕПОЧКА СЖАТИЯ ===");
        System.out.printf("Начальный размер: %d символов%n", input.length());
        System.out.println();
        
        while (cycle < maxCycles) {
            cycle++;
            System.out.printf("Цикл %d:%n", cycle);
            
            // Применяем преобразование
            String transformed = autoTransform(current);
            String transformName = getTransformName(transformed);
            
            // Сжимаем ZIP
            byte[] compressed = compressZip(transformed);
            int size = compressed.length;
            double ratio = (double) size / current.length();
            
            // Сохраняем историю
            transformHistory.add(transformName);
            sizeHistory.add(size);
            ratioHistory.add(ratio);
            
            System.out.printf("  Преобразование: %s%n", transformName);
            System.out.printf("  Размер после ZIP: %d байт (ratio: %.3f)%n", size, ratio);
            
            // Проверяем улучшение
            if (size < bestSize) {
                bestSize = size;
                bestData = new String(compressed);
                System.out.printf("  ✅ УЛУЧШЕНИЕ!%n");
            } else {
                System.out.printf("  ⚠️ Нет улучшения, останавливаемся.%n");
                break;
            }
            
            // Для следующего цикла используем сжатую строку как вход
            current = new String(compressed);
        }
        
        System.out.println();
        System.out.println("=".repeat(50));
        System.out.printf("Лучший размер: %d байт%n", bestSize);
        System.out.printf("Коэффициент сжатия: %.3f%n", 
            (double) bestSize / input.length());
        System.out.println("История преобразований: " + transformHistory);
        
        return new ChainResult(bestData.getBytes(), transformHistory, 
                              sizeHistory, ratioHistory, input.length());
    }
    
    // Анализ данных
    private static DataAnalysis analyzeData(String input) {
        DataAnalysis result = new DataAnalysis();
        
        // Уникальные символы
        Set<Character> unique = new HashSet<>();
        for (char c : input.toCharArray()) unique.add(c);
        result.uniqueChars = unique.size();
        
        // Самый длинный повтор
        int maxRun = 0;
        int currentRun = 1;
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) == input.charAt(i-1)) {
                currentRun++;
                maxRun = Math.max(maxRun, currentRun);
            } else {
                currentRun = 1;
            }
        }
        result.longestRun = maxRun;
        
        // Энтропия (упрощенная)
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        double entropy = 0.0;
        for (int count : freq.values()) {
            double p = (double) count / input.length();
            entropy -= p * Math.log(p) / Math.log(2);
        }
        result.entropy = entropy;
        
        return result;
    }
    
    private static String getTransformName(String transformed) {
        if (transformed.startsWith("SORT|")) return "Сортировка";
        if (transformed.startsWith("FREQ|")) return "Частотная";
        if (transformed.startsWith("RLE|")) return "RLE";
        if (transformed.startsWith("CYC|")) return "Цикл";
        return "Без преобразования";
    }
    
    private static byte[] compressZip(String input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    // Классы для результатов
    public static class DataAnalysis {
        int uniqueChars;
        int longestRun;
        double entropy;
    }
    
    public static class ChainResult {
        public final byte[] data;
        public final List<String> transforms;
        public final List<Integer> sizes;
        public final List<Double> ratios;
        public final int originalSize;
        
        public ChainResult(byte[] data, List<String> transforms, 
                          List<Integer> sizes, List<Double> ratios, int originalSize) {
            this.data = data;
            this.transforms = transforms;
            this.sizes = sizes;
            this.ratios = ratios;
            this.originalSize = originalSize;
        }
    }
}